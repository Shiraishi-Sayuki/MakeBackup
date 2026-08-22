/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

package com.sayuki.makebackup.command.runner;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.command.CommandArgs;
import com.sayuki.makebackup.command.Permissions;
import com.sayuki.makebackup.command.SubCommand;
import com.sayuki.makebackup.core.job.Job;
import com.sayuki.makebackup.core.job.SnapshotJob;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.platform.ModCommandSender;

import java.util.ArrayList;
import java.util.List;

// スナップショットコマンドクラス - バックアップを実行する
public class SnapshotCommand extends SubCommand {

    private final String afterBackup;
    private List<Target> storages;
    private long delaySeconds = 0;

    // コンストラクタ - 初期化する
    public SnapshotCommand(ModCommandSender sender, CommandArgs arguments, String afterBackup) {
        super(sender, arguments);
        this.afterBackup = afterBackup == null ? "NOTHING" : afterBackup.toUpperCase();
    }

    @Override
    // チェックする - ストレージと権限と遅延を確認する
    public boolean check() {
        Object storageObj = arguments.get("storage");
        if (storageObj == null) {
            returnFailure("Wrong storage name null");
            return false;
        }
        String storageString = String.valueOf(storageObj);
        if (storageString.isEmpty()) {
            returnFailure("Wrong storage name " + storageString);
            return false;
        }
        String[] ids = storageString.split("-");
        List<Target> list = new ArrayList<>();
        for (String id : ids) {
            if (id == null || id.isEmpty()) continue;
            Target storage = MakeBackup.getInstance().getStorageManager().getStorage(id);
            if (storage == null) {
                returnFailure("Wrong storage name %s".formatted(id));
                return false;
            }
            if (!storage.checkConnection()) {
                returnFailure("Failed to establish connection to storage %s".formatted(storage.getId()));
                return false;
            }
            if (!sender.hasPermission(Permissions.BACKUP.getPermission(storage))) {
                returnFailure("Don't have enough permissions to perform this command");
                return false;
            }
            list.add(storage);
        }
        if (list.isEmpty()) {
            returnFailure("No valid storage specified");
            return false;
        }
        list = list.stream().distinct().toList();
        this.storages = list;

        if (MakeBackup.getInstance().getTaskManager().isLocked()) {
            returnFailure("Blocked by another operation!");
            return false;
        }

        if ("STOP".equals(afterBackup) && !sender.hasPermission(Permissions.STOP.getPermission())) {
            returnFailure("Don't have enough permissions to perform this command");
            return false;
        }
        if ("RESTART".equals(afterBackup) && !sender.hasPermission(Permissions.RESTART.getPermission())) {
            returnFailure("Don't have enough permissions to perform this command");
            return false;
        }

        Object delayObj = arguments.get("delaySeconds");
        if (delayObj != null) {
            try {
                if (delayObj instanceof Number n) {
                    delaySeconds = n.longValue();
                } else {
                    delaySeconds = Long.parseLong(String.valueOf(delayObj));
                }
                if (delaySeconds < 0) {
                    returnFailure("Invalid delay!");
                    return false;
                }
            } catch (NumberFormatException e) {
                returnFailure("Invalid delay!");
                return false;
            }
        }

        return true;
    }

    @Override
    // 実行する - バックアップタスクを開始する
    public void run() {
        List<String> permissions = new ArrayList<>();
        for (Target storage : storages) {
            permissions.add(Permissions.BACKUP.getPermission(storage));
        }
        if ("STOP".equals(afterBackup)) {
            permissions.add(Permissions.STOP.getPermission());
        }
        if ("RESTART".equals(afterBackup)) {
            permissions.add(Permissions.RESTART.getPermission());
        }

        Job backupTask = new SnapshotJob(storages, afterBackup, false);

        Runnable start = () -> MakeBackup.getInstance().getTaskManager().startTask(backupTask, sender, permissions);

        if (delaySeconds > 0) {
            MakeBackup.getInstance().getScheduleManager().runGlobalRegionDelayed(start, delaySeconds * 20);
            sendMessage("Backup scheduled in %s seconds".formatted(delaySeconds));
        } else {
            start.run();
        }
    }
}
