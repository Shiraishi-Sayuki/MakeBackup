/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.LocalTarget;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.target.support.BasicTransferProgressListener;
import com.sayuki.makebackup.core.target.support.TransferProgressListener;
import com.sayuki.makebackup.core.helper.Helper;

import java.io.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// ZIP転送ジョブクラス - 複数ディレクトリをZIPにして転送する
public class TransferDirsAsZipJob extends BaseJob implements DoubleTargetJob {

    private static final int FILE_BUFFER_SIZE = 65536;
    private static final int STREAM_BUFFER_SIZE = 1048576;
    private static final int PIPE_BUFFER_SIZE = 4194304;

    private final Target sourceStorage;
    private final List<String> sourceDirs;
    private final boolean forceExcludedDirs;
    private final boolean createRootDirInTargetZIP;
    private final Target targetStorage;
    private final String targetParentDir;
    private final String targetZipFileName;

    private final List<TransferProgressListener> downloadProgressListeners = new java.util.ArrayList<>();
    private final AtomicLong bytesUploaded = new AtomicLong(0);

    // コンストラクタ - 初期化する
    public TransferDirsAsZipJob(Target sourceStorage, List<String> sourceDirs, Target targetStorage, String targetParentDir, String targetZipFileName,
                           boolean createRootDirInTargetZIP, boolean forceExcludedDirs) {

        this.sourceStorage = sourceStorage;
        this.targetStorage = targetStorage;
        this.sourceDirs = sourceDirs;
        this.targetParentDir = targetParentDir;
        this.targetZipFileName = targetZipFileName;
        this.createRootDirInTargetZIP = createRootDirInTargetZIP;
        this.forceExcludedDirs = forceExcludedDirs;
    }

    @Override
    // 実行する
    public void run() {

        try (PipedInputStream pipedInputStream = new PipedInputStream(PIPE_BUFFER_SIZE);
             PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream)) {

            MakeBackup.getInstance().getScheduleManager().runAsync(() -> {

                try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(pipedOutputStream, STREAM_BUFFER_SIZE);
                     ZipOutputStream targetZipOutputStream = new ZipOutputStream(bufferedOutputStream)) {

                    targetZipOutputStream.setLevel(targetStorage.getConfig().getZipCompressionLevel());

                    for (String sourceDirToAdd : sourceDirs) {
                        if (cancelled) return;
                        if (createRootDirInTargetZIP) {
                            addDirToZip(targetZipOutputStream, sourceDirToAdd, sourceStorage.getFileNameFromPath(sourceDirToAdd));
                        } else {
                            addDirToZip(targetZipOutputStream, sourceDirToAdd, "");
                        }
                    }

                } catch (Exception e) {
                    warn("Failed to send ZIP entry to %s storage".formatted(targetStorage), sender);
                    warn(e);
                }
            });

            targetStorage.uploadFile(pipedInputStream, targetZipFileName, targetParentDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        return downloadProgressListeners.stream().mapToLong(TransferProgressListener::getCurrentProgress).sum();
    }

    @Override
    // タスクを準備する - サイズを計算する
    public void prepareTask(ModCommandSender sender) {
        if (maxProgress != 0) return;
        if (sourceStorage instanceof LocalTarget && !forceExcludedDirs) {
            for (String dir : sourceDirs) {
                this.maxProgress += Helper.getFileFolderByteSizeExceptExcluded(new File(dir));
            }
        } else {
            for (String dir : sourceDirs) {
                this.maxProgress += sourceStorage.getDirByteSize(dir);
            }
        }
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
    }

    // ディレクトリをZIPに追加する - 再帰的に処理する
    private void addDirToZip(ZipOutputStream zip, String sourceDir, String relativeDirPath) {
        if (cancelled) return;
        if (!sourceStorage.exists(sourceDir)) {
            warn("Directory does not exist: %s".formatted(sourceDir), sender);
            return;
        }
        if (sourceStorage instanceof LocalTarget && !forceExcludedDirs && Helper.isExcludedDirectory(new File(sourceDir), sender)) return;
        try {
            if (sourceStorage.isFile(sourceDir)) {

                long crc = 0;
                if (isAlreadyCompressed(sourceStorage, sourceDir)) {
                    crc = calculateCRC(sourceDir);
                }

                TransferProgressListener downloadProgressListener = new BasicTransferProgressListener();
                downloadProgressListeners.add(downloadProgressListener);
                try (InputStream directInputStream = sourceStorage.downloadFile(sourceDir, downloadProgressListener);
                     BufferedInputStream bufferedInputStream = new BufferedInputStream(directInputStream, FILE_BUFFER_SIZE)) {

                    ZipEntry entry = new ZipEntry(relativeDirPath);
                    if (isAlreadyCompressed(sourceStorage, sourceDir)) {
                        entry.setMethod(ZipEntry.STORED);
                        entry.setCompressedSize(sourceStorage.getDirByteSize(sourceDir));
                        entry.setCrc(crc);
                    }
                    entry.setSize(sourceStorage.getDirByteSize(sourceDir));
                    zip.putNextEntry(entry);
                    byte[] buffer = new byte[FILE_BUFFER_SIZE];
                    int read;
                    while ((read = bufferedInputStream.read(buffer)) != -1) {
                        if (cancelled) return;
                        zip.write(buffer, 0, read);
                        bytesUploaded.getAndAdd(read);
                    }
                    zip.closeEntry();
                } finally {
                    sourceStorage.downloadCompleted();
                }
            }
        } catch (Exception e) {
            warn("Error adding to ZIP: %s".formatted(sourceDir), sender);
            warn(e);
        }
        if (sourceStorage.isDir(sourceDir)) {
            try {
                ZipEntry entry = new ZipEntry(relativeDirPath.endsWith("/") ? relativeDirPath : "%s/".formatted(relativeDirPath));
                zip.putNextEntry(entry);
                zip.closeEntry();

                List<String> ls = sourceStorage.ls(sourceDir);
                for (String file : ls) {
                    if (!"session.lock".equals(file)) {
                        addDirToZip(zip, sourceStorage.resolve(sourceDir, file), "%s/%s".formatted(relativeDirPath, file));
                    }
                }
            } catch (Exception e) {
                warn("Error adding a dir to ZIP: %s".formatted(sourceDir), sender);
                warn(e);
            }
        }
    }

    // 既に圧縮済みか判定する
    private boolean isAlreadyCompressed(Target storage, String path) {

        String name = storage.getFileNameFromPath(path).toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".gz")
                || name.endsWith(".7z") || name.endsWith(".rar")
                || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".mp3")
                || name.endsWith(".mp4") || name.endsWith(".avi")
                || name.endsWith(".mkv") || name.endsWith(".webm")
                || name.endsWith(".webp");
    }

    // CRCを計算する
    private long calculateCRC(String path) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(
                sourceStorage.downloadFile(path), FILE_BUFFER_SIZE)) {
            CRC32 crc = new CRC32();
            byte[] buffer = new byte[FILE_BUFFER_SIZE];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                crc.update(buffer, 0, read);
            }
            return crc.getValue();
        } finally {
            sourceStorage.downloadCompleted();
        }
    }

    // アップロード済みバイト数を取得する
    public long getBytesUploaded() {
        return bytesUploaded.get();
    }

    @Override
    // 転送元ストレージを取得する
    public Target getSourceStorage() {
        return sourceStorage;
    }

    @Override
    // 転送先ストレージを取得する
    public Target getTargetStorage() {
        return targetStorage;
    }
}
