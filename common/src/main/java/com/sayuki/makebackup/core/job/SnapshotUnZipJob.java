/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;

// スナップショット解凍ジョブクラス - ZIPをフォルダに戻す
public class SnapshotUnZipJob extends BaseJob {

    private final Snapshot backup;
    private UnpackZipJob unZipTask;
    private DeleteDirJob deleteZipTask;

    // コンストラクタ - バックアップで初期化する
    public SnapshotUnZipJob(Snapshot backup) {
        super();
        this.backup = backup;
    }

    @Override
    // 実行する
    public void run() {
        try {
            if (!cancelled) MakeBackup.getInstance().getTaskManager().startTaskRaw(unZipTask, sender);
            if (!cancelled) {
                backup.getStorage().getBackupManager().invalidateBackupSizeCache(backup.getName());
                MakeBackup.getInstance().getTaskManager().startTaskRaw(deleteZipTask, sender);
                backup.getStorage().renameFile(backup.getInProgressPath(Snapshot.BackupFileType.DIR), backup.getFileName(Snapshot.BackupFileType.DIR));
                backup.getStorage().getBackupManager().saveBackupSizeToCache(backup.getName(), unZipTask.getBytesUploaded());
            }
        } catch (JobException e) {
            warn(e);
        }
    }

    @Override
    // タスクを準備する
    public void prepareTask(ModCommandSender sender) throws Throwable {
        if (cancelled) return;
        deleteZipTask = new DeleteDirJob(backup.getStorage(), backup.getPath());
        deleteZipTask.maxProgress = backup.getByteSize();
        MakeBackup.getInstance().getTaskManager().prepareTask(deleteZipTask, sender);
        backup.getStorage().createDir(backup.getInProgressFileName(Snapshot.BackupFileType.DIR), backup.getStorage().getConfig().getBackupsFolder());
        unZipTask = new UnpackZipJob(backup.getStorage(), backup.getPath(), backup.getInProgressPath(Snapshot.BackupFileType.DIR));
        unZipTask.maxProgress = backup.getByteSize();
        MakeBackup.getInstance().getTaskManager().prepareTask(unZipTask, sender);
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
        if (unZipTask != null) MakeBackup.getInstance().getTaskManager().cancelTaskRaw(unZipTask);
        if (deleteZipTask != null) MakeBackup.getInstance().getTaskManager().cancelTaskRaw(deleteZipTask);
    }

    @Override
    // 最大進捗を取得する
    public long getTaskMaxProgress() {
        if (!isTaskPrepared()) return 0;
        return unZipTask.getTaskMaxProgress() * backup.getStorage().getZipProgressMultiplier() +
                deleteZipTask.getTaskMaxProgress() * backup.getStorage().getDeleteProgressMultiplier();
    }

    @Override
    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        if (!isTaskPrepared()) return 0;
        return unZipTask.getTaskCurrentProgress() * backup.getStorage().getZipProgressMultiplier() +
                deleteZipTask.getTaskCurrentProgress() * backup.getStorage().getDeleteProgressMultiplier();
    }
}
