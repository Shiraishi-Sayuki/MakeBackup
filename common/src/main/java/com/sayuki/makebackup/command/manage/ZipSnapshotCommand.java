/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command.manage;
import com.sayuki.makebackup.command.CommandArgs;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.command.ConfirmableSubCommand;
import com.sayuki.makebackup.command.Permissions;
import com.sayuki.makebackup.platform.ModCommandSender;

import java.util.List;

// スナップショットZIP化コマンドクラス - ディレクトリをZIPにする
public class ZipSnapshotCommand extends ConfirmableSubCommand {

    private Target storage;
    private Snapshot backup;

    // コンストラクタ - 初期化する
    public ZipSnapshotCommand(ModCommandSender sender, CommandArgs arguments) {
        super(sender, arguments);
    }

    @Override
    // チェックする - ストレージとバックアップを確認する
    public boolean check() {
        storage = MakeBackup.getInstance().getStorageManager().getStorage((String) arguments.get("storage"));
        if (storage == null) {
            returnFailure("Wrong storage name %s".formatted((String) arguments.get("storage")));
            return false;
        }
        if (!storage.checkConnection()) {
            returnFailure("Failed to establish connection to storage %s".formatted(storage.getId()));
            return false;
        }
        backup = storage.getBackupManager().getBackup((String) arguments.get("backupName"));
        if (backup == null) {
            returnFailure("Wrong backup name %s".formatted((String) arguments.get("backupName")));
            return false;
        }
        if (Snapshot.BackupFileType.ZIP.equals(backup.getFileType())) {
            returnFailure("Snapshot is already ZIP!");
            return false;
        }
        if (MakeBackup.getInstance().getTaskManager().isLocked()) {
            returnFailure("Blocked by another operation!");
            return false;
        }
        if (!sender.hasPermission(Permissions.TO_ZIP.getPermission(storage))) {
            returnFailure("Don't have enough permissions to perform this command");
            return false;
        }

        setMessage(backup);
        return true;
    }

    @Override
    // 実行する - ZIP化タスクを開始する
    public void run() {
        MakeBackup.getInstance().getTaskManager().startTaskAsync(backup.getToZipTask(), sender, List.of(Permissions.TO_ZIP.getPermission(storage)));
    }
}
