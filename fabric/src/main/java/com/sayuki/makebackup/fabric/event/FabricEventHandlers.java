/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.fabric.event;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.fabric.platform.FabricPlayer;
import net.minecraft.server.level.ServerPlayer;

// Fabricイベントハンドラークラス - イベントを処理する
public class FabricEventHandlers {

    // インスタンス化を禁止する
    private FabricEventHandlers() {
    }

    // プレイヤー参加時に処理する
    public static void onPlayerJoin(ServerPlayer player) {
        if (!MakeBackup.getInstance().isEnabled()) return;
        MakeBackup.getInstance().getConfigManager().updateLastChange();
        MakeBackup.getInstance().getStorageManager().onPlayerJoin(new FabricPlayer(player));
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
