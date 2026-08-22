/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.snapshot;

import com.google.api.services.drive.model.File;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.GoogleDriveTarget;

// GoogleDriveスナップショットクラス - Googleドライブのバックアップを表す
public class GoogleDriveSnapshot implements Snapshot {

    private final String backupName;
    private final GoogleDriveTarget storage;

    // 初期化する
    GoogleDriveSnapshot(GoogleDriveTarget storage, String backupName) {
        this.backupName = backupName;
        this.storage = storage;
    }

    @Override

    // ストレージを取得する
    public GoogleDriveTarget getStorage() {
        return storage;
    }

    @Override

    // 名前を取得する
    public String getName() {
        return backupName;
    }

    @Override

    // バイトサイズを計算する
    public long calculateByteSize() {
        try {

            String stringSize = getDriveFile().getAppProperties().get("size");
            return Long.parseLong(stringSize);
        } catch (Exception e) {
            return Snapshot.super.calculateByteSize();
        }
    }

    // サイズをファイルプロパティに保存する
    public boolean saveSizeToFileProperties(long byteSize) {

        try {
            storage.addProperty(getDriveFile().getId(), "size", String.valueOf(byteSize));
            return true;
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to save backup size to Google Drive");
            MakeBackup.getInstance().getLogManager().warn(e);
            return false;
        }
    }

    // ドライブファイルを取得する
    public File getDriveFile() {
        try {
            return storage.getFileByName(backupName + (BackupFileType.ZIP.equals(getFileType()) ? ".zip" : ""),
                    storage.getConfig().getBackupsFolderId());
        } catch (Exception e) {
            return null;
        }
    }
}
