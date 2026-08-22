/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command;

import net.kyori.adventure.text.Component;
import com.sayuki.makebackup.core.helper.UiHelper;
import com.sayuki.makebackup.platform.ModCommandSender;

// サブコマンド抽象クラス - 各コマンドの基底になる
public abstract class SubCommand {

    protected ModCommandSender sender;
    protected CommandArgs arguments;

    // コンストラクタ - 送信者と引数で初期化する
    protected SubCommand(ModCommandSender sender, CommandArgs arguments) {
        this.sender = sender;
        this.arguments = arguments;
    }

    // チェックする - 実行条件を確認する
    public abstract boolean check();

    // 実行する - 本処理を行う
    public abstract void run();

    // 実行する - チェックしてからrunを呼ぶ
    public void execute() {
        if (!check()) {
            cancelSound();
            return;
        }
        buttonSound();
        run();
    }

    // 成功メッセージを返す
    protected void returnSuccess(String message) {
        UiHelper.returnSuccess(message, sender);
    }

    // 失敗メッセージを返す
    protected void returnFailure(String message) {
        UiHelper.returnFailure(message, sender);
    }

    // 警告メッセージを返す
    protected void returnWarning(String message) {
        UiHelper.returnWarning(message, sender);
    }

    // メッセージを送信する
    protected void sendMessage(String message) {
        UiHelper.sendMessage(message, sender);
    }

    // キャンセル音を鳴らす
    protected void cancelSound() {
        UiHelper.cancelSound(sender);
    }

    // ボタン音を鳴らす
    protected void buttonSound() {
        UiHelper.buttonSound(sender);
    }

    // 成功音を鳴らす
    protected void successSound() {
        UiHelper.successSound(sender);
    }

    // 通知音を鳴らす
    protected void notificationSound() {
        UiHelper.notificationSound(sender);
    }

    // 枠付きメッセージを送信する
    protected void sendFramedMessage(Component message) {
        UiHelper.sendFramedMessage(message, sender);
    }

    // 枠付きメッセージを送信する - ダッシュ数指定
    protected void sendFramedMessage(Component message, int dashNumber) {
        UiHelper.sendFramedMessage(message, dashNumber, sender);
    }

    // 枠付きメッセージを送信する - ヘッダー付き
    protected void sendFramedMessage(Component header, Component message) {
        UiHelper.sendFramedMessage(header, message, sender);
    }

    // 枠付きメッセージを送信する - ヘッダーとダッシュ数指定
    protected void sendFramedMessage(Component header, Component message, int dashNumber) {
        UiHelper.sendFramedMessage(header, message, dashNumber, sender);
    }
}
