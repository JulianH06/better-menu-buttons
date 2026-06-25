package com.julianh06.bettermenubuttons.mixin;

import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OptionsScreen.class)
public interface OptionsScreenAccessor {
	@Accessor("inWorld")
	boolean better_menu_buttons$inWorld();
}