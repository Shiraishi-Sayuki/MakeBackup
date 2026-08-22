/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;

import com.jcraft.jsch.*;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;

import java.util.Properties;

// SFTPコネクションファクトリークラス - SFTP接続を管理する
public class SftpConnectionFactory {

    private final SftpTarget storage;
    private final String clientRole;

    private Session sshSession = null;
    private ChannelSftp sftpChannel = null;

    // コンストラクタ - 初期化する
    SftpConnectionFactory(SftpTarget storage, String clientRole) {
        this.storage = storage;
        this.clientRole = clientRole;
    }

    // クライアントを取得する
    synchronized ChannelSftp getClient() {
        if (sshSession != null && sftpChannel != null) {
            try {

                if (sftpChannel.isConnected() && sftpChannel.pwd() != null) return sftpChannel;
                sftpChannel.connect();
            } catch (Exception ignored) {
                connect();
                try {
                    if (sftpChannel.isConnected() && sftpChannel.pwd() != null) return sftpChannel;
                    throw new TargetConnectionException(storage, "Failed to connect to establish sftp connection");
                } catch (SftpException e) {
                    throw new TargetConnectionException(storage, "Failed to connect to establish sftp connection", e);
                }
            }
        }
        connect();
        try {
            if (sftpChannel.isConnected() && sftpChannel.pwd() != null) return sftpChannel;
            throw new TargetConnectionException(storage, "Failed to connect to establish sftp connection");
        } catch (SftpException e) {
            throw new TargetConnectionException(storage, "Failed to connect to establish sftp connection", e);
        }
    }

    // 接続する
    private void connect() {

        if (!storage.getConfig().getAuthType().equals("password") && !storage.getConfig().getAuthType().equals("key") && !storage.getConfig().getAuthType().equals("key_pass")) {
            throw new TargetConnectionException(storage, "Wrong auth type \"%s\"".formatted(storage.getConfig().getAuthType()));
        }

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channel = null;

        try {
            SftpTraceLogger protocolLogger = storage.getProtocolLogger();
            if (protocolLogger != null) {
                JSch.setLogger(protocolLogger.createJSchLogger(clientRole));
                protocolLogger.logLifecycle(clientRole, "Connecting to %s:%d".formatted(storage.getConfig().getAddress(), storage.getConfig().getPort()));
            }
            if (!storage.getConfig().getSshConfigFilePath().isEmpty()) {
                jsch.setConfigRepository(OpenSSHConfig.parseFile(storage.getConfig().getSshConfigFilePath()));
            } else {
                if (storage.getConfig().getAuthType().equals("key")) {
                    jsch.addIdentity(storage.getConfig().getKeyFilePath());
                }
                if (storage.getConfig().getAuthType().equals("key_pass")) {
                    jsch.addIdentity(storage.getConfig().getKeyFilePath(), storage.getConfig().getPassword());
                }

                session = jsch.getSession(storage.getConfig().getUsername(), storage.getConfig().getAddress(), storage.getConfig().getPort());

                if (storage.getConfig().getAuthType().equals("password")) {
                    session.setPassword(storage.getConfig().getPassword());
                }

                Properties properties = new Properties();
                if (this.storage.getConfig().getUseKnownHostsFile().equals("false")) {
                    properties.put("StrictHostKeyChecking", "no");
                } else {
                    properties.put("StrictHostKeyChecking", "yes");
                }
                session.setConfig(properties);

                if (!this.storage.getConfig().getUseKnownHostsFile().equals("false")) {
                    jsch.setKnownHosts(this.storage.getConfig().getKnownHostsFilePath());
                }

                session.connect(15000);
                session.setServerAliveInterval(60 * 1000);
                channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect(15000);
                if (protocolLogger != null) {
                    protocolLogger.logLifecycle(clientRole, "Connected. Working directory: %s".formatted(channel.pwd()));
                }

                this.sshSession = session;
                this.sftpChannel = channel;
            }
        } catch (Exception e) {
            try {
                if (channel != null) {
                    channel.exit();
                }
            } catch (Exception ignored) {

            }
            try {
                if (session != null) {
                    session.disconnect();
                }
            } catch (Exception ignored) {

            }
            throw new TargetConnectionException(storage, "Failed to establish SFTP connection", e);
        }
    }

    // 切断する
    void disconnect() {
        if (sftpChannel != null) {
            try {
                SftpTraceLogger protocolLogger = storage.getProtocolLogger();
                if (protocolLogger != null) {
                    protocolLogger.logLifecycle(clientRole, "Disconnecting");
                }
                sftpChannel.disconnect();
            } catch (Exception ignored) {

            }
            try {
                sshSession.disconnect();
            } catch (Exception ignored) {

            }
        }
        sftpChannel = null;
        sshSession = null;
    }
}
