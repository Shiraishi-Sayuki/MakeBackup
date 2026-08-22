/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.forge.platform;

import com.sayuki.makebackup.platform.ModCommandSender;
import com.sayuki.makebackup.platform.ModPlayer;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

// Forge送信者クラス - コマンド送信者をラップする
public class ForgeSender implements ModCommandSender {

    private static final Logger LOG = LoggerFactory.getLogger("MakeBackup");

    private static final UUID CONSOLE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final CommandSourceStack source;
    private final ServerPlayer player;

    // 初期化する
    private ForgeSender(CommandSourceStack source, ServerPlayer player) {
        this.source = source;
        this.player = player;
    }

    // コンソール送信者を作成する
    public static ForgeSender console() {
        return new ForgeSender(null, null);
    }

    // プレイヤー送信者を作成する
    public static ForgeSender player(ServerPlayer player) {
        return new ForgeSender(null, player);
    }

    // コマンドソースから作成する
    public static ForgeSender source(CommandSourceStack source) {
        return new ForgeSender(source, null);
    }

    @Override
    // 名前を取得する
    public String getName() {
        if (player != null) return player.getGameProfile().getName();
        if (source != null) return source.getTextName();
        return "Console";
    }

    @Override
    // コンソールか確認する
    public boolean isConsole() {
        return player == null;
    }

    @Override
    // プレイヤーか確認する
    public boolean isPlayer() {
        return player != null;
    }

    @Override
    // OPか確認する
    public boolean isOp() {
        return player != null ? player.hasPermissions(2) : true;
    }

    @Override
    // UUIDを取得する
    public UUID getUniqueId() {
        return player != null ? player.getUUID() : CONSOLE_UUID;
    }

    @Override
    // メッセージを送信する
    public void sendMessage(Component message) {
        try {
            if (player != null) {
                player.sendSystemMessage(AdventureBridge.toMinecraft(message), false);
            } else if (source != null) {
                source.sendSystemMessage(AdventureBridge.toMinecraft(message));
            } else {
                LOG.info(AdventureBridge.toPlain(message));
            }
        } catch (Exception e) {
            LOG.warn("Failed to send message: {}", e.getMessage());
        }
    }

    @Override
    // メッセージを送信する
    public void sendMessage(String message) {
        sendMessage(Component.text(message));
    }

    @Override
    // 権限をチェックする
    public boolean hasPermission(String permission) {
        return player != null ? player.hasPermissions(2) : true;
    }

    @Override
    // サウンドを再生する
    public void playSound(String soundKey, float volume, float pitch) {
        if (player == null) return;
        try {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse(soundKey));
            if (sound != null) {
                player.playSound(sound, volume, pitch);
            }
        } catch (Exception e) {
            LOG.warn("Failed to play sound {}: {}", soundKey, e.getMessage());
        }
    }

    @Override
    // プレイヤーとして取得する
    public ModPlayer asPlayer() {
        return player != null ? new ForgePlayer(player) : null;
    }
}
