/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.snapshot;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.*;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;
import com.sayuki.makebackup.core.target.error.TargetMethodException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

// スナップショットマネージャークラス - バックアップの管理を行う
public class SnapshotManager {

    private final Target storage;

    private final HashMap<String, Snapshot> backups = new HashMap<>();

    final Cache<String, Long> cachedBackupsSize = Caffeine.newBuilder().build();
    private final Cache<String, List<Snapshot>> cacheGetBackupList = Caffeine
            .newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .build();

    // 初期化する
    public SnapshotManager(Target storage) {
        this.storage = storage;
    }

    // バックアップを取得する
    public Snapshot getBackup(String backupName) {
        if (!checkBackupExists(backupName)) {
            return null;
        }

        if (storage instanceof LocalTarget localStorage) {
            return backups.computeIfAbsent(backupName, (name) -> new LocalSnapshot(localStorage, name));
        } else if (storage instanceof FtpTarget ftpStorage) {
            return backups.computeIfAbsent(backupName, (name) -> new FtpSnapshot(ftpStorage, name));
        } else if (storage instanceof SftpTarget sftpStorage) {
            return backups.computeIfAbsent(backupName, (name) -> new SftpSnapshot(sftpStorage, name));
        } else if (storage instanceof GoogleDriveTarget googleDriveStorage) {
            return backups.computeIfAbsent(backupName, (name) -> new GoogleDriveSnapshot(googleDriveStorage, name));
        }
        return null;
    }

    // バックアップリストを取得する
    public List<Snapshot> getBackupList() throws TargetConnectionException, TargetMethodException {
        return cacheGetBackupList.get("all", (key) -> {
            List<Snapshot> backups = new ArrayList<>();
            for (String fileName : storage.ls(storage.getConfig().getBackupsFolder())) {
                Snapshot backup = getBackup(fileName.replace(".zip", ""));
                if (backup != null) {
                    backups.add(backup);
                }
            }
            return backups;
        });
    }

    // バックアップの存在を確認する
    public boolean checkBackupExists(String backupName) {
        try {
            LocalDateTime.parse(backupName, MakeBackup.getInstance().getConfigManager().getBackupConfig().getDateTimeFormatter());
            return storage.ls(storage.getConfig().getBackupsFolder()).stream().anyMatch(file -> file.equals(backupName) || file.equals("%s.zip".formatted(backupName)));
        } catch (Exception e) {
            return false;
        }
    }

    // バックアップサイズをキャッシュに保存する
    public void saveBackupSizeToCache(String backupName, long byteSize) {
        cachedBackupsSize.put(backupName, byteSize);

        if (TargetType.GOOGLE_DRIVE.equals(storage.getType())) {
            GoogleDriveSnapshot backup = (GoogleDriveSnapshot) getBackup(backupName);
            if (backup == null) throw new RuntimeException("Tried to save nonexistent backup's size to cache");
            backup.saveSizeToFileProperties(byteSize);
        }
    }

    // バックアップサイズのキャッシュを無効化する
    public void invalidateBackupSizeCache(String backupName) {
        cachedBackupsSize.invalidate(backupName);
    }

    // サイズキャッシュを取得する
    public ConcurrentMap<String, Long> getSizeCache() {
        return cachedBackupsSize.asMap();
    }

    // サイズキャッシュを読み込む
    public void loadSizeCache(Map<String, Long> cache) {
        cachedBackupsSize.invalidateAll();
        cachedBackupsSize.putAll(cache);
    }
}
