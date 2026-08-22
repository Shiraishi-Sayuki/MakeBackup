/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command.manage;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.job.CopyToJob;
import com.sayuki.makebackup.core.job.Job;
import com.sayuki.makebackup.command.ConfirmableSubCommand;
import com.sayuki.makebackup.command.CommandArgs;
import com.sayuki.makebackup.command.Permissions;
import com.sayuki.makebackup.platform.ModCommandSender;

import java.util.List;

// スナップショットコピーコマンドクラス - バックアップを別ストレージにコピーする
public class CopySnapshotCommand extends ConfirmableSubCommand {

    private Target sourceStorage;
    private Snapshot backup;
    private Target targetStorage;

    // コンストラクタ - 初期化する
    public CopySnapshotCommand(ModCommandSender sender, CommandArgs arguments) {
        super(sender, arguments);
    }

    @Override
    // チェックする - ストレージとバックアップを確認する
    public boolean check() {

        sourceStorage = MakeBackup.getInstance().getStorageManager().getStorage((String) arguments.get("storage"));
        if (sourceStorage == null) {
            returnFailure("Wrong storage name %s".formatted((String) arguments.get("storage")));
            return false;
        }
        if (!sourceStorage.checkConnection()) {
            returnFailure("Failed to establish connection to storage %s".formatted(sourceStorage.getId()));
            return false;
        }
        backup = sourceStorage.getBackupManager().getBackup((String) arguments.get("backupName"));
        if (backup == null) {
            returnFailure("Wrong backup name %s".formatted((String) arguments.get("backupName")));
            return false;
        }
        targetStorage = MakeBackup.getInstance().getStorageManager().getStorage((String) arguments.get("targetStorage"));
        if (targetStorage == null) {
            returnFailure("Wrong target storage");
            return false;
        }
        if (!sourceStorage.checkConnection()) {
            returnFailure("Failed to establish connection to storage %s".formatted(targetStorage.getId()));
            return false;
        }
        if (MakeBackup.getInstance().getTaskManager().isLocked()) {
            returnFailure("Blocked by another operation!");
            return false;
        }
        if (targetStorage.getBackupManager().getBackupList().stream().anyMatch(backup -> backup.getName().equals((String) arguments.get("backupName")))) {
            returnFailure("Target storage already contains this backup");
            return false;
        }
        if (!sender.hasPermission(Permissions.STORAGE.getPermission(sourceStorage)) || !sender.hasPermission(Permissions.STORAGE.getPermission(targetStorage))) {
            returnFailure("Don't have enough permissions to perform this command");
            return false;
        }

        setMessage(backup);
        return true;
    }

    @Override
    // 実行する - コピータスクを開始する
    public void run() {
        Job task = new CopyToJob(backup, targetStorage);
        MakeBackup.getInstance().getTaskManager().startTask(task, sender, List.of(Permissions.STORAGE.getPermission(sourceStorage), Permissions.STORAGE.getPermission(targetStorage)));
    }
}
