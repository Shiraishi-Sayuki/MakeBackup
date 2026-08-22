/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target.error;

import com.sayuki.makebackup.core.target.Target;

// ターゲット制限例外クラス - 制限超過エラーを表す
public class TargetLimitException extends RuntimeException {

    // コンストラクタ - ストレージとメッセージで初期化する
    public TargetLimitException(Target storage, String message) {
        super("Target: %s. %s".formatted(storage.getId(), message));
    }

    // コンストラクタ - ストレージとメッセージと例外で初期化する
    public TargetLimitException(Target storage, String message, Exception e) {
        super("Target: %s. %s\n%s".formatted(storage.getId(), message, e.getMessage()), e);
        this.setStackTrace(e.getStackTrace());
    }
}
