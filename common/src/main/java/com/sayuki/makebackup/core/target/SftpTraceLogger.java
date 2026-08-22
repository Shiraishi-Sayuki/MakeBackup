/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;

import com.jcraft.jsch.Logger;

import java.io.Closeable;

// SFTPトレースロガークラス - SFTP通信を記録する
class SftpTraceLogger implements Closeable {

    private final TargetTraceLogger logger;

    // コンストラクタ - ストレージIDで初期化する
    SftpTraceLogger(String storageId) {
        this.logger = new TargetTraceLogger(storageId);
    }

    // ライフサイクルをログ出力する
    void logLifecycle(String clientRole, String message) {
        logger.log(clientRole, "INFO", message);
    }

    // 操作をログ出力する
    void logOperation(String operation, String message) {
        logger.logOperation(operation, message);
    }

    // JSchロガーを作成する
    Logger createJSchLogger(String clientRole) {
        return new Logger() {
            @Override
            // 有効か確認する
            public boolean isEnabled(int level) {
                return true;
            }

            @Override
            // ログを出力する
            public void log(int level, String message) {
                logger.log(clientRole, "JSCH-%s".formatted(levelName(level)), message);
            }
        };
    }

    // レベル名を取得する
    private String levelName(int level) {
        return switch (level) {
            case Logger.DEBUG -> "DEBUG";
            case Logger.INFO -> "INFO";
            case Logger.WARN -> "WARN";
            case Logger.ERROR -> "ERROR";
            case Logger.FATAL -> "FATAL";

            default -> String.valueOf(level);
        };
    }

    @Override
    // クローズする
    public void close() {
        logger.close();
    }
}
