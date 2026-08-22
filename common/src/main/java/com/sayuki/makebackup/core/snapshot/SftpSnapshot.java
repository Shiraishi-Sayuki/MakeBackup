/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.snapshot;

import com.sayuki.makebackup.core.target.SftpTarget;

// SFTPスナップショットクラス - SFTPのバックアップを表す
public class SftpSnapshot implements Snapshot {

    private final String backupName;
    private final SftpTarget storage;

    // 初期化する
    SftpSnapshot(SftpTarget storage, String backupName) {
        this.storage = storage;
        this.backupName = backupName;
    }

    @Override

    // 名前を取得する
    public String getName() {
        return backupName;
    }

    @Override

    // ストレージを取得する
    public SftpTarget getStorage() {
        return storage;
    }
}
