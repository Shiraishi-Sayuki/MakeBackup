/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;
import com.sayuki.makebackup.core.target.Target;

import static java.lang.Long.max;

// コピージョブクラス - バックアップを別ストレージにコピーする
public class CopyToJob extends BaseJob {

    private final Snapshot sourceBackup;
    private final Target targetStorage;

    private TransferDirJob copyToTask;

    // コンストラクタ - コピー元とコピー先で初期化する
    public CopyToJob(Snapshot sourceBackup, Target targetStorage) {
        super();
        this.sourceBackup = sourceBackup;
        this.targetStorage = targetStorage;
    }

    @Override
    // 実行する
    public void run() {
        try {
            if (!cancelled) {
                MakeBackup.getInstance().getTaskManager().startTaskRaw(copyToTask, sender);
                targetStorage.renameFile(targetStorage.resolve(targetStorage.getConfig().getBackupsFolder(), sourceBackup.getInProgressFileName()), sourceBackup.getFileName());
                targetStorage.getBackupManager().saveBackupSizeToCache(sourceBackup.getName(), sourceBackup.getByteSize());
            }
        } catch (Exception e) {
            warn(new JobException(copyToTask, e));
        }
    }

    @Override
    // タスクを準備する
    public void prepareTask(ModCommandSender sender) throws Throwable {
        if (cancelled) return;
        copyToTask = new TransferDirJob(
                sourceBackup.getStorage(),
                sourceBackup.getPath(),
                targetStorage,
                targetStorage.getConfig().getBackupsFolder(),
                sourceBackup.getInProgressFileName(),
                true);
        copyToTask.maxProgress = sourceBackup.getByteSize();
        MakeBackup.getInstance().getTaskManager().prepareTask(copyToTask, sender);
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
        if (copyToTask != null) MakeBackup.getInstance().getTaskManager().cancelTaskRaw(copyToTask);
    }

    @Override
    // 最大進捗を取得する
    public long getTaskMaxProgress() {
        if (!isTaskPrepared()) return 0;
        return copyToTask.getTaskMaxProgress() *
                max(sourceBackup.getStorage().getTransferProgressMultiplier(), targetStorage.getTransferProgressMultiplier());
    }

    @Override
    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        if (!isTaskPrepared()) return 0;
        return copyToTask.getTaskCurrentProgress() *
                max(sourceBackup.getStorage().getTransferProgressMultiplier(), targetStorage.getTransferProgressMultiplier());
    }
}
