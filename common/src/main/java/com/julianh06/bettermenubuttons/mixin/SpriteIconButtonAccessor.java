package com.julianh06.bettermenubuttons.mixin;

import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteIconButton.class)
public interface SpriteIconButtonAccessor {
	@Accessor("sprite")
	WidgetSprites better_menu_buttons$sprite();
}