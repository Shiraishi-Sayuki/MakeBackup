/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.forge;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.forge.event.ForgeEventHandlers;
import com.sayuki.makebackup.forge.platform.ForgePlatformHelper;
import com.sayuki.makebackup.platform.Services;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("makebackup")

// Forgeメインクラス - Forge環境の初期化をする
public class MakeBackupForge {

    private static MakeBackup instance;

    // 初期化する
    public MakeBackupForge() {
        instance = new MakeBackup();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    // 共通セットアップを処理する
    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(instance::onEnable);
    }

    @SubscribeEvent
    // サーバー開始時に処理する
    public void onServerStarting(ServerStartingEvent event) {
        Services.PLATFORM.setServer(event.getServer());
    }

    @SubscribeEvent
    // サーバー停止時に処理する
    public void onServerStopping(ServerStoppingEvent event) {
        instance.shutdown();
    }

    @SubscribeEvent
    // コマンド登録時に処理する
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ForgeCommandRegistry.register(event.getDispatcher(), Services.PLATFORM.getCommandTrees());
    }

    @SubscribeEvent
    // プレイヤー参加時に処理する
    public void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        ForgeEventHandlers.onPlayerJoin(event.getEntity());
    }

    @SubscribeEvent
    // プレイヤー切断時に処理する
    public void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        ForgeEventHandlers.onPlayerDisconnect();
    }

    @SubscribeEvent
    // ブロック破壊時に処理する
    public void onBlockBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        ForgeEventHandlers.onWorldChange();
    }

    @SubscribeEvent
    // ブロック設置時に処理する
    public void onBlockPlace(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        ForgeEventHandlers.onWorldChange();
    }

    @SubscribeEvent
    // プレイヤー操作時に処理する
    public void onPlayerInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent event) {
        ForgeEventHandlers.onWorldChange();
    }

    @SubscribeEvent
    // エンティティ死亡時に処理する
    public void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player) {
            ForgeEventHandlers.onWorldChange();
        }
    }

    @SubscribeEvent
    // アイテムドロップ時に処理する
    public void onItemToss(net.minecraftforge.event.entity.item.ItemTossEvent event) {
        ForgeEventHandlers.onWorldChange();
    }
}
