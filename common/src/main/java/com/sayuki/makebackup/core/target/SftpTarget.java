/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.jcraft.jsch.*;
import lombok.Setter;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.SnapshotManager;
import com.sayuki.makebackup.core.settings.SftpSettings;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;
import com.sayuki.makebackup.core.target.error.TargetLimitException;
import com.sayuki.makebackup.core.target.error.TargetMethodException;
import com.sayuki.makebackup.core.target.support.Retryable;
import com.sayuki.makebackup.core.target.support.TransferProgressListener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

// SFTPターゲットクラス - SFTPストレージを操作する
public class SftpTarget implements PathTarget {

    @Setter
    private String id = null;
    private final SftpSettings config;
    private final SnapshotManager backupManager;

    private final SftpConnectionFactory mainClient;
    private final SftpConnectionFactory downloadClient;
    private final SftpConnectionFactory uploadClient;
    private final SftpTraceLogger protocolLogger;

    private static final int FILE_BUFFER_SIZE = 65536;

    private final Retryable.RetriableExceptionHandler retriableExceptionHandler = new Retryable.RetriableExceptionHandler() {

        @Override
        // 通常例外を処理する
        public void handleRegularException(Exception e) {

            if (e instanceof java.net.SocketTimeoutException ||
                (e.getMessage() != null && e.getMessage().contains("Read timed out"))) {
                MakeBackup.getInstance().getLogManager().devWarn("SFTP read timeout");
            }

            else if (e instanceof java.net.SocketException &&
                    (e.getMessage() != null && (e.getMessage().contains("Connection reset") ||
                                              e.getMessage().contains("Connection closed") ||
                                              e.getMessage().contains("Broken pipe")))) {
                MakeBackup.getInstance().getLogManager().devWarn("SFTP connection reset");
            }

            else if (e instanceof JSchException &&
                    e.getMessage() != null && e.getMessage().contains("session is down")) {
                MakeBackup.getInstance().getLogManager().devWarn("SFTP session is down");
            }
        }

        @Override
        // 最終例外を処理する
        public RuntimeException handleFinalException(Exception e) {

            if (e instanceof JSchException) {
                if (e.getMessage() != null) {

                    if (e.getMessage().contains("auth fail") ||
                        e.getMessage().contains("Authentication fail")) {
                        return new TargetConnectionException(getStorage(), "Authentication failed to SFTP server", e);
                    }

                    else if (e.getMessage().contains("UnknownHostException") ||
                             e.getMessage().contains("Connection refused") ||
                             e.getMessage().contains("connect failed")) {
                        return new TargetConnectionException(getStorage(), "Failed to establish connection to SFTP server", e);
                    }

                    else if (e.getMessage().contains("timeout") ||
                             e.getMessage().contains("timed out") ||
                             e.getMessage().contains("session is down")) {
                        return new TargetConnectionException(getStorage(), "Connection timed out", e);
                    }
                }
            }

            if (e instanceof SftpException sftpException) {

                if (sftpException.id == 2) {
                    return new TargetMethodException(getStorage(), "File not found", e);
                }

                else if (sftpException.id == 3 || sftpException.id == 4) {
                    return new TargetMethodException(getStorage(), "Permissions denied", e);
                }

                else if (sftpException.id == 5 ||
                        (e.getMessage() != null && (e.getMessage().contains("disk full") ||
                                                  e.getMessage().contains("quota exceeded")))) {
                    return new TargetLimitException(getStorage(), "SFTP storage quota exceeded", e);
                }
            }

            return new TargetMethodException(getStorage(), e.getMessage(), e);
        }

        // ストレージを取得する
        public Target getStorage() {
            return SftpTarget.this;
        }
    };

    // コンストラクタ - SFTP設定で初期化する
    public SftpTarget(SftpSettings config) {
        this.config = config;
        this.backupManager = new SnapshotManager(this);
        this.protocolLogger = config.isProtocolLogging() ? new SftpTraceLogger(config.getId()) : null;
        this.mainClient = new SftpConnectionFactory(this, "main");
        this.downloadClient = new SftpConnectionFactory(this, "download");
        this.uploadClient = new SftpConnectionFactory(this, "upload");
    }

    @Override
    // IDを取得する
    public String getId() {
        return this.id;
    }

    @Override
    // タイプを取得する
    public TargetType getType() {
        return TargetType.SFTP;
    }

    @Override
    // 設定を取得する
    public SftpSettings getConfig() {
        return config;
    }

    @Override
    // バックアップマネージャーを取得する
    public SnapshotManager getBackupManager() {
        return backupManager;
    }

    @Override
    // 接続をチェックする
    public boolean checkConnection() {
        return checkConnection(null);
    }

    @Override
    // 接続をチェックする（送信者付き）
    public boolean checkConnection(ModCommandSender sender) {
        try {
            mainClient.getClient();
            downloadClient.getClient();
            uploadClient.getClient();
            return true;
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to establish connection to the SFTP server", sender);
            MakeBackup.getInstance().getLogManager().warn(e);
            return false;
        }
    }

    @Override
    // 一覧を取得する
    public List<String> ls(String path) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<List<String>>) () -> {
            synchronized (mainClient) {
                ChannelSftp sftp = mainClient.getClient();
                logOperation("LS", path);
                return sftp.ls(path).stream()
                        .map(ChannelSftp.LsEntry::getFilename)
                        .filter(file -> !file.equals(".") && !file.equals(".."))
                        .toList();
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // 存在を確認する
    public boolean exists(String path) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<Boolean>) () -> {
            synchronized (mainClient) {
                ChannelSftp sftp = mainClient.getClient();
                logOperation("STAT", path);
                try {
                    sftp.stat(path);
                    return true;
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        return false;
                    }
                    throw e;
                }
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ファイルか確認する
    public boolean isFile(String path) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<Boolean>) () -> {
            synchronized (mainClient) {
                ChannelSftp sftp = mainClient.getClient();
                logOperation("STAT", path);
                return !sftp.stat(path).isDir();
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ディレクトリサイズを取得する
    public long getDirByteSize(String path) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<Long>) () -> {
            List<ChannelSftp.LsEntry> files = new ArrayList<>();
            long dirSize = 0;
            synchronized (mainClient) {
                ChannelSftp sftp = mainClient.getClient();
                logOperation("SIZE", path);
                if (!sftp.stat(path).isDir()) {
                    dirSize += sftp.stat(path).getSize();
                } else {
                    files = sftp.ls(path);
                }
            }
            for (ChannelSftp.LsEntry entry : files) {
                if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) {
                    continue;
                }
                dirSize += getDirByteSize(resolve(path, entry.getFilename()));
            }
            return dirSize;
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ディレクトリを作成する
    public void createDir(String newDirName, String parentDir) throws TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            synchronized (mainClient) {
                ChannelSftp sftp = mainClient.getClient();

                String newDirPath = resolve(parentDir, newDirName);
                logOperation("MKDIR", newDirPath);
                sftp.mkdir(newDirPath);
                if (!sftp.stat(newDirPath).isDir()) {
                    throw new TargetMethodException(this, "Directory creation verification failed: %s".formatted(newDirPath));
                }
                return null;
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ファイルをアップロードする
    public void uploadFile(InputStream sourceStream, String newFileName, String targetParentDir, TransferProgressListener progressListener) throws TargetLimitException, TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            synchronized (uploadClient) {
                ChannelSftp sftp = uploadClient.getClient();

                String targetPath = resolve(targetParentDir, newFileName);
                logOperation("PUT", targetPath);
                sftp.put(sourceStream, targetPath, new SftpStorageProgressListener(progressListener), ChannelSftp.OVERWRITE);
                if (sftp.stat(targetPath).isDir()) {
                    throw new TargetMethodException(this, "Upload verification failed: %s".formatted(targetPath));
                }
                return null;
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ファイルをダウンロードする
    public InputStream downloadFile(String sourcePath, TransferProgressListener progressListener) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<InputStream>) () -> {
            synchronized (downloadClient) {
                ChannelSftp sftp = downloadClient.getClient();
                logOperation("GET", sourcePath);
                return sftp.get(sourcePath, new SftpStorageProgressListener(progressListener));
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // 削除する
    public void delete(String path) throws TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            synchronized (mainClient) {
                ChannelSftp sftp = mainClient.getClient();
                logOperation("DELETE", path);
                SftpATTRS stat = sftp.stat(path);
                if (stat.isDir()) {
                    sftp.rmdir(path);
                } else {
                    sftp.rm(path);
                }
                if (exists(path)) {
                    throw new TargetMethodException(this, "Delete verification failed: %s".formatted(path));
                }
                return null;
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ファイル名を変更する
    public void renameFile(String path, String newFileName) throws TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            synchronized (mainClient) {
                ChannelSftp sftp = mainClient.getClient();
                String parentPath = "";
                if (path.contains(config.getPathSeparatorSymbol())) {
                    parentPath = path.substring(0, path.lastIndexOf(config.getPathSeparatorSymbol()));
                    parentPath += config.getPathSeparatorSymbol();
                }
                String targetPath = parentPath + newFileName;
                logOperation("RENAME", "%s -> %s".formatted(path, targetPath));
                sftp.rename(path, targetPath);
                if (!exists(targetPath) || exists(path)) {
                    throw new TargetMethodException(this, "Rename verification failed from \"%s\" to \"%s\"".formatted(path, targetPath));
                }
                return null;
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // 速度係数を取得する
    public int getStorageSpeedMultiplier() {
        return 8;
    }

    @Override
    // 破棄する
    public void destroy() {
        mainClient.disconnect();
        downloadClient.disconnect();
        uploadClient.disconnect();
        if (protocolLogger != null) {
            protocolLogger.close();
        }
    }

    @Override
    // ダウンロード完了を処理する
    public void downloadCompleted() throws TargetMethodException, TargetConnectionException {

    }

    // プロトコルロガーを取得する
    SftpTraceLogger getProtocolLogger() {
        return protocolLogger;
    }

    // 操作をログ出力する
    private void logOperation(String operation, String message) {
        if (protocolLogger != null) {
            protocolLogger.logOperation(operation, message);
        }
    }

    // SFTP進捗リスナークラス - 進捗を通知する
    private static class SftpStorageProgressListener implements SftpProgressMonitor {

        private final TransferProgressListener progressListener;

        // コンストラクタ - リスナーで初期化する
        SftpStorageProgressListener(TransferProgressListener progressListener) {
            this.progressListener = progressListener;
        }

        @Override
        // 初期化する
        public void init(int operationCode, String sourceDir, String destDir, long maxProgress) {
        }

        @Override
        // 進捗をカウントする
        public boolean count(long l) {
            progressListener.incrementProgress(l);
            return true;
        }

        @Override
        // 終了する
        public void end() {

        }
    }
}
