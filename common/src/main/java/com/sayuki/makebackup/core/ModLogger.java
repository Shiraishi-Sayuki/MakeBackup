/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core;

import net.kyori.adventure.text.Component;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.job.JobException;
import com.sayuki.makebackup.core.helper.ComponentUtils;
import com.sayuki.makebackup.core.helper.UiHelper;
import com.sayuki.makebackup.platform.ModCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

// ロガークラス - ログ出力をまとめて扱う
public class ModLogger {

    private static final Logger LOG = LoggerFactory.getLogger("MakeBackup");

    // ロガーを取得する
    public static Logger getLogger() {
        return LOG;
    }

    // ログを出力する - 文字列をコンソールに出す
    public void log(String text) {
        LOG.info(text);
    }

    // ログを出力する - 送信者にもメッセージを送る
    public void log(String text, ModCommandSender sender) {

        LOG.info(text);

        if (sender != null && !sender.isConsole()) {
            try {
                UiHelper.sendMessage(text, sender);
            } catch (Exception ignored) {
            }
        }
    }

    // ログを出力する - Component版
    public void log(Component text, ModCommandSender sender) {

        Component consoleText = text;
        if (sender != null && !sender.isConsole()) {
            consoleText = Component.newline().append(text);
        }
        LOG.info(ComponentUtils.toPlainText(consoleText));

        if (sender != null && !sender.isConsole()) {
            try {
                text = Component.newline().append(text);
                UiHelper.sendMessage(text, sender);
            } catch (Exception ignored) {
            }
        }
    }

    // 開発用ログを出力する
    public void devLog(String text) {
        if (MakeBackup.getInstance().getConfigManager().getServerConfig().isBetterLogging()) {
            LOG.info(text);
        }
    }

    // 開発用ログを出力する - 送信者付き
    public void devLog(String text, ModCommandSender sender) {

        if (MakeBackup.getInstance().getConfigManager().getServerConfig().isBetterLogging()) {
            LOG.info(text);

            if (sender != null && !sender.isConsole()) {
                try {
                    UiHelper.sendMessage(text, sender);
                } catch (Exception ignored) {
                }
            }
        }
    }

    // 警告を出力する
    public void warn(String text) {
        LOG.warn(text);
    }

    // 警告を出力する - JobException版
    public void warn(JobException taskException) {
        LOG.warn(taskException.getMessage());
        LOG.warn(taskException.getException().getMessage());
        LOG.warn(Arrays.toString(taskException.getException().getStackTrace()));
    }

    // 警告を出力する - 送信者付き
    public void warn(String text, ModCommandSender sender) {

        LOG.warn(text);

        if (sender != null && !sender.isConsole()) {
            try {
                UiHelper.returnWarning(text, sender);
            } catch (Exception ignored) {
            }
        }
    }

    // 成功ログを出力する
    public void success(String text) {
        LOG.info(text);
    }

    // 成功ログを出力する - 送信者付き
    public void success(String text, ModCommandSender sender) {

        LOG.info(text);

        if (sender != null && !sender.isConsole()) {
            try {
                UiHelper.returnSuccess(text, sender);
            } catch (Exception ignored) {
            }
        }
    }

    // 開発用警告を出力する
    public void devWarn(String text) {

        if (!MakeBackup.getInstance().isEnabled()) {
            return;
        }

        if (MakeBackup.getInstance().getConfigManager().getServerConfig().isBetterLogging()) {
            LOG.warn(text);
        }
    }

    // 警告を出力する - 例外版
    public void warn(Exception exception) {

        if (!MakeBackup.getInstance().isEnabled()) {
            return;
        }

        LOG.warn("%s\n%s".formatted(exception.getMessage(), Arrays.toString(exception.getStackTrace())));
    }

    // 開発用警告を出力する - 例外版
    public void devWarn(Exception exception) {

        if (!MakeBackup.getInstance().isEnabled()) {
            return;
        }

        if (MakeBackup.getInstance().getConfigManager().getServerConfig().isBetterLogging()) {
            LOG.warn("%s\n%s".formatted(exception.getMessage(), Arrays.toString(exception.getStackTrace())));
        }
    }
}
