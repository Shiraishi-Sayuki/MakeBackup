/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;

import java.io.Closeable;

// FTPトレースロガークラス - FTP通信を記録する
class FtpTraceLogger implements Closeable {

    private final TargetTraceLogger logger;

    // コンストラクタ - ストレージIDで初期化する
    FtpTraceLogger(String storageId) {
        this.logger = new TargetTraceLogger(storageId);
    }

    // リスナーを作成する
    ProtocolCommandListener createListener(String clientRole) {
        return new Listener(clientRole);
    }

    // ライフサイクルをログ出力する
    synchronized void logLifecycle(String clientRole, String message) {
        logger.log(clientRole, "INFO", message);
    }

    @Override
    // クローズする
    public synchronized void close() {
        logger.close();
    }

    // リスナークラス - プロトコルコマンドを監視する
    private class Listener implements ProtocolCommandListener {

        private final String clientRole;

        // コンストラクタ - クライアントロールで初期化する
        private Listener(String clientRole) {
            this.clientRole = clientRole;
        }

        @Override
        // コマンド送信を処理する
        public void protocolCommandSent(ProtocolCommandEvent event) {

            String command = event.getCommand();
            if ("PASS".equalsIgnoreCase(command)) {
                logger.log(clientRole, "SEND", "PASS ***");
                return;
            }
            logger.log(clientRole, "SEND", event.getMessage());
        }

        @Override
        // 応答受信を処理する
        public void protocolReplyReceived(ProtocolCommandEvent event) {
            logger.log(clientRole, "REPLY", event.getMessage());
        }
    }
}
