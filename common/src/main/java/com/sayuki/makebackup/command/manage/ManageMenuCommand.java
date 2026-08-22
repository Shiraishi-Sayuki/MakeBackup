/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command.manage;

import com.sayuki.makebackup.command.CommandArgs;
import com.sayuki.makebackup.platform.ModCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.helper.UiHelper;
import com.sayuki.makebackup.command.SubCommand;
import com.sayuki.makebackup.command.Permissions;

// 管理メニューコマンドクラス - バックアップの操作メニューを表示する
public class ManageMenuCommand extends SubCommand {

    private Target storage;
    private Snapshot backup;

    // コンストラクタ - 初期化する
    public ManageMenuCommand(ModCommandSender sender, CommandArgs arguments) {
        super(sender, arguments);
    }

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
        if (!sender.hasPermission(Permissions.STORAGE.getPermission(storage))) {
            returnFailure("Don't have enough permissions to perform this command");
            return false;
        }

        return true;
    }

    @Override
    // 実行する - メニューを表示する
    public void run() {

        String backupFormattedName = backup.getFormattedName();

        long backupMbSize = backup.getMbSize();

        Component header = Component.empty();
        header = header
                .append(Component.text("Snapshot menu")
                        .decorate(TextDecoration.BOLD))
                .append(Component.space())
                .append(Component.text("(%s)".formatted(storage.getId()))
                        .color(UiHelper.getSecondaryColor())
                        .decorate(TextDecoration.BOLD));

        Component message = Component.empty();
        if (!sender.isConsole()) {
            message = message
                    .append(Component.text(backupFormattedName)
                            .hoverEvent(HoverEvent.showText(Component.text("(%s) (%s) %s MB".formatted(storage.getId(), backup.getFileType().name(), backup.getMbSize())))))
                    .append(Component.newline())
                    .append(Component.newline());

            if (Snapshot.BackupFileType.DIR.equals(backup.getFileType())) {
                message = message
                        .append(Component.text("[TO ZIP]")
                                .clickEvent(ClickEvent.runCommand("/makebackup menu %s tozip %s".formatted(backup.getStorage().getId(), backup.getName())))
                                .decorate(TextDecoration.BOLD)
                                .color(TextColor.color(0x4974B)))
                        .append(Component.space());
            }

            if (Snapshot.BackupFileType.ZIP.equals(backup.getFileType())) {
                message = message
                        .append(Component.text("[UNZIP]")
                                .clickEvent(ClickEvent.runCommand("/makebackup menu %s unzip %s".formatted(backup.getStorage().getId(), backup.getName())))
                                .decorate(TextDecoration.BOLD)
                                .color(TextColor.color(0x4974B)))
                        .append(Component.space());
            }

            if (MakeBackup.getInstance().getStorageManager().getStorages().size() >= 2) {
                message = message
                        .append(Component.text("[COPY TO]")
                                .clickEvent(ClickEvent.suggestCommand("/makebackup menu %s \"%s\" copyto ".formatted(storage.getId(), backup.getName())))
                                .decorate(TextDecoration.BOLD)
                                .color(TextColor.color(17, 102, 212)))
                        .append(Component.space());
            }

            message = message
                    .append(Component.text("[DELETE]")
                            .clickEvent(ClickEvent.runCommand("/makebackup menu %s delete %s".formatted(backup.getStorage().getId(), backup.getName())))
                            .decorate(TextDecoration.BOLD)
                            .color(TextColor.color(0xB02100)));

            sendFramedMessage(header, message, 15);

        } else {
            message = message
                    .append(Component.text(backupFormattedName))
                    .append(Component.space())
                    .append(Component.text("(%s)".formatted(backup.getStorage().getId())))
                    .append(Component.space())
                    .append(Component.text(backup.getFileType().name()))
                    .append(Component.space())
                    .append(Component.text(backupMbSize))
                    .append(Component.space())
                    .append(Component.text("MB"));

            sendFramedMessage(header, message);
        }
    }
}
