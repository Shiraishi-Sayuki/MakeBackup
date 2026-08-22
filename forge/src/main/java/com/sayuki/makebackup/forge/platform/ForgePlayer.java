/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.forge.platform;

import com.sayuki.makebackup.platform.ModPlayer;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

// Forgeプレイヤークラス - Forgeのプレイヤーをラップする
public class ForgePlayer implements ModPlayer {

    private final Player player;

    // 初期化する
    public ForgePlayer(Player player) {
        this.player = player;
    }

    // プレイヤーを取得する
    public Player getPlayer() {
        return player;
    }

    @Override
    // 名前を取得する
    public String getName() {
        return player.getGameProfile().getName();
    }

    @Override
    // コンソールか確認する
    public boolean isConsole() {
        return false;
    }

    @Override
    // プレイヤーか確認する
    public boolean isPlayer() {
        return true;
    }

    @Override
    // OPか確認する
    public boolean isOp() {
        return player.hasPermissions(2);
    }

    @Override
    // UUIDを取得する
    public UUID getUniqueId() {
        return player.getUUID();
    }

    @Override
    // メッセージを送信する
    public void sendMessage(Component message) {
        player.sendSystemMessage(AdventureBridge.toMinecraft(message));
    }

    @Override
    // メッセージを送信する
    public void sendMessage(String message) {
        sendMessage(Component.text(message));
    }

    @Override
    // 権限をチェックする
    public boolean hasPermission(String permission) {
        return player.hasPermissions(2);
    }

    @Override
    // サウンドを再生する
    public void playSound(String soundKey, float volume, float pitch) {
        try {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse(soundKey));
            if (sound != null) {
                player.playSound(sound, volume, pitch);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    // プレイヤーとして取得する
    public ModPlayer asPlayer() {
        return this;
    }
}
