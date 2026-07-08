package com.julianh06.bettermenubuttons.mixin;

import com.julianh06.bettermenubuttons.config.MenuArea;
import com.julianh06.bettermenubuttons.config.MenuButton;
import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfigScreen;
import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfig;
import com.julianh06.bettermenubuttons.config.BetterMenuButtonsIconButton;
import com.julianh06.bettermenubuttons.config.CustomJoinIconButton;
import com.julianh06.bettermenubuttons.config.CustomJoinTargets;
import com.julianh06.bettermenubuttons.compat.ModMenuIntegration;
import com.julianh06.bettermenubuttons.layout.SocialButtonLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	@Unique private static final int MAIN_MENU_ICON_ROW_GAP = 12;
	@Unique private static final int VANILLA_MENU_WIDTH = 204;
	@Unique private static final Identifier LANGUAGE_SPRITE = Identifier.withDefaultNamespace("icon/language");
	@Unique private static final Identifier ACCESSIBILITY_SPRITE = Identifier.withDefaultNamespace("icon/accessibility");

	@Shadow private FriendsButton friends;
	@Unique private AbstractWidget better_menu_buttons$configButton;
	@Unique private final List<CustomJoinIconButton> better_menu_buttons$customButtons = new ArrayList<>();
	@Unique private final Map<CustomJoinIconButton, String> better_menu_buttons$customButtonIds = new HashMap<>();
	@Unique private boolean better_menu_buttons$layoutApplied;
	@Unique private int better_menu_buttons$modMenuLayoutState;

	private TitleScreenMixin() {
		super(Component.empty());
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void better_menu_buttons$layoutFriendsButton(CallbackInfo callbackInfo) {
		this.better_menu_buttons$layoutApplied = false;
		for (CustomJoinIconButton button : this.better_menu_buttons$customButtons) {
			button.close();
		}
		this.better_menu_buttons$customButtons.clear();
		this.better_menu_buttons$customButtonIds.clear();
		this.better_menu_buttons$modMenuLayoutState = ModMenuIntegration.buttonLayoutState(MenuArea.MAIN_MENU);
		this.better_menu_buttons$configButton = this.addRenderableWidget(new BetterMenuButtonsIconButton(0, 0, button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new BetterMenuButtonsConfigScreen(this, MenuArea.MAIN_MENU));
			}
		}));
		this.better_menu_buttons$configButton.setTooltip(Tooltip.create(BetterMenuButtonsIconButton.label()));
		this.better_menu_buttons$addCustomJoinButtons();
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void better_menu_buttons$layoutFriendsButtonAfterInit(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks, CallbackInfo callbackInfo) {
		int modMenuLayoutState = ModMenuIntegration.buttonLayoutState(MenuArea.MAIN_MENU);
		if (this.better_menu_buttons$layoutApplied) {
			if (this.better_menu_buttons$modMenuLayoutState == modMenuLayoutState) {
				return;
			}
			this.better_menu_buttons$modMenuLayoutState = modMenuLayoutState;
			this.better_menu_buttons$layoutApplied = false;
			this.rebuildWidgets();
			callbackInfo.cancel();
			return;
		}

		this.better_menu_buttons$layoutApplied = true;
		this.better_menu_buttons$modMenuLayoutState = modMenuLayoutState;
		java.util.LinkedHashMap<String, AbstractWidget> moveableButtons = new java.util.LinkedHashMap<>();
		if (this.better_menu_buttons$configButton != null) {
			moveableButtons.put(BetterMenuButtonsConfig.id(MenuButton.BETTER_MENU_BUTTONS), this.better_menu_buttons$configButton);
		}
		if (this.friends != null) {
			moveableButtons.put(BetterMenuButtonsConfig.id(MenuButton.FRIENDS), this.friends);
		}
		for (CustomJoinIconButton button : this.better_menu_buttons$customButtons) {
			moveableButtons.put(this.better_menu_buttons$customButtonIds.get(button), button);
		}
		List<AbstractWidget> vanillaMenuButtons = new ArrayList<>();
		List<Integer> iconRowYs = new ArrayList<>();

		int menuLeft = this.width / 2 - VANILLA_MENU_WIDTH / 2;
		int menuRight = menuLeft + VANILLA_MENU_WIDTH;
		int menuTop = Integer.MAX_VALUE;
		int menuBottom = 0;
		int insertedRowY = Integer.MAX_VALUE;
		for (AbstractWidget widget : this.better_menu_buttons$widgets()) {
			if (widget == null) {
				continue;
			}

			MenuButton buttonId = buttonIdFor(widget);
			if (buttonId != null) {
				moveableButtons.put(BetterMenuButtonsConfig.id(buttonId), widget);
			}
			if (this.isIconRowWidget(widget, buttonId)) {
				iconRowYs.add(widget.getY());
			}
			if (ModMenuIntegration.isModsRowButton(widget)) {
				insertedRowY = Math.min(insertedRowY, widget.getY());
			}

			if (isTitleMenuButton(widget, menuLeft, menuRight)) {
				vanillaMenuButtons.add(widget);
				menuTop = Math.min(menuTop, widget.getY());
				menuBottom = Math.max(menuBottom, widget.getBottom());
			}
		}

		if (menuTop == Integer.MAX_VALUE) {
			if (this.friends != null) {
				menuTop = this.friends.getY();
				menuBottom = this.friends.getBottom();
			} else {
				menuTop = this.height / 4;
				menuBottom = this.height / 2;
			}
		}

		if (!BetterMenuButtonsConfig.hasVanillaIconRowButtons(MenuArea.MAIN_MENU) && !ModMenuIntegration.hasInsertedRow(MenuArea.MAIN_MENU)) {
			moveBottomMenuRowUp(vanillaMenuButtons, menuBottom);
			menuBottom -= MAIN_MENU_ICON_ROW_GAP;
		}

		if (ModMenuIntegration.hasInsertedRow(MenuArea.MAIN_MENU) && BetterMenuButtonsConfig.hasVanillaIconRowButtons(MenuArea.MAIN_MENU) && insertedRowY != Integer.MAX_VALUE) {
			moveButtonsBelowInsertedRow(vanillaMenuButtons, insertedRowY, insertedRowY + 48);
			menuTop = vanillaMenuButtons.stream().mapToInt(AbstractWidget::getY).min().orElse(menuTop);
			menuBottom = vanillaMenuButtons.stream().mapToInt(AbstractWidget::getBottom).max().orElse(menuBottom);
		} else if (ModMenuIntegration.hasInsertedRow(MenuArea.MAIN_MENU) && insertedRowY != Integer.MAX_VALUE) {
			moveButtonsBelowInsertedRow(vanillaMenuButtons, insertedRowY, insertedRowY + 24 + MAIN_MENU_ICON_ROW_GAP);
			menuTop = vanillaMenuButtons.stream().mapToInt(AbstractWidget::getY).min().orElse(menuTop);
			menuBottom = vanillaMenuButtons.stream().mapToInt(AbstractWidget::getBottom).max().orElse(menuBottom);
		}

		SocialButtonLayout.MenuGeometry geometry = SocialButtonLayout.geometry(MenuArea.MAIN_MENU, menuTop, menuBottom, menuRowYs(vanillaMenuButtons), iconRowY(iconRowYs));
		SocialButtonLayout.applyById(this.width, this.height, menuLeft, menuRight, geometry, MenuArea.MAIN_MENU, moveableButtons);
	}

	@Unique
	private void better_menu_buttons$addCustomJoinButtons() {
		if (this.minecraft == null) {
			return;
		}
		for (BetterMenuButtonsConfig.CustomJoinButton config : BetterMenuButtonsConfig.customButtons()) {
			CustomJoinIconButton button = this.addRenderableWidget(new CustomJoinIconButton(0, 0, config, ignored -> this.better_menu_buttons$join(config)));
			Component tooltip = Component.literal(config.label);
			if (!this.better_menu_buttons$targetAvailable(config)) {
				button.active = false;
				tooltip = Component.literal(config.label + " (missing)");
			}
			button.setTooltip(Tooltip.create(tooltip));
			this.better_menu_buttons$customButtons.add(button);
			this.better_menu_buttons$customButtonIds.put(button, config.id);
		}
	}

	@Unique
	private boolean better_menu_buttons$targetAvailable(BetterMenuButtonsConfig.CustomJoinButton config) {
		if (this.minecraft == null) {
			return false;
		}
		if (config.targetType == BetterMenuButtonsConfig.CustomJoinButton.TargetType.SERVER) {
			return CustomJoinTargets.server(this.minecraft, config.targetId) != null;
		}
		LevelSummary world = CustomJoinTargets.world(this.minecraft, config.targetId);
		return world != null && world.primaryActionActive();
	}

	@Unique
	private void better_menu_buttons$join(BetterMenuButtonsConfig.CustomJoinButton config) {
		if (this.minecraft == null) {
			return;
		}
		if (config.targetType == BetterMenuButtonsConfig.CustomJoinButton.TargetType.SERVER) {
			ServerData server = CustomJoinTargets.server(this.minecraft, config.targetId);
			if (server != null) {
				ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(server.ip), server, false, null);
			}
			return;
		}
		LevelSummary world = CustomJoinTargets.world(this.minecraft, config.targetId);
		if (world != null && world.primaryActionActive()) {
			this.minecraft.createWorldOpenFlows().openWorld(world.getLevelId(), this::rebuildWidgets);
		}
	}

	@Inject(method = "removed", at = @At("TAIL"))
	private void better_menu_buttons$closeCustomJoinButtons(CallbackInfo callbackInfo) {
		for (CustomJoinIconButton button : this.better_menu_buttons$customButtons) {
			button.close();
		}
		this.better_menu_buttons$customButtons.clear();
		this.better_menu_buttons$customButtonIds.clear();
	}

	@Unique
	private List<AbstractWidget> better_menu_buttons$widgets() {
		List<AbstractWidget> widgets = new ArrayList<>();
		this.better_menu_buttons$addWidgets(widgets, this.better_menu_buttons$fabricScreenApiWidgets());
		for (GuiEventListener child : this.children()) {
			if (child instanceof AbstractWidget widget && !widgets.contains(widget)) {
				widgets.add(widget);
			}
		}
		for (Renderable renderable : ((ScreenAccessor) this).better_menu_buttons$renderables()) {
			if (renderable instanceof AbstractWidget widget && !widgets.contains(widget)) {
				widgets.add(widget);
			}
		}
		return widgets;
	}

	@Unique
	@SuppressWarnings("unchecked")
	private List<AbstractWidget> better_menu_buttons$fabricScreenApiWidgets() {
		try {
			Class<?> screensClass = Class.forName("net.fabricmc.fabric.api.client.screen.v1.Screens");
			Method getWidgets = screensClass.getMethod("getWidgets", Screen.class);
			Object result = getWidgets.invoke(null, this);
			return result instanceof List<?> list ? (List<AbstractWidget>) list : List.of();
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
			return List.of();
		}
	}

	@Unique
	private void better_menu_buttons$addWidgets(List<AbstractWidget> target, List<AbstractWidget> source) {
		for (AbstractWidget widget : source) {
			if (widget != null && !target.contains(widget)) {
				target.add(widget);
			}
		}
	}

	@Unique
	private static boolean isTitleMenuButton(AbstractWidget widget, int menuLeft, int menuRight) {
		return widget.getWidth() >= 98
				&& widget.getX() >= menuLeft - 4
				&& widget.getX() + widget.getWidth() <= menuRight + 4;
	}

	@Unique
	private static void moveBottomMenuRowUp(List<AbstractWidget> vanillaMenuButtons, int menuBottom) {
		for (AbstractWidget widget : vanillaMenuButtons) {
			if (widget.getBottom() == menuBottom) {
				widget.setPosition(widget.getX(), widget.getY() - MAIN_MENU_ICON_ROW_GAP);
			}
		}
	}

	@Unique
	private static void moveButtonsBelowInsertedRow(List<AbstractWidget> vanillaMenuButtons, int insertedRowY, int firstRowY) {
		List<Integer> rows = vanillaMenuButtons.stream()
				.map(AbstractWidget::getY)
				.filter(y -> y > insertedRowY)
				.distinct()
				.sorted()
				.toList();
		if (rows.isEmpty()) {
			return;
		}
		int originalFirstRowY = rows.getFirst();
		for (AbstractWidget widget : vanillaMenuButtons) {
			if (widget.getY() > insertedRowY) {
				widget.setPosition(widget.getX(), firstRowY + widget.getY() - originalFirstRowY);
			}
		}
	}

	@Unique
	private static List<Integer> menuRowYs(List<AbstractWidget> vanillaMenuButtons) {
		return vanillaMenuButtons.stream().map(AbstractWidget::getY).toList();
	}

	@Unique
	private Integer iconRowY(List<Integer> iconRowYs) {
		return iconRowYs.stream().min(Integer::compareTo).orElse(null);
	}

	@Unique
	private boolean isIconRowWidget(AbstractWidget widget, MenuButton buttonId) {
		return widget == this.friends || (buttonId != null && widget.getWidth() <= 20);
	}

	@Unique
	private static MenuButton buttonIdFor(AbstractWidget widget) {
		if (ModMenuIntegration.isModsIconButton(widget)) {
			return MenuButton.MODS;
		}

		if (!(widget instanceof SpriteIconButton button) || button instanceof FriendsButton) {
			return null;
		}

		Identifier sprite = ((SpriteIconButtonAccessor) button).better_menu_buttons$sprite().enabled();
		if (ModMenuIntegration.isNeoForgeLoaded() && ModMenuIntegration.NEOFORGE_MODS_SPRITE.equals(sprite)) {
			return MenuButton.MODS;
		}
		if (LANGUAGE_SPRITE.equals(sprite)) {
			return MenuButton.LANGUAGE;
		}
		if (ACCESSIBILITY_SPRITE.equals(sprite)) {
			return MenuButton.ACCESSIBILITY;
		}
		return null;
	}
}