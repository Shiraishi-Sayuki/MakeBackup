/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.target.support.BasicTransferProgressListener;
import com.sayuki.makebackup.core.target.support.TransferProgressListener;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// ZIP解凍ジョブクラス - ZIPを展開する
public class UnpackZipJob extends BaseJob implements SingleTargetJob {

    private final Target storage;
    private final String sourceZipDir;
    private final String targetFolderDir;

    private TransferProgressListener downloadProgressListener;
    private final List<TransferProgressListener> uploadProgressListeners = new ArrayList<>();

    private static final int STREAM_BUFFER_SIZE = 1048576;

    // コンストラクタ - 初期化する
    public UnpackZipJob(Target storage, String sourceZipDir, String targetFolderDir) {
        super();
        this.storage = storage;
        this.sourceZipDir = sourceZipDir;
        this.targetFolderDir = targetFolderDir;
    }

    @Override
    // 実行する
    public void run() {
        downloadProgressListener = new BasicTransferProgressListener();
        try (InputStream directInputStream = storage.downloadFile(sourceZipDir, downloadProgressListener);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(directInputStream, STREAM_BUFFER_SIZE);
             ZipInputStream zipInput = new ZipInputStream(bufferedInputStream)) {
            ZipEntry zipEntry;
            if (!storage.exists(targetFolderDir)) {
                storage.createDir(storage.getFileNameFromPath(targetFolderDir), storage.getParentPath(targetFolderDir));
            }

            while (!cancelled && (zipEntry = zipInput.getNextEntry()) != null) {

                String name = zipEntry.getName();
                try {
                    List<String> entryRelativeListedPath = new ArrayList<>();
                    if (zipEntry.isDirectory()) name = name.substring(0, name.length() - 1);
                    String currentRelativePath = name;
                    while (!(currentRelativePath = getParentFromZipPath(currentRelativePath)).isEmpty()) entryRelativeListedPath.add(getFileNameFromZipPath(currentRelativePath));
                    Collections.reverse(entryRelativeListedPath);
                    String entryParentRelativePath = targetFolderDir;
                    for (String dir : entryRelativeListedPath) {
                        entryParentRelativePath = storage.resolve(entryParentRelativePath, dir);
                    }

                    if (zipEntry.isDirectory()) {
                        if (zipEntry.getName().equals("/")) continue;
                        storage.createDir(getFileNameFromZipPath(name), entryParentRelativePath);
                    } else {
                        TransferProgressListener uploadProgressListener = new BasicTransferProgressListener();
                        uploadProgressListeners.add(uploadProgressListener);
                        storage.uploadFile(zipInput, getFileNameFromZipPath(name), entryParentRelativePath, uploadProgressListener);
                    }
                } catch (Exception e) {
                    warn("Something went wrong while trying to unpack file", sender);
                    warn(e);
                }
                 zipInput.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            storage.downloadCompleted();
        }
    }

    @Override
    // タスクを準備する - サイズを計算する
    public void prepareTask(ModCommandSender sender) throws IOException {
        if (maxProgress != 0) return;
        maxProgress = storage.getDirByteSize(sourceZipDir);
    }

    @Override
    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        return downloadProgressListener.getCurrentProgress();
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
    }

    @Override
    // ストレージを取得する
    public Target getStorage() {
        return storage;
    }

    // ZIPパスからファイル名を取得する
    private String getFileNameFromZipPath(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    // ZIPパスから親パスを取得する
    private String getParentFromZipPath(String path) {
        return path.substring(0, path.lastIndexOf("/") == -1 ? 0 : path.lastIndexOf("/"));
    }

    // アップロード済みバイト数を取得する
    public long getBytesUploaded() {
        return uploadProgressListeners.stream().mapToLong(TransferProgressListener::getCurrentProgress).sum();
    }
}
