/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

// パスターゲット設定インターフェース - パス系ストレージの設定を扱う
public interface PathTargetSettings extends TargetSettings {

    // パス区切り文字を取得する
    String getPathSeparatorSymbol();
}
