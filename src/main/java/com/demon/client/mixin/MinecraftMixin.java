package com.demon.client.mixin;

import com.demon.client.gui.DemonTitleScreen;
import com.demon.client.module.modules.InterfaceModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** Swap the vanilla title screen for the Demon main menu. */
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen demon$replaceTitle(Screen screen) {
		if (screen instanceof TitleScreen && InterfaceModule.customMenu()) {
			return new DemonTitleScreen();
		}
		return screen;
	}
}
