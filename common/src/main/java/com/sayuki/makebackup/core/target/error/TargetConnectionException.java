/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target.error;

import com.sayuki.makebackup.core.target.Target;

// ターゲット接続例外クラス - 接続エラーを表す
public class TargetConnectionException extends RuntimeException {

    // コンストラクタ - ストレージとメッセージで初期化する
    public TargetConnectionException(Target storage, String message) {
        super("Target: %s. %s".formatted(storage, message));
    }

    // コンストラクタ - ストレージとメッセージと例外で初期化する
    public TargetConnectionException(Target storage, String message, Exception e) {
        super("Target: %s. %s\n%s".formatted(storage, message, e.getMessage()), e);
        this.setStackTrace(e.getStackTrace());
    }
}
