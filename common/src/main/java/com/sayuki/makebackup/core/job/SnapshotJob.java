/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import org.jetbrains.annotations.ApiStatus;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.target.error.TargetMethodException;
import com.sayuki.makebackup.core.helper.Helper;
import com.sayuki.makebackup.platform.ModWorld;
import com.sayuki.makebackup.platform.Services;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.lang.Long.max;

// スナップショットジョブクラス - 全バックアップ処理を統括する
public class SnapshotJob extends BaseJob {

    private final boolean isAutoBackup;
    private final String afterBackup;
    private final List<Target> storages;

    private String backupName;

    private final List<Job> tasks = new ArrayList<>();

    // コンストラクタ - ストレージと事後処理で初期化する
    public SnapshotJob(List<Target> storages, String afterBackup, boolean isAutoBackup) {
        super();
        this.storages = storages;
        this.afterBackup = afterBackup.toUpperCase();
        this.isAutoBackup = isAutoBackup;
    }

    @Override
    @ApiStatus.Internal
    // 開始する - 重複チェックから実行まで行う
    public void start(ModCommandSender sender) throws JobException {
        try {
            if (!cancelled) {
                this.sender = sender;
            }

            if (!cancelled && MakeBackup.getInstance().getConfigManager().getBackupConfig().isSkipDuplicateBackup() && isAutoBackup &&
                    MakeBackup.getInstance().getConfigManager().getLastBackup() >= MakeBackup.getInstance().getConfigManager().getLastChange()) {

                log("The backup cycle will be skipped since there were no changes from the previous backup", sender);
                MakeBackup.getInstance().getConfigManager().updateLastBackup();

                if (afterBackup.equals("RESTART")) {
                    MakeBackup.getInstance().getScheduleManager().runGlobalRegionDelayed(() -> {
                        MakeBackup.getInstance().getScheduleManager().destroy();
                        Services.PLATFORM.stopServer(true);
                    }, 20);

                } else if (afterBackup.equals("STOP")) {
                    devLog("Stopping server...");
                    Services.PLATFORM.stopServer(false);
                }
                return;
            }

            if (!isTaskPrepared() && !cancelled) {
                try {
                    MakeBackup.getInstance().getTaskManager().prepareTask(this, sender);
                } catch (Throwable e) {
                    throw new JobException(this, e);
                }
            }
            if (!cancelled) {
                prepareTaskFuture.get();
            }
            if (!cancelled) {
                run();
            }
        } catch (Exception e) {
            throw new JobException(this, e);
        }
    }

    @Override
    // 実行する - 各ストレージにバックアップを作成する
    public void run() {

        HashMap<Target, Long> storageBackupByteSize = new HashMap<>();
        List<CompletableFuture<Void>> taskFutures = new ArrayList<>();

        if (!cancelled) {
            try {
                MakeBackup.getInstance().getTaskManager().startTaskRaw(new SetWorldsReadOnlyJob(), sender);
            } catch (JobException e) {
                warn(e);
            }
        }
        HashMap<Target, List<Job>> storageTasks = new HashMap<>();
        for (Job task : tasks) {
            if (task instanceof DoubleTargetJob doubleStorageTask) {
                storageTasks.compute(doubleStorageTask.getTargetStorage(), (storage, tasks) -> {
                    if (tasks == null) tasks = new ArrayList<>();
                    tasks.add(doubleStorageTask);
                    return tasks;
                });
            } else {
                MakeBackup.getInstance().getLogManager().warn("Non-DoubleTargetJob found in SnapshotJob tasks list: %s".formatted(task.getClass().getName()));
            }
        }

        for (Target storage : storageTasks.keySet()) {
            if (cancelled) break;

            taskFutures.add(MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                try {
                    for (Job task : storageTasks.get(storage)) {
                        if (cancelled) break;

                        MakeBackup.getInstance().getTaskManager().startTaskRaw(task, sender);

                        if (!cancelled && (task instanceof TransferDirJob transferDirTask)) {
                            storageBackupByteSize.compute(transferDirTask.getTargetStorage(), (transferTaskStorage, size) -> size == null ? transferDirTask.getTaskMaxProgress() : size + transferDirTask.getTaskMaxProgress());
                        }
                    }

                    if (!cancelled) {
                        devLog("The Rename \"in progress\" in %s storage task has been started".formatted(storage.getId()));
                        String fileType = "";
                        if (storage.getConfig().isZipArchive()) {
                            fileType = ".zip";
                        }

                        try {

                            String backupsFolder = storage.getConfig().getBackupsFolder();
                            String inProgressFileName = backupName + fileType;

                            String inProgressPath = storage.resolve(backupsFolder, inProgressFileName);
                            String finalFileName = backupName.replace(" in progress", "") + fileType;

                            verifyBackupExists(storage, inProgressPath, "Snapshot upload verification failed. In-progress backup does not exist: %s".formatted(inProgressPath));
                            storage.renameFile(inProgressPath, finalFileName);

                            String finalPath = null;
                            for (int i = 0; i < 6; i++) {
                                finalPath = storage.resolve(backupsFolder, finalFileName);
                                if (finalPath != null) break;
                                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                            }
                            if (finalPath == null) {

                                devLog("Rename not yet visible for %s, keeping in-progress file as valid backup".formatted(finalFileName));
                                finalPath = storage.resolve(backupsFolder, inProgressFileName);
                                if (finalPath != null) {

                                    verifyBackupExists(storage, finalPath, "Snapshot upload verification failed. In-progress backup does not exist: %s".formatted(finalPath));
                                } else {
                                    verifyBackupExists(storage, null, "Snapshot rename verification failed. Final backup does not exist: %s".formatted(finalPath));
                                }
                            } else {
                                verifyBackupExists(storage, finalPath, "Snapshot rename verification failed. Final backup does not exist: %s".formatted(finalPath));

                                for (int i = 0; i < 3; i++) {

                                    String still = storage.resolve(backupsFolder, inProgressFileName);
                                    if (still == null) break;
                                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                                    if (i == 2) verifyBackupNameDoesNotExist(storage, backupsFolder, inProgressFileName, "Snapshot rename verification failed. In-progress backup still exists: %s".formatted(inProgressFileName));
                                }
                            }

                            if (!storage.getConfig().isZipArchive()) {
                                storage.getBackupManager().saveBackupSizeToCache(backupName.replace(" in progress", ""), storageBackupByteSize.get(storage));
                                devLog("New backup size in %s storage has been cached".formatted(storage.getId()));
                            }
                        } catch (Exception e) {
                            warn("Failed to finalize backup %s in %s storage".formatted(backupName, storage.getId()), sender);
                            warn(e);
                        }
                        devLog("The Rename \"in progress\" Folder %s storage task has been finished".formatted(storage.getId()));
                    }
                } catch (JobException e) {
                    warn(e);
                }
            }));
        }

        CompletableFuture.allOf(taskFutures.toArray(new CompletableFuture[0])).join();
        try {
            MakeBackup.getInstance().getTaskManager().startTaskRaw(new SetWorldsWritableJob(), sender);
        } catch (JobException e) {
            warn(e);
        }

        if (!cancelled && isAutoBackup) {
            devLog("Update \"lastBackup\" Variable task has been started");
            MakeBackup.getInstance().getConfigManager().updateLastBackup();
            devLog("Update \"lastBackup\" Variable task has been finished");
        }

        if (!cancelled) {

            BaseJob deleteOldBackupTask = new DeleteOldSnapshotsJob();
            tasks.add(deleteOldBackupTask);
            try {
                MakeBackup.getInstance().getTaskManager().startTaskRaw(deleteOldBackupTask, sender);
            } catch (Exception e) {
                warn(new JobException(deleteOldBackupTask, e));
            }

            if (MakeBackup.getInstance().getConfigManager().getBackupConfig().isDeleteBrokenBackups()) {
                BaseJob deleteBrokenBackupsTask = new DeleteBrokenSnapshotsJob();
                tasks.add(deleteBrokenBackupsTask);
                try {
                    MakeBackup.getInstance().getTaskManager().startTaskRaw(deleteBrokenBackupsTask, sender);
                } catch (Exception e) {
                    warn(new JobException(deleteBrokenBackupsTask, e));
                }
            }
        }

        if (!cancelled) {
            if (afterBackup.equals("RESTART")) {
                MakeBackup.getInstance().getScheduleManager().runGlobalRegionDelayed(() -> {
                    MakeBackup.getInstance().getScheduleManager().destroy();
                    Services.PLATFORM.stopServer(true);
                }, 20);

            } else if (afterBackup.equals("STOP")) {
                log("Stopping server...", sender);
                Services.PLATFORM.stopServer(false);
            }
        }
    }

    @Override
    // タスクを準備する
    public void prepareTask(ModCommandSender sender) {
        try {
            this.backupName = "%s in progress".formatted(LocalDateTime.now().format(MakeBackup.getInstance().getConfigManager().getBackupConfig().getDateTimeFormatter()));
            for (Target storage : storages) {
                if (!cancelled) {
                    prepareStorageTask(storage);
                }
            }
        } catch (Exception e) {
            warn("The Snapshot task has been finished with an exception!", this.sender);
            warn(e);
        }
    }

    // ストレージタスクを準備する
    private void prepareStorageTask(Target storage) {
        try {
            if (cancelled) return;

            if (!storage.getConfig().isZipArchive()) {
                storage.createDir(backupName, storage.getConfig().getBackupsFolder());
            }

            ArrayList<String> dirsToAddToZip = new ArrayList<>();
            for (String directoryToBackup : getDirectoriesToBackup()) {
                try {
                    if (cancelled) return;

                    File additionalDirectoryToBackupFile = Paths.get(directoryToBackup).toFile();

                    boolean isExcludedDirectory = Helper.isExcludedDirectory(additionalDirectoryToBackupFile, sender);
                    if (!additionalDirectoryToBackupFile.exists()) {
                        warn("addDirectoryToBackup \"%s\" does not exist!".formatted(additionalDirectoryToBackupFile.getPath()));
                        continue;
                    }
                    if (isExcludedDirectory) continue;

                    if (!storage.getConfig().isZipArchive()) {
                        Job task = new TransferDirJob(MakeBackup.getInstance().getStorageManager().getStorage("makebackup"),
                                additionalDirectoryToBackupFile.toPath().toAbsolutePath().normalize().toString(),
                                storage,
                                storage.resolve(storage.getConfig().getBackupsFolder(), backupName),
                                additionalDirectoryToBackupFile.getName(),
                                false);
                        try {
                            MakeBackup.getInstance().getTaskManager().prepareTask(task, sender);
                        } catch (Throwable e) {
                            throw new JobException(task, e);
                        }
                        tasks.add(task);
                    } else {
                        dirsToAddToZip.add(additionalDirectoryToBackupFile.toPath().toAbsolutePath().normalize().toString());
                    }
                } catch (Exception e) {
                    warn("Something went wrong when trying to backup an additional directory \"%s\"".formatted(directoryToBackup), sender);
                    warn(e);
                }
            }

            if (storage.getConfig().isZipArchive()) {
                Job task = new TransferDirsAsZipJob(MakeBackup.getInstance().getStorageManager().getStorage("makebackup"), dirsToAddToZip, storage, storage.getConfig().getBackupsFolder(), "%s.zip".formatted(backupName), true, false);
                try {
                    MakeBackup.getInstance().getTaskManager().prepareTask(task, sender);
                } catch (Throwable e) {
                    throw new JobException(task, e);
                }
                tasks.add(task);
            }
        } catch (Exception e) {
            warn("Something went wrong while trying to prepare %s storage backup task".formatted(storage.getId()));
            warn(e);
        }
    }

    // タスク進捗倍率を取得する
    private long getTaskProgressMultiplier(Job task) {
        if (task instanceof TransferDirJob transferDirTask) {
            return max(transferDirTask.getTargetStorage().getTransferProgressMultiplier(), transferDirTask.getSourceStorage().getTransferProgressMultiplier());
        } else if (task instanceof TransferDirsAsZipJob addLocalDirToZipTask) {
            return max(addLocalDirToZipTask.getTargetStorage().getZipProgressMultiplier(), addLocalDirToZipTask.getSourceStorage().getZipProgressMultiplier());
        }
        return 1;
    }

    // バックアップが存在するか検証する
    private void verifyBackupExists(Target storage, String path, String errorMessage) {
        if (path == null || !storage.exists(path)) {
            throw new TargetMethodException(storage, errorMessage);
        }
    }

    // バックアップが存在しないか検証する
    private void verifyBackupNameDoesNotExist(Target storage, String parentPath, String fileName, String errorMessage) {

        String path = storage.resolve(parentPath, fileName);
        if (path != null && storage.exists(path)) {
            throw new TargetMethodException(storage, errorMessage);
        }
    }

    @Override
    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        return tasks.stream().mapToLong(task -> task.getTaskCurrentProgress() * getTaskProgressMultiplier(task)).sum();
    }

    @Override
    // 最大進捗を取得する
    public long getTaskMaxProgress() {
        return tasks.stream().mapToLong(task -> task.getTaskMaxProgress() * getTaskProgressMultiplier(task)).sum();
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
        for (Job task : tasks) {
            MakeBackup.getInstance().getTaskManager().cancelTaskRaw(task);
        }
    }

    // バックアップ対象ディレクトリを取得する
    private List<String> getDirectoriesToBackup(){

        return Stream.concat(getWorldsDirectoryToBackup().stream(), getAddDirectoryToBackup().stream()).distinct().toList();
    }

    // ワールドディレクトリを取得する
    private List<String> getWorldsDirectoryToBackup() {

        try {
            return List.of(Services.PLATFORM.getLevelDirectory().toFile().getPath());
        } catch (Throwable e) {
            return Services.PLATFORM.getWorlds().stream().map(world -> world.getWorldFolder().getPath()).toList();
        }
    }

    // 追加バックアップディレクトリを取得する
    private List<String> getAddDirectoryToBackup() {

        if(MakeBackup.getInstance().getConfigManager().getBackupConfig().getAddDirectoryToBackup().contains("*")){

            List<String> list = MakeBackup.getInstance().getConfigManager().getBackupConfig().getAddDirectoryToBackup().stream().filter(directory -> !directory.equals("*")).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            File file = new File(".");
            for (File subFile: file.listFiles()) {
                list.add(subFile.getPath());
            }
            return list;
        }else{
            return MakeBackup.getInstance().getConfigManager().getBackupConfig().getAddDirectoryToBackup().stream().map(addDirectory -> new File(addDirectory).getPath()).toList();
        }
    }
}
