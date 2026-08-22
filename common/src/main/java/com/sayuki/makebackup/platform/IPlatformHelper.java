/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.platform;

import com.sayuki.makebackup.command.ModCommandTree;

import java.nio.file.Path;
import java.util.List;

// プラットフォームヘルパーインターフェース - 環境差異を吸収する
public interface IPlatformHelper {

    // プラットフォーム名を取得する
    String getPlatformName();

    // MODが読み込まれているか判定する
    boolean isModLoaded(String modId);

    // 開発環境かどうか判定する
    boolean isDevelopmentEnvironment();

    // 環境名を取得する - developmentかproductionを返す
    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }

    // コンソール送信者を取得する
    ModCommandSender getConsoleSender();

    // コンフィグディレクトリを取得する
    Path getConfigDir();

    // ワールド一覧を取得する
    List<ModWorld> getWorlds();

    // レベルディレクトリを取得する
    Path getLevelDirectory();

    // オンラインプレイヤーを取得する
    List<ModPlayer> getOnlinePlayers();

    // サーバーを停止する - 再起動フラグを指定する
    void stopServer(boolean restart);

    // コマンドを登録する
    void registerCommands(List<ModCommandTree> trees);

    // コマンドツリーを取得する
    List<ModCommandTree> getCommandTrees();

    // サーバーを設定する
    void setServer(Object server);
}
