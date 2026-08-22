/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;
import com.sayuki.makebackup.platform.ModCommandSender;

// 確認付きサブコマンド抽象クラス - 確認が必要なコマンドの基底
public abstract class ConfirmableSubCommand extends SubCommand {

    protected Component message = null;

    // コンストラクタ - 初期化する
    protected ConfirmableSubCommand(ModCommandSender sender, CommandArgs arguments) {
        super(sender, arguments);
    }

    // 確認表示を実行する
    public void runConfirm() {
        Component header = Component.empty();
        header = header
                .append(Component.text("Confirm %s".formatted(this.getClass().getSimpleName().replace("SubCommand", "")))
                        .decorate(TextDecoration.BOLD)
                        .color(TextColor.color(0xB02100)));
        Component message = net.kyori.adventure.text.Component.empty();
        if (this.message != null) {
            message = message
                    .append(this.message)
                    .append(Component.newline())
                    .append(Component.newline());
        }
        if (sender.isPlayer()) {
            MakeBackup.getInstance().getCommandManager().registerConfirmation(sender.asPlayer().getUniqueId(), this);
            message = message
                    .append(Component.text("[CONFIRM]")
                            .clickEvent(ClickEvent.runCommand("/makebackup confirm"))
                            .color(TextColor.color(0xB02100))
                            .decorate(TextDecoration.BOLD));
        } else {
            message = message
                    .append(Component.text("[CONFIRM]")
                            .color(TextColor.color(0xB02100))
                            .decorate(TextDecoration.BOLD));
        }

        sendFramedMessage(header, message, 15);
    }

    // 確認付きで実行する
    public void executeConfirm() {
        if (!check()) {
            cancelSound();
            return;
        }
        buttonSound();
        runConfirm();
    }

    // メッセージを設定する - Component版
    protected void setMessage(Component message) {
        this.message = message;
    }

    // メッセージを設定する - Snapshot版
    protected void setMessage(Snapshot backup) {
        this.message = Component.text(backup.getFormattedName())
                .hoverEvent(HoverEvent.showText(Component.text("(%s) (%s) %s MB".formatted(backup.getStorage().getId(), backup.getFileType().name(), backup.getMbSize()))));
    }
}
