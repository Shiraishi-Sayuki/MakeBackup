/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.platform;

import java.io.File;

// ワールドインターフェース - ワールド情報を扱う
public interface ModWorld {

    // 名前を取得する
    String getName();

    // ワールドフォルダを取得する
    File getWorldFolder();

    // 自動保存かどうか判定する
    boolean isAutoSave();

    // 自動保存を設定する
    void setAutoSave(boolean autoSave);
}
