/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.platform;

// プレイヤーインターフェース - プレイヤー固有の操作を扱う
public interface ModPlayer extends ModCommandSender {

    // コンソールかどうか判定する - 常にfalseを返す
    default boolean isConsole() {
        return false;
    }

    // プレイヤーかどうか判定する - 常にtrueを返す
    default boolean isPlayer() {
        return true;
    }

    // OPかどうか判定する
    default boolean isOp() {
        return hasPermission("makebackup.op");
    }

    // プレイヤーとして取得する
    default ModPlayer asPlayer() {
        return this;
    }
}
