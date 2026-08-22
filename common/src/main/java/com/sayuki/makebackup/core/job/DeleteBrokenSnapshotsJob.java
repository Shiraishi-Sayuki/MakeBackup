/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.Target;

import java.util.ArrayList;

// 壊れたスナップショット削除ジョブクラス - in progressの残骸を削除する
public class DeleteBrokenSnapshotsJob extends BaseJob {

    private final ArrayList<Job> tasks = new ArrayList<>();

    @Override
    // 実行する
    public void run() {
        for (Job task : tasks) {
            if (cancelled) return;
            try {
                MakeBackup.getInstance().getTaskManager().startTaskRaw(task, sender);
            } catch (JobException e) {
                warn(e);
            }
        }
    }

    @Override
    // タスクを準備する - 壊れたバックアップを探す
    public void prepareTask(ModCommandSender sender) throws Throwable {
        if (cancelled) return;

        for (Target storage : MakeBackup.getInstance().getStorageManager().getStorages()) {
            if (!storage.checkConnection(sender)) continue;

            for (String file : storage.ls(storage.getConfig().getBackupsFolder())) {
                if (cancelled) return;
                if (file.replace(".zip", "").endsWith(" in progress")) {
                    Job task = new DeleteDirJob(storage, storage.resolve(storage.getConfig().getBackupsFolder(), file));
                    MakeBackup.getInstance().getTaskManager().prepareTask(task, sender);
                    tasks.add(task);
                }
            }
        }
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
        for (Job task : tasks) {
            MakeBackup.getInstance().getTaskManager().cancelTaskRaw(task);
        }
    }

    @Override
    // 最大進捗を取得する
    public long getTaskMaxProgress() {
        return tasks.stream().mapToLong(Job::getTaskMaxProgress).sum();
    }

    @Override
    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        return tasks.stream().mapToLong(Job::getTaskCurrentProgress).sum();

    }
}
