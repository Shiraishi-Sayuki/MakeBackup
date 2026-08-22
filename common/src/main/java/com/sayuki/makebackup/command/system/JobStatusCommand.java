/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command.system;

import com.sayuki.makebackup.command.CommandArgs;
import com.sayuki.makebackup.platform.ModCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.helper.UiHelper;
import com.sayuki.makebackup.command.SubCommand;
import com.sayuki.makebackup.command.Permissions;

// ジョブステータスコマンドクラス - 現在のジョブ状況を表示する
public class JobStatusCommand extends SubCommand {

    // コンストラクタ - 初期化する
    public JobStatusCommand(ModCommandSender sender, CommandArgs arguments) {
        super(sender, arguments);
    }

    @Override
    // チェックする - 実行中タスクと権限を確認する
    public boolean check() {
        if (!MakeBackup.getInstance().getTaskManager().isLocked()) {
            returnFailure("No task is currently running");
            return false;
        }
        if (!sender.hasPermission(Permissions.STATUS.getPermission())) {
            returnFailure("Don't have enough permissions to perform this command");
            return false;
        }

        return true;
    }

    @Override
    // 実行する - 進捗を表示する
    public void run() {

        long progress = MakeBackup.getInstance().getTaskManager().getCurrentTask().getTaskPercentProgress();
        TextColor color;
        if (!MakeBackup.getInstance().getTaskManager().getCurrentTask().isTaskPrepared()) {
            color = TextColor.color(190, 20, 255);
        } else if (progress < 40) {
            color = TextColor.color(190, 0, 27);
        } else if (progress < 75) {
            color = TextColor.color(190, 151, 0);
        } else {
            color = TextColor.color(0, 156, 61);
        }
        Component message = Component.empty();
        message = message
                .append(Component.text("Current task:"))
                .append(Component.space())
                .append(Component.text(MakeBackup.getInstance().getTaskManager().getCurrentTask().getTaskName())
                        .decorate(TextDecoration.BOLD)
                        .color(UiHelper.getSecondaryColor()))
                .append(Component.newline())
                .append(Component.text("Job progress:"))
                .append(Component.space())
                .append(Component.text((!MakeBackup.getInstance().getTaskManager().getCurrentTask().isTaskPrepared() ? "Preparing..." : "%s%%".formatted(progress)) +
                                (MakeBackup.getInstance().getTaskManager().getCurrentTask().isCancelled() ? " (Cancelling...)" : ""))
                        .decorate(TextDecoration.BOLD)
                        .color(color));

        if (!sender.isConsole()) {
            message = message
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("[STATUS]")
                            .clickEvent(ClickEvent.runCommand("/makebackup task status"))
                            .color(TextColor.color(17, 102, 212))
                            .decorate(TextDecoration.BOLD))
                    .append(Component.space())
                    .append(Component.text("[CANCEL]")
                            .decorate(TextDecoration.BOLD)
                            .color(TextColor.color(0xB02100))
                            .clickEvent(ClickEvent.runCommand("/makebackup task cancel")));
        }

        if (!sender.isConsole()) {
            sendFramedMessage(message, 15);
        } else {
            sendFramedMessage(message);
        }
    }
}
