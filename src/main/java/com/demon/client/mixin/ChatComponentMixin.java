package com.demon.client.mixin;

import com.demon.client.module.ModuleManager;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
		at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
	private Component demon$timestamp(Component message) {
		if (ModuleManager.chatTimestamps == null || !ModuleManager.chatTimestamps.isEnabled()) return message;
		String t = LocalTime.now().format(DateTimeFormatter.ofPattern(ModuleManager.chatTimestamps.seconds.on() ? "HH:mm:ss" : "HH:mm"));
		return Component.literal("[" + t + "] ").withColor(0x5B8DEF).append(message);
	}
}
