/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.platform;

import net.kyori.adventure.text.Component;

import java.util.UUID;

// コマンド送信者インターフェース - コマンド実行者を抽象化する
public interface ModCommandSender {

    // 名前を取得する
    String getName();

    // コンソールかどうか判定する
    boolean isConsole();

    // プレイヤーかどうか判定する
    boolean isPlayer();

    // OPかどうか判定する
    boolean isOp();

    // UUIDを取得する
    UUID getUniqueId();

    // メッセージを送信する - Component版
    void sendMessage(Component message);

    // メッセージを送信する - 文字列版
    void sendMessage(String message);

    // 権限を持っているか確認する
    boolean hasPermission(String permission);

    // サウンドを再生する
    void playSound(String soundKey, float volume, float pitch);

    // プレイヤーとして取得する - デフォルトはnullを返す
    default ModPlayer asPlayer() {
        return null;
    }
}
