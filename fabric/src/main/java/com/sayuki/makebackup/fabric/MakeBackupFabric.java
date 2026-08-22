/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.fabric;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.fabric.event.FabricEventHandlers;
import com.sayuki.makebackup.fabric.platform.FabricPlatformHelper;
import com.sayuki.makebackup.platform.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.InteractionResult;

// Fabricメインクラス - Fabric環境の初期化をする
public class MakeBackupFabric implements ModInitializer {

    @Override
    // 初期化する
    public void onInitialize() {
        MakeBackup instance = new MakeBackup();
        instance.onEnable();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> Services.PLATFORM.setServer(server));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> instance.shutdown());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            FabricEventHandlers.onPlayerJoin(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            FabricEventHandlers.onPlayerDisconnect();
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!player.isSpectator()) {
                FabricEventHandlers.onWorldChange();
            }
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!player.isSpectator()) FabricEventHandlers.onWorldChange();
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!player.isSpectator()) FabricEventHandlers.onWorldChange();
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!player.isSpectator()) FabricEventHandlers.onWorldChange();
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!player.isSpectator()) FabricEventHandlers.onWorldChange();
            return InteractionResult.PASS;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                FabricEventHandlers.onWorldChange();
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            FabricCommandRegistry.register(dispatcher, Services.PLATFORM.getCommandTrees());
        });
    }
}
