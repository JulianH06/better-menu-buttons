package com.julianh06.bettermenubuttons.mixin;

import com.julianh06.bettermenubuttons.compat.ModMenuIntegration;
import com.julianh06.bettermenubuttons.config.MenuArea;
import com.julianh06.bettermenubuttons.config.MenuButton;
import com.julianh06.bettermenubuttons.config.ModMenuIconButton;
import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfigScreen;
import com.julianh06.bettermenubuttons.config.BetterMenuButtonsIconButton;
import com.julianh06.bettermenubuttons.layout.SocialButtonLayout;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
	private static final int FALLBACK_OPTIONS_MENU_WIDTH = 308;
	private static final int BUTTON_HEIGHT = 20;

	private OptionsScreenMixin() {
		super(Component.empty());
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void better_menu_buttons$addConfigButton(CallbackInfo callbackInfo) {
		MenuBounds menuBounds = this.better_menu_buttons$menuBounds();
		EnumMap<MenuButton, AbstractWidget> moveableButtons = new EnumMap<>(MenuButton.class);
		BetterMenuButtonsIconButton configButton = this.addRenderableWidget(new BetterMenuButtonsIconButton(0, 0, button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new BetterMenuButtonsConfigScreen(this, MenuArea.OPTIONS_MENU));
			}
		}));
		configButton.setTooltip(Tooltip.create(Component.translatable("better-menu-buttons.options_button")));
		moveableButtons.put(MenuButton.BETTER_MENU_BUTTONS, configButton);

		if (ModMenuIntegration.isLoaded()) {
			ModMenuIconButton modsButton = this.addRenderableWidget(new ModMenuIconButton(0, 0, button -> {
				if (this.minecraft != null) {
					this.minecraft.setScreenAndShow(ModMenuIntegration.createModsScreen(this));
				}
			}));
			modsButton.setTooltip(Tooltip.create(Component.literal("Mods")));
			moveableButtons.put(MenuButton.MODS, modsButton);
		}

		SocialButtonLayout.apply(this.width, this.height, menuBounds.left(), menuBounds.right(), menuBounds.top(), menuBounds.bottom(), MenuArea.OPTIONS_MENU, moveableButtons);
	}

	private MenuBounds better_menu_buttons$menuBounds() {
		List<Integer> rowYs = new ArrayList<>();

		for (GuiEventListener child : this.children()) {
			if (!(child instanceof AbstractWidget widget) || widget.getWidth() < 98) {
				continue;
			}

			if (!rowYs.contains(widget.getY())) {
				rowYs.add(widget.getY());
			}
		}
		rowYs.sort(Integer::compareTo);

		int menuLeft = this.width / 2 - FALLBACK_OPTIONS_MENU_WIDTH / 2;
		int menuRight = menuLeft + FALLBACK_OPTIONS_MENU_WIDTH;
		int menuTop = this.height / 4;
		int menuBottom = menuTop + BUTTON_HEIGHT;

		if (rowYs.size() >= 3) {
			int firstPositionableRow = rowYs.get(1);
			int lastPositionableRow = rowYs.get(rowYs.size() - 2);
			menuTop = firstPositionableRow;
			menuBottom = lastPositionableRow + BUTTON_HEIGHT;

			for (GuiEventListener child : this.children()) {
				if (!(child instanceof AbstractWidget widget) || widget.getWidth() < 98) {
					continue;
				}

				if (widget.getY() <= lastPositionableRow) {
					menuLeft = Math.min(menuLeft, widget.getX());
					menuRight = Math.max(menuRight, widget.getRight());
				}
			}
		}

		return new MenuBounds(menuLeft, menuRight, menuTop, menuBottom);
	}

	private record MenuBounds(int left, int right, int top, int bottom) {
	}
}
