/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command.drive;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.target.UserAuthTarget;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;
import com.sayuki.makebackup.command.SubCommand;
import com.sayuki.makebackup.command.CommandArgs;
import com.sayuki.makebackup.command.Permissions;
import com.sayuki.makebackup.platform.ModCommandSender;

// ドライブリンクコマンドクラス - GoogleDriveアカウントをリンクする
public class DriveLinkCommand extends SubCommand {

    private UserAuthTarget storage;

    // コンストラクタ - 初期化する
    public DriveLinkCommand(ModCommandSender sender, CommandArgs arguments) {
        super(sender, arguments);
    }

    @Override
    // チェックする - ストレージと権限を確認する
    public boolean check() {
        if (MakeBackup.getInstance().getTaskManager().isLocked()) {
            returnFailure("You cannot link your account while some process is running");
            return false;
        }
        Target storage = MakeBackup.getInstance().getStorageManager().getStorage((String) arguments.get("storage"));
        if (storage == null) {
            returnFailure("Wrong storage name %s".formatted((String) arguments.get("storage")));
            return false;
        }
        if (!sender.hasPermission(Permissions.ACCOUNT.getPermission(storage))) {
            returnFailure("Don't have enough permissions to perform this command");
            return false;
        }
        if (!(storage instanceof UserAuthTarget)) {
            returnFailure("There is no option to link account to this storage %s".formatted((String) arguments.get("storage")));
            return false;
        }
        this.storage = (UserAuthTarget) storage;

        return true;
    }

    @Override
    // 実行する - 認証コードでリンクする
    public void run() {
        try {
            if (arguments.containsKey("code") && arguments.get("code") != null && !((String) arguments.get("code")).isEmpty()) {
                String code = (String) arguments.get("code");
                storage.authorizeWithCode(code, sender);
            } else {
                storage.authorizeForced(sender);
            }
        } catch (TargetConnectionException e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to link account to %s storage".formatted(storage.getId()), sender);
            MakeBackup.getInstance().getLogManager().warn(e);
        }
    }
}
