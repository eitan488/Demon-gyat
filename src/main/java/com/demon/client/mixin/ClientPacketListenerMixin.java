package com.demon.client.mixin;

import com.demon.client.combat.CombatFx;
import com.demon.client.module.ModuleManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	private static final String ENSURE = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V";

	@Inject(method = "handleEntityEvent", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), require = 0)
	private void demon$entityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || packet.getEventId() != 35) return;
		Entity e = packet.getEntity(mc.level);
		if (e != null) CombatFx.onTotemPop(e);
	}

	@Inject(method = "handleSetTime", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), require = 0)
	private void demon$time(ClientboundSetTimePacket packet, CallbackInfo ci) {
		CombatFx.onTimePacket(packet.gameTime());
	}

	@Inject(method = "handleBlockUpdate", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), require = 0)
	private void demon$blockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
		if (ModuleManager.newChunks.isEnabled()) ModuleManager.newChunks.onBlockUpdate(packet.getPos(), packet.getBlockState());
	}

	@Inject(method = "handleChunkBlocksUpdate", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), require = 0)
	private void demon$sectionUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
		if (ModuleManager.newChunks.isEnabled()) packet.runUpdates((pos, state) -> ModuleManager.newChunks.onBlockUpdate(pos.immutable(), state));
	}

	@Inject(method = "handlePlayerInfoRemove", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), require = 0)
	private void demon$playerLeft(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
		if (!ModuleManager.logoutSpots.isEnabled()) return;
		ClientPacketListener self = (ClientPacketListener) (Object) this;
		List<String> names = new ArrayList<>();
		for (UUID id : packet.profileIds()) {
			PlayerInfo info = self.getPlayerInfo(id);
			names.add(info != null ? info.getProfile().name() : null);
		}
		ModuleManager.logoutSpots.onPlayersLeft(packet.profileIds(), names);
	}
}
