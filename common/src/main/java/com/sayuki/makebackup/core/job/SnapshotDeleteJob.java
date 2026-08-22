/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;

// スナップショット削除ジョブクラス - 単一バックアップを削除する
public class SnapshotDeleteJob extends BaseJob {

    private final Snapshot backup;
    private DeleteDirJob deleteBackupTask;

    // コンストラクタ - バックアップで初期化する
    public SnapshotDeleteJob(Snapshot backup) {
        super();
        this.backup = backup;
    }

    @Override
    // 実行する
    public void run() {
        if (!cancelled) {
            try {
                MakeBackup.getInstance().getTaskManager().startTaskRaw(deleteBackupTask, sender);
            } catch (Exception e) {
                warn(new JobException(deleteBackupTask, e));
            }
            backup.getStorage().getBackupManager().invalidateBackupSizeCache(backup.getName());
        }
    }

    @Override
    // タスクを準備する
    public void prepareTask(ModCommandSender sender) throws Throwable {
        if (cancelled) return;
        deleteBackupTask = new DeleteDirJob(backup.getStorage(), backup.getPath());
        deleteBackupTask.maxProgress = backup.getByteSize();
        MakeBackup.getInstance().getTaskManager().prepareTask(deleteBackupTask, sender);
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
        if (deleteBackupTask != null) {
            MakeBackup.getInstance().getTaskManager().cancelTaskRaw(deleteBackupTask);
        }
    }

    @Override
    // 最大進捗を取得する
    public long getTaskMaxProgress() {
        if (!isTaskPrepared()) return 0;
        return deleteBackupTask.getTaskMaxProgress() * backup.getStorage().getDeleteProgressMultiplier();
    }

    @Override
    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        if (!isTaskPrepared()) return 0;
        return deleteBackupTask.getTaskCurrentProgress() * backup.getStorage().getDeleteProgressMultiplier();
    }
}
