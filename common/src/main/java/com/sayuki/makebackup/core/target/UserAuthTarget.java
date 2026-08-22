/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.core.target.error.TargetConnectionException;

// ユーザー認証ターゲットインターフェース - 認証操作を定義する
public interface UserAuthTarget extends Target {

    // 強制的に認証する
    void authorizeForced(ModCommandSender sender) throws TargetConnectionException;

    // コードで認証する
    void authorizeWithCode(String code, ModCommandSender sender) throws TargetConnectionException;
}
