/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;
import com.sayuki.makebackup.platform.ModCommandSender;

import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.SnapshotManager;
import com.sayuki.makebackup.core.settings.FtpSettings;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;
import com.sayuki.makebackup.core.target.error.TargetLimitException;
import com.sayuki.makebackup.core.target.error.TargetMethodException;
import com.sayuki.makebackup.core.target.support.Retryable;
import com.sayuki.makebackup.core.target.support.TransferProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;

// FTPターゲットクラス - FTPストレージを操作する
public class FtpTarget implements PathTarget {

    @Setter
    private String id = null;
    private final FtpSettings config;
    private final SnapshotManager backupManager;

    private final FtpConnectionFactory mainClient;
    private final FtpConnectionFactory downloadClient;
    private final FtpConnectionFactory uploadClient;
    private final FtpTraceLogger protocolLogger;

    private final Retryable.RetriableExceptionHandler retriableExceptionHandler = new Retryable.RetriableExceptionHandler() {

        @Override
        // 通常例外を処理する
        public void handleRegularException(Exception e) {

            if (e instanceof SocketTimeoutException ||
                e.getMessage() != null && e.getMessage().contains("Read timed out")) {
                MakeBackup.getInstance().getLogManager().devWarn("FTP read timeout");
            }

            else if (e instanceof SocketException &&
                     (e.getMessage() != null && (e.getMessage().contains("Connection reset") ||
                                               e.getMessage().contains("Connection closed") ||
                                               e.getMessage().contains("Broken pipe")))) {
                MakeBackup.getInstance().getLogManager().devWarn("FTP connection reset");
            }

            else if (e instanceof IOException &&
                     e.getMessage() != null && e.getMessage().contains("Could not parse passive host information")) {
                MakeBackup.getInstance().getLogManager().devWarn("FTP passive mode error");
            }
        }

        @Override
        // 最終例外を処理する
        public RuntimeException handleFinalException(Exception e) {

            if (e instanceof IOException) {
                if (e.getMessage() != null) {

                    if (e.getMessage().contains("421") || e.getMessage().contains("Failed to establish connection")) {
                        return new TargetConnectionException(getStorage(), "Failed to establish connection to FTP server", e);
                    }

                    else if (e.getMessage().contains("timed out") || e.getMessage().contains("Read timed out")) {
                        return new TargetConnectionException(getStorage(), "Connection timed out", e);
                    }

                    else if (e.getMessage().contains("Could not parse passive host information")) {
                        return new TargetMethodException(getStorage(), "Failed to establish passive connection", e);
                    }

                    else if (e.getMessage().contains("550") &&
                             (e.getMessage().contains("quota exceeded") || e.getMessage().contains("disk full"))) {
                        return new TargetLimitException(getStorage(), "FTP storage quota exceeded", e);
                    }

                    else if (e.getMessage().contains("550") || e.getMessage().contains("Permissions denied")) {
                        return new TargetMethodException(getStorage(), "Access denied or file not found", e);
                    }
                }
            }

            if (e.getMessage() != null && e.getMessage().contains("in is null")) {
                return new TargetMethodException(getStorage(), "Failed to get input stream", e);
            }

            return new TargetMethodException(getStorage(), e.getMessage(), e);
        }

        // ストレージを取得する
        public Target getStorage() {
            return FtpTarget.this;
        }
    };

    // コンストラクタ - FTP設定で初期化する
    public FtpTarget(FtpSettings config) {
        this.config = config;
        this.backupManager = new SnapshotManager(this);
        this.protocolLogger = config.isProtocolLogging() ? new FtpTraceLogger(config.getId()) : null;
        this.mainClient = new FtpConnectionFactory(this, "main");
        this.downloadClient = new FtpConnectionFactory(this, "download");
        this.uploadClient = new FtpConnectionFactory(this, "upload");
    }

    @Override
    // IDを取得する
    public String getId() {
        return this.id;
    }

    @Override
    // タイプを取得する
    public TargetType getType() {
        return TargetType.FTP;
    }

    @Override
    // 設定を取得する
    public FtpSettings getConfig() {
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
            MakeBackup.getInstance().getLogManager().warn("Failed to establish connection to the FTP(S) server", sender);
            MakeBackup.getInstance().getLogManager().warn(e);
            return false;
        }
    }

    @Override
    // 一覧を取得する
    public List<String> ls(String path) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<List<String>>) () -> {
            synchronized (mainClient) {
                FTPClient ftp = mainClient.getClient();
                changeWorkingDirectory(ftp, path);
                FTPFile[] files = ftp.listFiles();
                if (files == null) {
                    throw new IOException("Failed to list files in directory: " + path);
                }
                return Arrays.stream(files)
                    .map(FTPFile::getName)
                    .filter(fileName -> !fileName.equals(".") && !fileName.equals(".."))
                    .toList();
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // パスを解決する
    public String resolve(String path, String fileName) {
        if (path == null) {
            return fileName;
        }
        if (!path.endsWith(config.getPathSeparatorSymbol())) {
            path = "%s%s".formatted(path, config.getPathSeparatorSymbol());
        }
        return "%s%s".formatted(path, fileName);
    }

    @Override
    // 存在を確認する
    public boolean exists(String path) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<Boolean>) () -> {
            synchronized (mainClient) {
                FTPClient ftp = mainClient.getClient();

                String parentPath = getParentPath(path);

                String fileName = getFileNameFromPath(path);

                if (!ftp.changeWorkingDirectory(parentPath)) return false;
                return Arrays.stream(ftp.listFiles()).map(FTPFile::getName).anyMatch(name -> name.equals(fileName));
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ファイルか確認する
    public boolean isFile(String path) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<Boolean>) () -> {
            synchronized (mainClient) {
                FTPClient ftp = mainClient.getClient();

                String parentPath = getParentPath(path);

                String fileName = getFileNameFromPath(path);

                if (!ftp.changeWorkingDirectory(path)) {
                    if (!ftp.changeWorkingDirectory(parentPath)) return false;
                    return Arrays.stream(ftp.listFiles()).map(FTPFile::getName).anyMatch(name -> name.equals(fileName));
                }
                FTPFile[] listFiles = ftp.listFiles();
                return listFiles.length == 0 || listFiles.length == 1 && listFiles[0].getName().equals(getFileNameFromPath(path));
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ディレクトリサイズを取得する
    public long getDirByteSize(String path) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<Long>) () -> {
            FTPFile[] files = new FTPFile[0];
            long dirSize = 0;
            synchronized (mainClient) {
                FTPClient ftp = mainClient.getClient();

                String parentPath = getParentPath(path);

                String fileName = getFileNameFromPath(path);

                changeWorkingDirectory(ftp, parentPath);

                if (isFile(path)) {
                    dirSize += Long.valueOf(ftp.getSize(fileName));
                }
                if (isDir(path)) {

                    files = ftp.listFiles();
                    if (files == null) {
                        throw new TargetMethodException(this, "Failed to list files in directory: " + path);
                    }
                }
            }
            for (FTPFile file : files) {
                if (file.getName().equals(".") || file.getName().equals("..")) {
                    continue;
                }
                dirSize += getDirByteSize(resolve(path, file.getName()));
            }
            return dirSize;
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ディレクトリを作成する
    public void createDir(String newDirName, String parentDir) throws TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            synchronized (mainClient) {
                FTPClient ftp = mainClient.getClient();
                changeWorkingDirectory(ftp, parentDir);
                makeDirectory(ftp, newDirName);
                return null;
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ファイルをアップロードする
    public void uploadFile(InputStream sourceStream, String newFileName, String targetParentDir, TransferProgressListener progressListener)
            throws TargetLimitException, TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            synchronized (uploadClient) {
                FTPClient ftp = uploadClient.getClient();
                ftp.setCopyStreamListener(new FtpStorageProgressListener(progressListener));
                changeWorkingDirectory(ftp, targetParentDir);
                if (!ftp.storeFile(newFileName, sourceStream)) {
                    throw new TargetMethodException(this, "Failed to upload stream to \"%s\"".formatted(this.resolve(ftp.printWorkingDirectory(), newFileName)), new RuntimeException(ftp.getReplyString()));
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
                FTPClient ftp = downloadClient.getClient();

                String parentPath = getParentPath(sourcePath);

                String fileName = getFileNameFromPath(sourcePath);

                changeWorkingDirectory(ftp, parentPath);
                return new FtpStorageInputStream(ftp.retrieveFileStream(fileName), progressListener);
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ダウンロード完了を処理する
    public void downloadCompleted() throws TargetMethodException, TargetConnectionException {
        try {
            synchronized (downloadClient) {
                FTPClient ftp = downloadClient.getClient();

                boolean completed = ftp.completePendingCommand();
                if (!completed) {
                    MakeBackup.getInstance().getLogManager().devWarn("FTP command completion returned false");
                }
            }
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().devWarn(e);
        }
    }

    @Override
    // 削除する
    public void delete(String path) throws TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            synchronized (mainClient) {
                FTPClient ftp = mainClient.getClient();

                String parentPath = getParentPath(path);

                String fileName = getFileNameFromPath(path);

                changeWorkingDirectory(ftp, parentPath);
                if (isFile(path)) {
                    mainClient.resetWorkingDirectory();
                    changeWorkingDirectory(ftp, parentPath);

                    deleteFile(ftp, fileName);
                } else {
                    mainClient.resetWorkingDirectory();
                    changeWorkingDirectory(ftp, parentPath);

                    removeDirectory(ftp, fileName);
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
                FTPClient ftp = mainClient.getClient();

                String parentPath = getParentPath(path);

                String fileName = getFileNameFromPath(path);

                changeWorkingDirectory(ftp, parentPath);
                rename(ftp, fileName, newFileName);
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

    // プロトコルロガーを取得する
    FtpTraceLogger getProtocolLogger() {
        return protocolLogger;
    }

    // 作業ディレクトリを変更する
    private void changeWorkingDirectory(FTPClient ftp, String path) throws IOException {
        if (!ftp.changeWorkingDirectory(path)) {
            throw ftpCommandException(ftp, "change working directory to \"%s\"".formatted(path));
        }
    }

    // ディレクトリを作成する
    private void makeDirectory(FTPClient ftp, String path) throws IOException {
        if (!ftp.makeDirectory(path)) {
            throw ftpCommandException(ftp, "create directory \"%s\"".formatted(path));
        }
    }

    // ファイルを削除する
    private void deleteFile(FTPClient ftp, String path) throws IOException {
        if (!ftp.deleteFile(path)) {
            throw ftpCommandException(ftp, "delete file \"%s\"".formatted(path));
        }
    }

    // ディレクトリを削除する
    private void removeDirectory(FTPClient ftp, String path) throws IOException {
        if (!ftp.removeDirectory(path)) {
            throw ftpCommandException(ftp, "remove directory \"%s\"".formatted(path));
        }
    }

    // 名前を変更する
    private void rename(FTPClient ftp, String from, String to) throws IOException {
        if (!ftp.rename(from, to)) {
            throw ftpCommandException(ftp, "rename \"%s\" to \"%s\"".formatted(from, to));
        }
    }

    // FTPコマンド例外を生成する
    private TargetMethodException ftpCommandException(FTPClient ftp, String action) {
        return new TargetMethodException(this, "Failed to %s. FTP reply: %s".formatted(action, ftp.getReplyString()));
    }

    // FTP進捗リスナークラス - 進捗を通知する
    private static class FtpStorageProgressListener implements CopyStreamListener {

        private final TransferProgressListener progressListener;

        // コンストラクタ - リスナーで初期化する
        FtpStorageProgressListener(TransferProgressListener progressListener) {
            this.progressListener = progressListener;
        }

        @Override
        // 転送バイト数を通知する（イベント）
        public void bytesTransferred(CopyStreamEvent copyStreamEvent) {
            progressListener.incrementProgress(copyStreamEvent.getBytesTransferred());
        }

        @Override
        // 転送バイト数を通知する（詳細）
        public void bytesTransferred(long totalBytesTransferred, int delta, long totalStreamSize) {
            progressListener.incrementProgress(delta);
        }
    }

    // FTP入力ストリームクラス - 進捗付きで読み込む
    private static class FtpStorageInputStream extends InputStream {

        private final InputStream inputStream;
        private final TransferProgressListener progressListener;

        // コンストラクタ - ストリームとリスナーで初期化する
        FtpStorageInputStream(InputStream inputStream, TransferProgressListener progressListener) {
            this.inputStream = inputStream;
            this.progressListener = progressListener;
        }

        @Override
        // 1バイト読み込む
        public int read() throws IOException {

            int result = inputStream.read();
            if (result != -1) {
                progressListener.incrementProgress(1);
            }
            return result;
        }

        @Override
        // バイト配列に読み込む
        public int read(byte[] b) throws IOException {

            int bytesRead = inputStream.read(b);
            if (bytesRead > 0) {
                progressListener.incrementProgress(bytesRead);
            }
            return bytesRead;
        }

        @Override
        // 指定範囲に読み込む
        public int read(byte[] b, int off, int len) throws IOException {

            int bytesRead = inputStream.read(b, off, len);
            if (bytesRead > 0) {
                progressListener.incrementProgress(bytesRead);
            }
            return bytesRead;
        }
    }}
