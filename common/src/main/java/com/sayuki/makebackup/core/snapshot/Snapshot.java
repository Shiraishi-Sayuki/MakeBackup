/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.snapshot;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.job.SnapshotDeleteJob;
import com.sayuki.makebackup.core.job.SnapshotToZipJob;
import com.sayuki.makebackup.core.job.SnapshotUnZipJob;

import java.time.LocalDateTime;

// スナップショットインターフェース - バックアップを表現する
public interface Snapshot extends Comparable<Snapshot> {

    // ストレージを取得する
    Target getStorage();

    // 日時を取得する
    default LocalDateTime getLocalDateTime() {
        return LocalDateTime.parse(getName(), MakeBackup.getInstance().getConfigManager().getBackupConfig().getDateTimeFormatter());
    }

    // 名前を取得する
    String getName();

    // フォーマット済み名前を取得する
    default String getFormattedName() {
        return getLocalDateTime().format(MakeBackup.getInstance().getConfigManager().getBackupConfig().getDateTimeFormatter());
    }

    // バイトサイズを計算する
    default long calculateByteSize() {
        return getStorage().getDirByteSize(getPath());
    }

    // バイトサイズを取得する - キャッシュ付き
    default long getByteSize() {
        return getStorage().getBackupManager().cachedBackupsSize.get(this.getName(), (key) -> calculateByteSize());
    }

    // MBサイズを取得する
    default long getMbSize() {
        return getByteSize() / 1024 / 1024;
    }

    // ファイルタイプを取得する
    default BackupFileType getFileType() {
        if (getStorage().ls(getStorage().getConfig().getBackupsFolder()).contains("%s.zip".formatted(getName()))) {
            return BackupFileType.ZIP;
        }
        return BackupFileType.DIR;
    }

    // ファイル名を取得する - タイプ指定
    default String getFileName(BackupFileType fileType) {
        if (BackupFileType.ZIP.equals(fileType)) {
            return "%s.zip".formatted(getName());
        } else {
            return getName();
        }
    }

    // ファイル名を取得する
    default String getFileName() {
        return getFileName(getFileType());
    }

    // 進行中ファイル名を取得する - タイプ指定
    default String getInProgressFileName(BackupFileType fileType) {
        if (BackupFileType.ZIP.equals(fileType)) {
            return "%s in progress.zip".formatted(getName());
        } else {
            return "%s in progress".formatted(getName());
        }
    }

    // 進行中ファイル名を取得する
    default String getInProgressFileName() {
        return getInProgressFileName(getFileType());
    }

    // パスを取得する - タイプ指定
    default String getPath(BackupFileType fileType) {
        return getStorage().resolve(getStorage().getConfig().getBackupsFolder(), getFileName(fileType));
    }

    // パスを取得する
    default String getPath() {
        return getPath(getFileType());
    }

    // 進行中パスを取得する - タイプ指定
    default String getInProgressPath(BackupFileType fileType) {
        return getStorage().resolve(getStorage().getConfig().getBackupsFolder(), getInProgressFileName(fileType));
    }

    // 進行中パスを取得する
    default String getInProgressPath() {
        return getInProgressPath(getFileType());
    }

    // 削除タスクを取得する
    default SnapshotDeleteJob getDeleteTask() {
        return new SnapshotDeleteJob(this);
    }

    // 解凍タスクを取得する
    default SnapshotUnZipJob getUnZipTask() {
        return new SnapshotUnZipJob(this);
    }

    // ZIP化タスクを取得する
    default SnapshotToZipJob getToZipTask() {
        return new SnapshotToZipJob(this);
    }

    @Override
    // 比較する - 日時でソートする
    default int compareTo(Snapshot backup) {
        return this.getLocalDateTime().compareTo(backup.getLocalDateTime());
    }

    // バックアップファイルタイプ列挙
    enum BackupFileType {
        DIR,
        ZIP
    }
}
