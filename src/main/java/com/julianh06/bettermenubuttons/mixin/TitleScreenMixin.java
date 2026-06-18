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
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	@Unique private static final int MAIN_MENU_ICON_ROW_GAP = 12;
	@Unique private static final int VANILLA_MENU_WIDTH = 204;
	@Unique private static final Identifier LANGUAGE_SPRITE = Identifier.withDefaultNamespace("icon/language");
	@Unique private static final Identifier ACCESSIBILITY_SPRITE = Identifier.withDefaultNamespace("icon/accessibility");

	@Shadow private FriendsButton friends;
	@Unique private AbstractWidget better_menu_buttons$configButton;
	@Unique private boolean better_menu_buttons$layoutApplied;
	@Unique private int better_menu_buttons$modMenuLayoutState;

	private TitleScreenMixin() {
		super(Component.empty());
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void better_menu_buttons$layoutFriendsButton(CallbackInfo callbackInfo) {
		this.better_menu_buttons$layoutApplied = false;
		this.better_menu_buttons$modMenuLayoutState = ModMenuIntegration.buttonLayoutState(MenuArea.MAIN_MENU);
		this.better_menu_buttons$configButton = this.addRenderableWidget(new BetterMenuButtonsIconButton(0, 0, button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new BetterMenuButtonsConfigScreen(this, MenuArea.MAIN_MENU));
			}
		}));
		this.better_menu_buttons$configButton.setTooltip(Tooltip.create(Component.translatable("better-menu-buttons.options_button")));
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void better_menu_buttons$layoutFriendsButtonAfterInit(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks, CallbackInfo callbackInfo) {
		int modMenuLayoutState = ModMenuIntegration.buttonLayoutState(MenuArea.MAIN_MENU);
		if (this.better_menu_buttons$layoutApplied && this.better_menu_buttons$modMenuLayoutState == modMenuLayoutState) {
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
		List<AbstractWidget> vanillaMenuButtons = new ArrayList<>();

		int menuLeft = this.width / 2 - VANILLA_MENU_WIDTH / 2;
		int menuRight = menuLeft + VANILLA_MENU_WIDTH;
		int menuTop = Integer.MAX_VALUE;
		int menuBottom = 0;
		for (GuiEventListener child : this.children()) {
			if (!(child instanceof AbstractWidget widget)) {
				continue;
			}

			MenuButton buttonId = buttonIdFor(widget);
			if (buttonId != null) {
				moveableButtons.put(buttonId, widget);
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

		SocialButtonLayout.apply(this.width, this.height, menuTop, menuBottom, MenuArea.MAIN_MENU, moveableButtons);
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
	private static MenuButton buttonIdFor(AbstractWidget widget) {
		if (ModMenuIntegration.isModsIconButton(widget)) {
			return MenuButton.MODS;
		}

		if (!(widget instanceof SpriteIconButton button) || button instanceof FriendsButton) {
			return null;
		}

		Identifier sprite = ((SpriteIconButtonAccessor) button).better_menu_buttons$sprite().enabled();
		if (LANGUAGE_SPRITE.equals(sprite)) {
			return MenuButton.LANGUAGE;
		}
		if (ACCESSIBILITY_SPRITE.equals(sprite)) {
			return MenuButton.ACCESSIBILITY;
		}
		return null;
	}
}
