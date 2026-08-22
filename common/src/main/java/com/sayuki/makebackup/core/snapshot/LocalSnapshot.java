/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.snapshot;

import com.sayuki.makebackup.core.target.LocalTarget;

import java.io.File;

// ローカルスナップショットクラス - ローカルのバックアップを表す
public class LocalSnapshot implements Snapshot {

    private final String backupName;
    private final LocalTarget storage;

    // 初期化する
    LocalSnapshot(LocalTarget storage, String backupName) {
        this.backupName = backupName;
        this.storage = storage;
    }

    @Override

    // 名前を取得する
    public String getName() {
        return backupName;
    }

    // ファイルを取得する
    public File getFile() {
        File backupsFolder = new File(storage.getConfig().getBackupsFolder());

        if (BackupFileType.ZIP.equals(this.getFileType())) {
            return backupsFolder.toPath().resolve("%s.zip".formatted(backupName)).toFile();
        } else {
            return backupsFolder.toPath().resolve(backupName).toFile();
        }
    }

    @Override

    // ストレージを取得する
    public LocalTarget getStorage() {
        return storage;
    }
}
