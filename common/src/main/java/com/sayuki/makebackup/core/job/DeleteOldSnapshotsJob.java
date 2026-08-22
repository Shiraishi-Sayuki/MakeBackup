/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;
import com.sayuki.makebackup.core.target.Target;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

// 古いスナップショット削除ジョブクラス - 保持数や容量を超えた古いバックアップを削除する
public class DeleteOldSnapshotsJob extends BaseJob {

    private final ArrayList<Job> tasks = new ArrayList<>();

    // コンストラクタ - 初期化する
    public DeleteOldSnapshotsJob() {
        super();
    }

    @Override
    // 実行する
    public void run() {
        for (Job deleteDirTask : tasks) {
            if (!cancelled) {
                try {
                    MakeBackup.getInstance().getTaskManager().startTaskRaw(deleteDirTask, sender);
                } catch (Exception e) {
                    warn(new JobException(deleteDirTask, e));
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
    // タスクを準備する - 削除対象を選定する
    public void prepareTask(ModCommandSender sender) {
        for (Target storage : MakeBackup.getInstance().getStorageManager().getStorages()) {
            if (cancelled || storage.getConfig().getBackupsNumber() == 0 && storage.getConfig().getBackupsWeight() == 0) return;

            HashSet<LocalDateTime> backupsToDeleteList = new HashSet<>();
            long backupsFolderByteSize = 0;

            ArrayList<Snapshot> backups = new ArrayList<>(storage.getBackupManager().getBackupList());
            if (cancelled) return;

            for (Snapshot backup : backups) {
                if (cancelled) return;
                try {
                    backupsFolderByteSize += backup.getByteSize();
                } catch (Exception e) {
                    warn("Failed to get \"%s\" backup byte size in %s storage".formatted(backup.getName(), storage.getId()), sender);
                    warn(e);
                }
            }

            List<LocalDateTime> backupDateTimes = backups.stream().map(Snapshot::getLocalDateTime).sorted().toList();
            if (storage.getConfig().getBackupsNumber() != 0) {

                int backupsToDelete = backups.size() - storage.getConfig().getBackupsNumber();
                for (LocalDateTime fileName : backupDateTimes) {
                    if (backupsToDelete <= 0) break;
                    if (backupsToDeleteList.contains(fileName)) continue;

                    for (Snapshot backup : backups) {
                        if (cancelled) return;

                        String backupFileName = backup.getName().replace(".zip", "");
                        try {
                            if (LocalDateTime.parse(backupFileName, MakeBackup.getInstance().getConfigManager().getBackupConfig().getDateTimeFormatter()).equals(fileName)) {

                                Job deleteBackupTask = backup.getDeleteTask();
                                MakeBackup.getInstance().getTaskManager().prepareTask(deleteBackupTask, sender);
                                tasks.add(deleteBackupTask);
                                backupsToDeleteList.add(fileName);
                                backupsFolderByteSize -= backup.getByteSize();
                            }
                        } catch (Throwable e) {
                            MakeBackup.getInstance().getLogManager().warn(new RuntimeException(e));
                        }
                    }
                    backupsToDelete--;
                }
            }

            if (storage.getConfig().getBackupsWeight() != 0) {

                long bytesToDelete = backupsFolderByteSize - storage.getConfig().getBackupsWeight();
                for (LocalDateTime fileName : backupDateTimes) {
                    if (bytesToDelete <= 0) break;
                    if (backupsToDeleteList.contains(fileName)) continue;

                    for (Snapshot backup : backups) {
                        if (cancelled) return;

                        String backupFileName = backup.getName().replace(".zip", "");
                        try {
                            if (LocalDateTime.parse(backupFileName, MakeBackup.getInstance().getConfigManager().getBackupConfig().getDateTimeFormatter()).equals(fileName)) {

                                bytesToDelete -= backup.getByteSize();
                                BaseJob deleteBackupTask = backup.getDeleteTask();
                                MakeBackup.getInstance().getTaskManager().prepareTask(deleteBackupTask, sender);
                                tasks.add(deleteBackupTask);
                                backupsToDeleteList.add(fileName);
                            }
                        } catch (Throwable e) {
                            MakeBackup.getInstance().getLogManager().warn(new RuntimeException(e));
                        }
                    }
                }
            }
        }
    }

    @Override
    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        return tasks.stream().mapToLong(Job::getTaskCurrentProgress).sum();
    }

    @Override
    // 最大進捗を取得する
    public long getTaskMaxProgress() {
        return tasks.stream().mapToLong(Job::getTaskMaxProgress).sum();
    }
}
