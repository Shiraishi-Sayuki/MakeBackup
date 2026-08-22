/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.snapshot;

import com.sayuki.makebackup.core.target.FtpTarget;

// FTPスナップショットクラス - FTPのバックアップを表す
public class FtpSnapshot implements Snapshot {

    private final String backupName;
    private final FtpTarget storage;

    // 初期化する
    FtpSnapshot(FtpTarget storage, String backupName) {
        this.backupName = backupName;
        this.storage = storage;
    }

    @Override

    // ストレージを取得する
    public FtpTarget getStorage() {
        return storage;
    }

    @Override

    // 名前を取得する
    public String getName() {
        return backupName;
    }
}
