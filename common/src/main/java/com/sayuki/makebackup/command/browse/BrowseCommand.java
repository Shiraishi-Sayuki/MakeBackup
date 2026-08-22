/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command.browse;

import com.sayuki.makebackup.command.CommandArgs;
import com.sayuki.makebackup.platform.ModCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.helper.UiHelper;
import com.sayuki.makebackup.command.SubCommand;
import com.sayuki.makebackup.command.Permissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

// ブラウズコマンドクラス - バックアップ一覧を表示する
public class BrowseCommand extends SubCommand {

    private List<List<TextComponent>> pages;
    private Target storage;
    private final boolean sendResult;

    private final HashMap<String, Long> backupNameMbSize = new HashMap<>();
    private final HashMap<String, Snapshot.BackupFileType> backupNameFileType = new HashMap<>();

    // コンストラクタ - 結果送信フラグで初期化する
    public BrowseCommand(boolean sendResult, ModCommandSender sender, CommandArgs arguments) {
        super(sender, arguments);
        this.sendResult = sendResult;
    }

    @Override
    // チェックする - パラメータと権限を確認する
    public boolean check() {
        storage = MakeBackup.getInstance().getStorageManager().getStorage((String) arguments.get("storage"));
        if (storage == null) {
            returnFailure("Wrong storage name %s".formatted((String) arguments.get("storage")));
            return false;
        }
        sendMessage("Creating a list of backups may take some time...");
        if (!storage.checkConnection()) {
            returnFailure("Failed to establish connection to %s storage".formatted(storage.getId()));
            return false;
        }
        if (!sender.hasPermission(Permissions.STORAGE.getPermission(storage))) {
            returnFailure("Don't have enough permissions to perform this command");
            return false;
        }

        int listPageCount = getListPageCount();

        int pageNumber = (Integer) arguments.getOrDefault("pageNumber", 1);
        if (pageNumber < 1) {
            returnFailure("Invalid page number!");
            return false;
        }

        return true;
    }

    @Override
    // 実行する - 一覧を作成して送信する
    public void run() {
        Component header = Component.empty();

        header = header
                .append(Component.text("Snapshot list")
                        .decorate(TextDecoration.BOLD))
                .append(Component.space())
                .append(Component.text("(%s)".formatted(storage.getId()))
                        .color(UiHelper.getSecondaryColor())
                        .decorate(TextDecoration.BOLD));

        int pageNumber = (Integer) arguments.getOrDefault("pageNumber", 1);
        if (!(sender.isConsole())) {
            sendFramedMessage(header, createListMessage(pageNumber, true), 15);
        } else {
            sendFramedMessage(header, createListMessage(pageNumber, arguments.get("pageNumber") != null), 41);
        }
        buttonSound();
    }

    // リストページを更新する
    private void updateListPages() {
        List<Snapshot> backups = new ArrayList<>(storage.getBackupManager().getBackupList());
        backups.sort(Snapshot::compareTo);
        Collections.reverse(backups);

        List<List<TextComponent>> pages = new ArrayList<>();
        for (int i = 1; i <= backups.size(); i++) {
            if (i % 10 == 1) {
                pages.add(new ArrayList<>());
            }
            Snapshot backup = backups.get(i - 1);

            String backupName = backup.getName();

            String backupFormattedName = backup.getFormattedName();

            long backupMbSize = backup.getMbSize();

            backupNameMbSize.put(backupFormattedName, backupMbSize);
            backupNameFileType.put(backupFormattedName, backup.getFileType());

            HoverEvent<net.kyori.adventure.text.Component> hoverEvent = HoverEvent
                    .showText(net.kyori.adventure.text.Component.text("(%s) %s %s MB".formatted(backup.getStorage().getId(), backup.getFileType().name(), backupMbSize)));
            ClickEvent clickEvent = ClickEvent.runCommand("/makebackup menu %s %s".formatted(storage.getId(), backupName));

            pages.get((i - 1) / 10)
                    .add(net.kyori.adventure.text.Component.text(backupFormattedName)
                            .hoverEvent(hoverEvent)
                            .clickEvent(clickEvent));
        }
        this.pages = pages;
    }

    // リストメッセージを作成する
    private Component createListMessage(int pageNumber, boolean pagedListMessage) {
        Component message = Component.empty();

        if (!(sender.isConsole())) {
            message = message
                    .append(Component.text("<<<<<<<<")
                            .decorate(TextDecoration.BOLD)
                            .color(UiHelper.getSecondaryColor())
                            .clickEvent(ClickEvent.runCommand("/makebackup list %s %s".formatted(storage.getId(), pageNumber - 1))))
                    .append(Component.text(String.valueOf(pageNumber))
                            .decorate(TextDecoration.BOLD))
                    .append(Component.text(">>>>>>>>")
                            .decorate(TextDecoration.BOLD)
                            .color(UiHelper.getSecondaryColor())
                            .clickEvent(ClickEvent.runCommand("/makebackup list %s %s".formatted(storage.getId(), pageNumber + 1))))
                    .append(Component.newline());

            if (pages.size() >= pageNumber) {
                for (TextComponent backupComponent : pages.get(pageNumber - 1)) {
                    message = message
                            .append(Component.space())
                            .append(backupComponent)
                            .append(Component.newline());
                }
            }

            message = message
                    .append(Component.text("<<<<<<<<")
                            .decorate(TextDecoration.BOLD)
                            .color(UiHelper.getSecondaryColor())
                            .clickEvent(ClickEvent.runCommand("/makebackup list %s %s".formatted(storage.getId(), pageNumber - 1))))
                    .append(Component.text(String.valueOf(pageNumber))
                            .decorate(TextDecoration.BOLD))
                    .append(Component.text(">>>>>>>>")
                            .decorate(TextDecoration.BOLD)
                            .color(UiHelper.getSecondaryColor())
                            .clickEvent(ClickEvent.runCommand("/makebackup list %s %s".formatted(storage.getId(), pageNumber + 1))));

        } else {
            int backupIndex = 1;
            if (pagedListMessage) {
                message = message
                        .append(Component.text("<".repeat(20))
                                .decorate(TextDecoration.BOLD)
                                .color(UiHelper.getSecondaryColor()))
                        .append(Component.text(pageNumber))
                        .append(Component.text(">".repeat(20))
                                .decorate(TextDecoration.BOLD)
                                .color(UiHelper.getSecondaryColor()))
                        .append(Component.newline());

                if (pages.size() >= pageNumber) {
                    for (TextComponent backupComponent : pages.get(pageNumber - 1)) {
                        if (backupIndex > 1) message = message.append(Component.newline());

                        String backupName = backupComponent.content();
                        message = message
                                .append(Component.text(backupName))
                                .append(Component.space())
                                .append(Component.text("(%s)".formatted(storage.getId())))
                                .append(Component.space())
                                .append(Component.text(backupNameFileType.get(backupName).name()))
                                .append(Component.space())
                                .append(Component.text(backupNameMbSize.get(backupName)))
                                .append(Component.space())
                                .append(Component.text("MB"));
                        backupIndex++;
                    }
                }

                message = message
                        .append(Component.newline())
                        .append(Component.text("<".repeat(20))
                                .decorate(TextDecoration.BOLD)
                                .color(UiHelper.getSecondaryColor()))
                        .append(Component.text(pageNumber))
                        .append(Component.text(">".repeat(20))
                                .decorate(TextDecoration.BOLD)
                                .color(UiHelper.getSecondaryColor()));
            } else {
                for (List<TextComponent> page : pages) {
                    for (TextComponent backupComponent : page) {

                        if (backupIndex > 1) {
                            message = message
                                    .append(Component.newline());
                        }

                        String backupName = backupComponent.content();

                        message = message
                                .append(Component.text(backupComponent.content()))
                                .append(Component.space())
                                .append(Component.text("(%s)".formatted(storage.getId())))
                                .append(Component.space())
                                .append(Component.text(backupNameFileType.get(backupName).name()))
                                .append(Component.space())
                                .append(Component.text(backupNameMbSize.get(backupName)))
                                .append(Component.space())
                                .append(Component.text("MB"));

                        backupIndex++;
                    }
                }
            }
        }
        return message;
    }

    // ページ数を取得する
    private int getListPageCount() {
        updateListPages();
        return pages.size();
    }

    @Override
    // 失敗メッセージを返す - フラグが有効なら送信する
    protected void returnFailure(String message) {
        if (sendResult) super.returnFailure(message);
    }

    @Override
    // 成功メッセージを返す
    protected void returnSuccess(String message) {
        if (sendResult) super.returnSuccess(message);
    }

    @Override
    // 警告メッセージを返す
    protected void returnWarning(String message) {
        if (sendResult) super.returnWarning(message);
    }

    @Override
    // メッセージを送信する
    protected void sendMessage(String message) {
        if (sendResult) super.sendMessage(message);
    }

    @Override
    // 枠付きメッセージを送信する
    protected void sendFramedMessage(Component message) {
        if (sendResult) super.sendFramedMessage(message);
    }

    @Override
    // 枠付きメッセージを送信する - ダッシュ数指定
    protected void sendFramedMessage(Component message, int dashNumber) {
        if (sendResult) super.sendFramedMessage(message, dashNumber);
    }

    @Override
    // 枠付きメッセージを送信する - ヘッダー付き
    protected void sendFramedMessage(Component header, Component message) {
        if (sendResult) super.sendFramedMessage(header, message);
    }

    @Override
    // 枠付きメッセージを送信する - ヘッダーとダッシュ数指定
    protected void sendFramedMessage(Component header, Component message, int dashNumber) {
        if (sendResult) super.sendFramedMessage(header, message, dashNumber);
    }
}
