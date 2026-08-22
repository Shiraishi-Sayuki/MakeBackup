/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.forge.event;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.forge.platform.ForgePlayer;
import net.minecraft.world.entity.player.Player;

// Forgeイベントハンドラークラス - イベントを処理する
public class ForgeEventHandlers {

    // インスタンス化を禁止する
    private ForgeEventHandlers() {
    }

    // プレイヤー参加時に処理する
    public static void onPlayerJoin(Player player) {
        if (!MakeBackup.getInstance().isEnabled()) return;
        MakeBackup.getInstance().getConfigManager().updateLastChange();
        MakeBackup.getInstance().getStorageManager().onPlayerJoin(new ForgePlayer(player));
    }

    // プレイヤー切断時に処理する
    public static void onPlayerDisconnect() {
        if (!MakeBackup.getInstance().isEnabled()) return;
        MakeBackup.getInstance().getConfigManager().updateLastChange();
    }

    // ワールド変更時に処理する
    public static void onWorldChange() {
        if (!MakeBackup.getInstance().isEnabled()) return;
        MakeBackup.getInstance().getConfigManager().updateLastChange();
    }
}
