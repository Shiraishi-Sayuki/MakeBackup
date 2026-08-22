/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;

import com.sayuki.makebackup.MakeBackup;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ターゲットトレースロガークラス - 操作ログを記録する
class TargetTraceLogger implements Closeable {

    private static final long MAX_LOG_SIZE_BYTES = 1_048_576L;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final String storageId;
    private final File logFile;

    private PrintWriter writer;

    // コンストラクタ - ストレージIDで初期化する
    TargetTraceLogger(String storageId) {
        this.storageId = storageId;
        this.logFile = new File(MakeBackup.getInstance().getModDir().getParentFile().getParentFile(), "MakeBackup/logs/%s.log".formatted(storageId));
    }

    // ログを出力する
    synchronized void log(String clientRole, String type, String message) {
        try {
            openWriterIfNeeded();
            rotateIfNeeded();
            writer.printf("[%s] [%s] [%s] %s%n", LocalDateTime.now().format(TIMESTAMP_FORMAT), clientRole, type, message.stripTrailing());
            writer.flush();
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().devWarn("Failed to write protocol log for %s storage".formatted(storageId));
            MakeBackup.getInstance().getLogManager().devWarn(e);
        }
    }

    // 操作をログ出力する
    void logOperation(String operation, String message) {
        log("storage", operation, message);
    }

    // ライターを開く（必要なら）
    private void openWriterIfNeeded() throws IOException {
        if (writer != null) {
            return;
        }

        File parent = logFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create protocol log directory: %s".formatted(parent.getPath()));
        }
        writer = new PrintWriter(new FileWriter(logFile, true), true);
    }

    // ローテーションする（必要なら）
    private void rotateIfNeeded() throws IOException {
        if (!logFile.exists() || logFile.length() < MAX_LOG_SIZE_BYTES) {
            return;
        }

        closeWriter();
        File rotatedFile = new File(logFile.getParentFile(), "%s-%s.log".formatted(storageId, LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT)));
        int collisionIndex = 1;
        while (rotatedFile.exists()) {
            rotatedFile = new File(logFile.getParentFile(), "%s-%s-%d.log".formatted(storageId, LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT), collisionIndex++));
        }
        if (!logFile.renameTo(rotatedFile)) {
            throw new IOException("Failed to rotate protocol log: %s".formatted(logFile.getPath()));
        }
        openWriterIfNeeded();
    }

    // ライターを閉じる
    private void closeWriter() {
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }
    }

    @Override
    // クローズする
    public synchronized void close() {
        closeWriter();
    }
}
