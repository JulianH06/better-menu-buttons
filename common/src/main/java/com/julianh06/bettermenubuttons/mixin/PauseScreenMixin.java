package com.julianh06.bettermenubuttons.mixin;

import com.julianh06.bettermenubuttons.config.MenuArea;
import com.julianh06.bettermenubuttons.config.MenuButton;
import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfigScreen;
import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfig;
import com.julianh06.bettermenubuttons.config.BetterMenuButtonsIconButton;
import com.julianh06.bettermenubuttons.compat.ModMenuIntegration;
import com.julianh06.bettermenubuttons.layout.SocialButtonLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	@Unique private static final int PAUSE_MENU_SOCIAL_ROW_Y_OFFSET = 52;
	@Unique private static final int PAUSE_MENU_ICON_ROW_GAP = 24;
	@Unique private static final int VANILLA_MENU_WIDTH = 204;
	@Unique private static final Identifier BUG_REPORT_SPRITE = Identifier.withDefaultNamespace("pause_menu/bug");
	@Unique private static final Identifier SOCIAL_INTERACTIONS_SPRITE = Identifier.withDefaultNamespace("pause_menu/social_interactions");
	@Unique private static final Identifier PLAYER_REPORTING_SPRITE = Identifier.withDefaultNamespace("pause_menu/player_reporting");

	@Shadow	private FriendsButton friends;
	@Unique private AbstractWidget better_menu_buttons$configButton;
	@Unique private boolean better_menu_buttons$layoutApplied;
	@Unique private int better_menu_buttons$modMenuLayoutState;

	private PauseScreenMixin() {
		super(Component.empty());
	}

	@Inject(method = "createPauseMenu", at = @At("TAIL"))
	private void better_menu_buttons$layoutPauseSocialButtons(CallbackInfo callbackInfo) {
		this.better_menu_buttons$layoutApplied = false;
		this.better_menu_buttons$modMenuLayoutState = ModMenuIntegration.buttonLayoutState(MenuArea.PAUSE_MENU);
		this.better_menu_buttons$configButton = this.addRenderableWidget(new BetterMenuButtonsIconButton(0, 0, button -> {
			if (this.minecraft != null) {
				Screen screen = new BetterMenuButtonsConfigScreen(this, MenuArea.PAUSE_MENU);
				this.minecraft.getInstance().gui.setScreen(screen);
			}
		}));
		this.better_menu_buttons$configButton.setTooltip(Tooltip.create(BetterMenuButtonsIconButton.label()));
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void better_menu_buttons$layoutPauseSocialButtonsAfterInit(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks, CallbackInfo callbackInfo) {
		int modMenuLayoutState = ModMenuIntegration.buttonLayoutState(MenuArea.PAUSE_MENU);
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
		EnumMap<MenuButton, AbstractWidget> moveableButtons = new EnumMap<>(MenuButton.class);
		if (this.better_menu_buttons$configButton != null) {
			moveableButtons.put(MenuButton.BETTER_MENU_BUTTONS, this.better_menu_buttons$configButton);
		}
		if (this.friends != null) {
			moveableButtons.put(MenuButton.FRIENDS, this.friends);
		}

		int menuTop = Integer.MAX_VALUE;
		int menuBottom = 0;
		int insertedRowY = Integer.MAX_VALUE;
		AbstractWidget insertedRowButton = null;
		List<AbstractWidget> vanillaMenuButtons = new ArrayList<>();
		List<Integer> iconRowYs = new ArrayList<>();
		for (AbstractWidget widget : this.better_menu_buttons$widgets()) {
			if (widget == null) {
				continue;
			}

			MenuButton buttonId = buttonIdFor(widget);
			if (buttonId != null) {
				moveableButtons.put(buttonId, widget);
			}
			if (this.isIconRowWidget(widget, buttonId)) {
				iconRowYs.add(widget.getY());
			}
			if (ModMenuIntegration.isModsRowButton(widget)) {
				insertedRowY = Math.min(insertedRowY, widget.getY());
				insertedRowButton = widget;
			}

			if (widget.getWidth() >= 98) {
				vanillaMenuButtons.add(widget);
				menuTop = Math.min(menuTop, widget.getY());
				menuBottom = Math.max(menuBottom, widget.getBottom());
			}
		}

		if (menuTop == Integer.MAX_VALUE) {
			menuTop = 0;
			menuBottom = this.height / 2;
		}

		if (!BetterMenuButtonsConfig.hasVanillaIconRowButtons(MenuArea.PAUSE_MENU) && !ModMenuIntegration.hasInsertedRow(MenuArea.PAUSE_MENU)) {
			moveButtonsBelowSocialRowUp(vanillaMenuButtons, menuTop);
			menuBottom -= PAUSE_MENU_ICON_ROW_GAP;
		}

		Integer iconRowY = iconRowY(iconRowYs);
		if (ModMenuIntegration.hasInsertedRow(MenuArea.PAUSE_MENU) && BetterMenuButtonsConfig.hasVanillaIconRowButtons(MenuArea.PAUSE_MENU) && insertedRowButton != null && iconRowY != null) {
			insertedRowButton.setPosition(insertedRowButton.getX(), iconRowY);
			moveButtonsBelowInsertedRow(vanillaMenuButtons, insertedRowY, iconRowY + 48);
			menuTop = vanillaMenuButtons.stream().mapToInt(AbstractWidget::getY).min().orElse(menuTop);
			menuBottom = vanillaMenuButtons.stream().mapToInt(AbstractWidget::getBottom).max().orElse(menuBottom);
			insertedRowY = iconRowY;
		} else if (ModMenuIntegration.hasInsertedRow(MenuArea.PAUSE_MENU) && insertedRowY != Integer.MAX_VALUE) {
			int compactInsertedRowY = iconRowY == null ? insertedRowY - PAUSE_MENU_ICON_ROW_GAP : iconRowY;
			if (insertedRowButton != null) {
				insertedRowButton.setPosition(insertedRowButton.getX(), compactInsertedRowY);
			}
			moveButtonsBelowInsertedRow(vanillaMenuButtons, insertedRowY, compactInsertedRowY + PAUSE_MENU_ICON_ROW_GAP);
			menuTop = vanillaMenuButtons.stream().mapToInt(AbstractWidget::getY).min().orElse(menuTop);
			menuBottom = vanillaMenuButtons.stream().mapToInt(AbstractWidget::getBottom).max().orElse(menuBottom);
		}

		SocialButtonLayout.MenuGeometry geometry = SocialButtonLayout.geometry(MenuArea.PAUSE_MENU, menuTop, menuBottom, menuRowYs(vanillaMenuButtons), iconRowY);
		int menuLeft = this.width / 2 - VANILLA_MENU_WIDTH / 2;
		SocialButtonLayout.apply(this.width, this.height, menuLeft, menuLeft + VANILLA_MENU_WIDTH, geometry, MenuArea.PAUSE_MENU, moveableButtons);
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
	private static void moveButtonsBelowSocialRowUp(List<AbstractWidget> vanillaMenuButtons, int menuTop) {
		int socialRowY = menuTop + PAUSE_MENU_SOCIAL_ROW_Y_OFFSET;
		for (AbstractWidget widget : vanillaMenuButtons) {
			if (widget.getY() > socialRowY) {
				widget.setPosition(widget.getX(), widget.getY() - PAUSE_MENU_ICON_ROW_GAP);
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
		if (BUG_REPORT_SPRITE.equals(sprite)) {
			return MenuButton.BUG_REPORT;
		}
		if (SOCIAL_INTERACTIONS_SPRITE.equals(sprite)) {
			return MenuButton.FEEDBACK;
		}
		if (PLAYER_REPORTING_SPRITE.equals(sprite)) {
			return MenuButton.PLAYER_REPORTING;
		}
		return null;
	}
}