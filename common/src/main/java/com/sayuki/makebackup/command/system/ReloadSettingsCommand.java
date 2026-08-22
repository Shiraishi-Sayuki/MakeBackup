/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command.system;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.command.SubCommand;
import com.sayuki.makebackup.command.CommandArgs;
import com.sayuki.makebackup.command.Permissions;
import com.sayuki.makebackup.platform.ModCommandSender;

// 設定リロードコマンドクラス - コンフィグを再読み込みする
public class ReloadSettingsCommand extends SubCommand {

    // コンストラクタ - 初期化する
    public ReloadSettingsCommand(ModCommandSender sender, CommandArgs arguments) {
        super(sender, arguments);
    }

    @Override
    // チェックする - 権限とロック状態を確認する
    public boolean check() {
        if (!sender.hasPermission(Permissions.CONFIG_RELOAD.getPermission())) {
            returnFailure("Don't have enough permissions to perform this command");
            return false;
        }
        if (MakeBackup.getInstance().getTaskManager().isLocked() || MakeBackup.restarting) {
            returnFailure("Blocked by another operation!");
            return false;
        }

        return true;
    }

    @Override
    // 実行する - シャットダウンして再初期化する
    public void run() {
        MakeBackup.restarting = true;
        MakeBackup.getInstance().shutdown();
        MakeBackup.getInstance().init();
        returnSuccess("Reloading completed");
        MakeBackup.restarting = false;
    }
}
