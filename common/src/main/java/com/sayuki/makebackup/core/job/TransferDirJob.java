/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.jcraft.jsch.SftpException;
import com.sayuki.makebackup.core.target.LocalTarget;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.target.error.TargetLimitException;
import com.sayuki.makebackup.core.target.error.TargetQuotaExceededException;
import com.sayuki.makebackup.core.target.support.BasicTransferProgressListener;
import com.sayuki.makebackup.core.target.support.TransferProgressListener;
import com.sayuki.makebackup.core.helper.Helper;

import javax.naming.AuthenticationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

// 転送ジョブクラス - ディレクトリを転送する
public class TransferDirJob extends BaseJob implements DoubleTargetJob {

    private final Target sourceStorage;
    private final String sourceDir;
    private final Target targetStorage;
    private final String targetParentDir;
    private final String targetFileName;
    private final boolean forceExcludedDirs;

    private ArrayList<TransferProgressListener> downloadProgressListeners;

    private static final int STREAM_BUFFER_SIZE = 1048576;

    // コンストラクタ - 転送元と転送先で初期化する
    public TransferDirJob(Target sourceStorage, String sourceDir, Target targetStorage, String targetParentDir, String targetFileName, boolean forceExcludedDirs) {
        super();
        this.sourceStorage = sourceStorage;
        this.targetStorage = targetStorage;
        this.sourceDir = sourceDir;
        this.targetParentDir = targetParentDir;
        this.targetFileName = targetFileName;
        this.forceExcludedDirs = forceExcludedDirs;
    }

    @Override
    // 実行する
    public void run() {
        downloadProgressListeners = new ArrayList<>();
        if (!cancelled) {
            sendFolder(sourceDir, targetParentDir, targetFileName);
        }
    }

    @Override
    // タスクを準備する - サイズを計算する
    public void prepareTask(ModCommandSender sender) throws ExecutionException, InterruptedException, AuthenticationException, IOException, TargetLimitException, TargetQuotaExceededException, SftpException {
        if (maxProgress != 0) return;
        if (sourceStorage instanceof LocalTarget && !forceExcludedDirs) {
            maxProgress = Helper.getFileFolderByteSizeExceptExcluded(new File(sourceDir));
        } else {
            maxProgress = sourceStorage.getDirByteSize(sourceDir);
        }
    }

    // フォルダを送信する - 再帰的に転送する
    private void sendFolder(final String sourceDir, final String targetParentDir, String targetFileName) {
        if (cancelled) return;

        if (!sourceStorage.exists(sourceDir)) {
            warn("Something went wrong while trying to send files from %s".formatted(sourceDir));
            warn("Directory %s doesn't exist".formatted(sourceDir), sender);
            return;
        }

        if (sourceStorage instanceof LocalTarget && !forceExcludedDirs) {
            if (Helper.isExcludedDirectory(new File(sourceDir), sender)) return;
        }

        if (sourceStorage.isFile(sourceDir) && !sourceStorage.getFileNameFromPath(sourceDir).equals("session.lock")) {
            try {
                final TransferProgressListener progressListener = new BasicTransferProgressListener();
                downloadProgressListeners.add(progressListener);
                try (InputStream directInputStream = sourceStorage.downloadFile(sourceDir, progressListener);
                     BufferedInputStream inputStream = new BufferedInputStream(directInputStream, STREAM_BUFFER_SIZE)) {
                    targetStorage.uploadFile(inputStream, targetFileName, targetParentDir);
                } catch (Exception e) {
                    warn("Failed to send file \"%s\" to %s storage".formatted(sourceDir, targetStorage.getId()));
                    warn(e);
                } finally {
                    sourceStorage.downloadCompleted();
                }

            } catch (Exception e) {
                warn("Failed to send file \"%s\" to %s storage".formatted(sourceDir, targetStorage.getId()), sender);
                warn(e);
            }
        }
        if (sourceStorage.isDir(sourceDir)) {
            targetStorage.createDir(targetFileName, targetParentDir);
            for (String file : sourceStorage.ls(sourceDir)) {
                sendFolder(sourceStorage.resolve(sourceDir, file), targetStorage.resolve(targetParentDir, targetFileName), file);
            }
        }
    }

    @Override
    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        if (downloadProgressListeners == null) return 0;
        return downloadProgressListeners.stream().mapToLong(TransferProgressListener::getCurrentProgress).sum();
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
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
