/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;

import java.util.List;

// スナップショットZIP化ジョブクラス - フォルダをZIPに変換する
public class SnapshotToZipJob extends BaseJob {

    private final Snapshot backup;
    private TransferDirsAsZipJob toZipTask;
    private DeleteDirJob deleteFolderTask;

    // コンストラクタ - バックアップで初期化する
    public SnapshotToZipJob(Snapshot backup) {
        super();
        this.backup = backup;
    }

    @Override
    // 実行する
    public void run() {
        try {
            if (!cancelled) MakeBackup.getInstance().getTaskManager().startTaskRaw(toZipTask, sender);
            if (!cancelled) {
                backup.getStorage().getBackupManager().invalidateBackupSizeCache(backup.getName());
                MakeBackup.getInstance().getTaskManager().startTaskRaw(deleteFolderTask, sender);
                backup.getStorage().renameFile(backup.getInProgressPath(Snapshot.BackupFileType.ZIP), backup.getFileName(Snapshot.BackupFileType.ZIP));
            }
        } catch (Exception e) {
            warn(new JobException(toZipTask, e));
        }
    }

    @Override
    // タスクを準備する
    public void prepareTask(ModCommandSender sender) throws Throwable {
        if (cancelled) return;
        deleteFolderTask = new DeleteDirJob(backup.getStorage(), backup.getPath());
        deleteFolderTask.maxProgress = backup.getByteSize();
        MakeBackup.getInstance().getTaskManager().prepareTask(deleteFolderTask, sender);
        toZipTask = new TransferDirsAsZipJob(backup.getStorage(), List.of(backup.getPath()), backup.getStorage(), backup.getStorage().getConfig().getBackupsFolder(), backup.getInProgressFileName(Snapshot.BackupFileType.ZIP), false, true);
        toZipTask.maxProgress = backup.getByteSize();
        MakeBackup.getInstance().getTaskManager().prepareTask(toZipTask, sender);
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
        if (toZipTask != null) MakeBackup.getInstance().getTaskManager().cancelTaskRaw(toZipTask);
        if (deleteFolderTask != null) MakeBackup.getInstance().getTaskManager().cancelTaskRaw(deleteFolderTask);
    }

    @Override
    // 最大進捗を取得する
    public long getTaskMaxProgress() {
        if (!isTaskPrepared()) return 0;
        return toZipTask.getTaskMaxProgress() * backup.getStorage().getZipProgressMultiplier() +
                deleteFolderTask.getTaskMaxProgress() * backup.getStorage().getDeleteProgressMultiplier();
    }

    @Override
    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        if (!isTaskPrepared()) return 0;
        return toZipTask.getTaskCurrentProgress() * backup.getStorage().getZipProgressMultiplier() +
                deleteFolderTask.getTaskCurrentProgress() * backup.getStorage().getDeleteProgressMultiplier();
    }
}
