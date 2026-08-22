/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command.system;

import com.sayuki.makebackup.command.CommandArgs;
import com.sayuki.makebackup.platform.ModCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.helper.UiHelper;
import com.sayuki.makebackup.command.ConfirmableSubCommand;

// ジョブキャンセルコマンドクラス - 実行中のジョブをキャンセルする
public class CancelJobCommand extends ConfirmableSubCommand {

    // コンストラクタ - 初期化する
    public CancelJobCommand(ModCommandSender sender, CommandArgs arguments) {
        super(sender, arguments);
    }

    @Override
    // チェックする - 実行中タスクがあるか確認する
    public boolean check() {
        if (!MakeBackup.getInstance().getTaskManager().isLocked()) {
            returnFailure("No task is currently running");
            return false;
        }

        long progress = MakeBackup.getInstance().getTaskManager().getCurrentTask().getTaskPercentProgress();
        TextColor color;
        if (progress < 40) {
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
                .append(Component.text("%s%%".formatted(progress))
                        .decorate(TextDecoration.BOLD)
                        .color(color));

        setMessage(message);
        return true;
    }

    @Override
    // 実行する - キャンセルする
    public void run() {
        MakeBackup.getInstance().getTaskManager().cancelCurrentTask(sender);
    }
}
