/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.helper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.platform.ModCommandSender;

// UIヘルパークラス - メッセージやサウンドの表示をまとめる
public class UiHelper {

    // 失敗メッセージを返す - 赤色で表示する
    public static void returnFailure(String message, ModCommandSender sender) {
        Component text = Component.text(message).color(NamedTextColor.RED);
        sendMessage(text, sender);
    }

    // 成功メッセージを返す - 緑色で表示する
    public static void returnSuccess(String message, ModCommandSender sender) {
        Component text = Component.text(message).color(NamedTextColor.GREEN);
        sendMessage(text, sender);
    }

    // 警告メッセージを返す - 黄色で表示する
    public static void returnWarning(String message, ModCommandSender sender) {
        Component text = Component.text(message).color(NamedTextColor.YELLOW);
        sendMessage(text, sender);
    }

    // メッセージを送信する - 文字列版
    public static void sendMessage(String message, ModCommandSender sender) {
        sendMessage(Component.text(message), sender);
    }

    // メッセージを送信する - Component版
    public static void sendMessage(Component message, ModCommandSender sender) {
        try {
            if (sender.isConsole()) {
                MakeBackup.getInstance().getLogManager().log(message, sender);
            } else {
                sender.sendMessage(message);
            }
        } catch (Exception ignored) {
        }
    }

    // キャンセル音を鳴らす
    public static void cancelSound(ModCommandSender sender) {
        try {
            sender.playSound("block.anvil.place", 50, 1);
        } catch (Exception ignored) {
        }
    }

    // ボタン音を鳴らす
    public static void buttonSound(ModCommandSender sender) {
        try {
            sender.playSound("ui.button.click", 50, 1);
        } catch (Exception ignored) {
        }
    }

    // 成功音を鳴らす
    public static void successSound(ModCommandSender sender) {
        try {
            sender.playSound("entity.player.levelup", 50, 1);
        } catch (Exception ignored) {
        }
    }

    // 通知音を鳴らす
    public static void notificationSound(ModCommandSender sender) {
        try {
            sender.playSound("entity.player.levelup", 50, 50);
        } catch (Exception ignored) {
        }
    }

    // 枠付きメッセージを取得する - ヘッダーと本文をデコる
    public static Component getFramedMessage(Component header, Component message, ModCommandSender sender) {
        return getFramedMessage(header, message, 42, sender);
    }

    // 枠付きメッセージを取得する - ダッシュ数を指定する
    public static Component getFramedMessage(Component header, Component message, int dashNumber, ModCommandSender sender) {
        Component framedMessage = Component.empty();

        if (sender.isConsole()) {
            framedMessage = framedMessage
                    .append(Component.newline());
        }

        framedMessage = framedMessage
                .append(Component.text("-".repeat(dashNumber))
                        .decorate(TextDecoration.BOLD)
                        .color(UiHelper.getMainColor()))
                .append(Component.newline());

        framedMessage = framedMessage
                .append(header)
                .append(Component.newline());

        framedMessage = framedMessage
                .append(Component.text("-".repeat(dashNumber))
                        .decorate(TextDecoration.BOLD)
                        .color(UiHelper.getSecondaryColor()))
                .append(Component.newline());

        framedMessage = framedMessage.append(message);

        framedMessage = framedMessage
                .append(Component.newline())
                .append(Component.text("-".repeat(dashNumber))
                        .decorate(TextDecoration.BOLD)
                        .color(UiHelper.getMainColor()));

        return framedMessage;
    }

    // 枠付きメッセージを取得する - メッセージのみ
    public static Component getFramedMessage(Component message, int dashNumber, ModCommandSender sender) {
        Component framedMessage = Component.empty();

        if (sender.isConsole()) {
            framedMessage = framedMessage
                    .append(Component.newline());
        }

        framedMessage = framedMessage
                .append(Component.text("-".repeat(dashNumber))
                        .decorate(TextDecoration.BOLD)
                        .color(UiHelper.getMainColor()))
                .append(Component.newline());

        framedMessage = framedMessage.append(message);

        framedMessage = framedMessage
                .append(Component.newline())
                .append(Component.text("-".repeat(dashNumber))
                        .decorate(TextDecoration.BOLD)
                        .color(UiHelper.getMainColor()));
        return framedMessage;
    }

    // 枠付きメッセージを取得する - デフォルト枠で取得する
    public static Component getFramedMessage(Component message, ModCommandSender sender) {
        return getFramedMessage(message, 42, sender);
    }

    // 枠付きメッセージを送信する - ヘッダー付き
    public static void sendFramedMessage(Component header, Component message, ModCommandSender sender) {
        sendFramedMessage(header, message, 42, sender);
    }

    // 枠付きメッセージを送信する - ダッシュ数指定
    public static void sendFramedMessage(Component header, Component message, int dashNumber, ModCommandSender sender) {
        sendMessage(getFramedMessage(header, message, dashNumber, sender), sender);
    }

    // 枠付きメッセージを送信する - メッセージのみダッシュ数指定
    public static void sendFramedMessage(Component message, int dashNumber, ModCommandSender sender) {
        sendMessage(getFramedMessage(message, dashNumber, sender), sender);
    }

    // 枠付きメッセージを送信する - シンプル版
    public static void sendFramedMessage(Component message, ModCommandSender sender) {
        sendFramedMessage(message, 42, sender);
    }

    // メインカラーを取得する
    public static TextColor getMainColor() {
        return TextColor.color(0x550D77);
    }

    // サブカラーを取得する
    public static TextColor getSecondaryColor() {
        return TextColor.color(0x831012);
    }
}
