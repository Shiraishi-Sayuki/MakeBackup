/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;

import java.io.IOException;
import java.time.Duration;

// FTPコネクションファクトリークラス - FTP接続を管理する
public class FtpConnectionFactory {

    private final FtpTarget storage;
    private final String clientRole;

    private FTPClient ftpClient = null;
    private String defaultPath = ".";

    // コンストラクタ - 初期化する
    FtpConnectionFactory(FtpTarget storage, String clientRole) {
        this.storage = storage;
        this.clientRole = clientRole;
    }

    // クライアントを取得する
    synchronized FTPClient getClient() throws TargetConnectionException {
        if (ftpClient != null) {
            try {
                if (!ftpClient.isConnected() || !ftpClient.isAvailable()) {
                    connect();
                }
                try {
                    if (!ftpClient.sendNoOp()) {
                        connect();
                        ftpClient.sendNoOp();
                    }
                } catch (Exception e) {
                    connect();
                    ftpClient.sendNoOp();
                }
            } catch (IOException e) {
                MakeBackup.getInstance().getLogManager().warn("Failed to reconnect to FTP(S) connection");
                MakeBackup.getInstance().getLogManager().warn(e);
            }
            resetWorkingDirectory();
            return ftpClient;
        }

        ftpClient = new FTPClient();
        addProtocolLogger();
        connect();
        try {
            defaultPath = ftpClient.printWorkingDirectory();
        } catch (IOException e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to get default %s working directory".formatted(storage.getId()));
        }
        return ftpClient;
    }

    // 作業ディレクトリをリセットする
    synchronized void resetWorkingDirectory() {
        try {
            if (!ftpClient.changeWorkingDirectory(defaultPath)) {
                MakeBackup.getInstance().getLogManager().devWarn("Failed to reset FTP working directory to \"%s\". FTP reply: %s".formatted(defaultPath, ftpClient.getReplyString()));
            }
        } catch (Exception ignored) {
        }
    }

    // 接続する
    private void connect() {
        if (ftpClient == null) {
            ftpClient = new FTPClient();
            addProtocolLogger();
        }
        logLifecycle("Connecting to %s:%d".formatted(storage.getConfig().getAddress(), storage.getConfig().getPort()));
        ftpClient.setConnectTimeout(30 * 1000);
        ftpClient.setDefaultTimeout(30 * 1000);
        ftpClient.setDataTimeout(Duration.ofSeconds(30));
        ftpClient.setControlKeepAliveTimeout(Duration.ofMinutes(5));
        ftpClient.setControlEncoding("UTF-8");

        try {
            ftpClient.connect(storage.getConfig().getAddress(), storage.getConfig().getPort());
        } catch (IOException e) {
            throw new TargetConnectionException(storage, "Failed to establish FTP(S) connection", e);
        }

        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            try {
                ftpClient.disconnect();
            } catch (IOException ignored) {

            }
            throw new TargetConnectionException(storage, "Failed to establish FTP(S) connection");
        }

        ftpClient.enterLocalPassiveMode();
        try {
            ftpClient.login(storage.getConfig().getUsername(), storage.getConfig().getPassword());
        } catch (IOException e) {
            throw new TargetConnectionException(storage, "Failed to login FTP(S) connection", e);
        }
        reply = ftpClient.getReplyCode();

        if (!FTPReply.isPositiveCompletion(reply)) {
            try {
                ftpClient.disconnect();
            } catch (IOException ignored) {

            }
            throw new TargetConnectionException(storage, "Failed to establish FTP(S) connection");
        }
        try {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            ftpClient.setListHiddenFiles(true);
        } catch (IOException e) {
            throw new TargetConnectionException(storage, "Failed to set FTP(S) connection parameters", e);
        }

        try {
            ftpClient.sendNoOp();
        } catch (IOException e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to send FTP(S) server ping");
        }
        logLifecycle("Connected. Default path: %s".formatted(getCurrentWorkingDirectory()));
    }

    // 切断する
    void disconnect() {
        if (ftpClient != null) {
            try {
                logLifecycle("Disconnecting");
                ftpClient.disconnect();
            } catch (Exception ignored) {

            }
        }
        ftpClient = null;
    }

    // プロトコルロガーを追加する
    private void addProtocolLogger() {
        FtpTraceLogger protocolLogger = storage.getProtocolLogger();
        if (protocolLogger != null) {
            ftpClient.addProtocolCommandListener(protocolLogger.createListener(clientRole));
        }
    }

    // ライフサイクルをログ出力する
    private void logLifecycle(String message) {
        FtpTraceLogger protocolLogger = storage.getProtocolLogger();
        if (protocolLogger != null) {
            protocolLogger.logLifecycle(clientRole, message);
        }
    }

    // 現在の作業ディレクトリを取得する
    private String getCurrentWorkingDirectory() {
        try {
            return ftpClient.printWorkingDirectory();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
