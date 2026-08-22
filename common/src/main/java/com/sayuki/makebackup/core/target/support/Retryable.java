/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target.support;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;
import com.sayuki.makebackup.core.target.error.TargetLimitException;
import com.sayuki.makebackup.core.target.error.TargetMethodException;
import com.sayuki.makebackup.core.target.error.TargetQuotaExceededException;

// リトライ可能インターフェース - 再試行ロジックを提供する
@FunctionalInterface
public interface Retryable<T> {

    int DEFAULT_RETRIES = 5;
    int DEFAULT_RETRY_DELAY_MILLIS = 3000;

    // 実行する
    T run() throws Exception;

    // リトライする - 例外ハンドラー付きで再試行する
    default T retry(RetriableExceptionHandler exceptionHandler, int retries, int retryDelayMillis) throws TargetMethodException, TargetConnectionException, TargetLimitException, TargetQuotaExceededException {
        int completedRetries = 0;

        while (completedRetries < retries) {
            try {
                return run();
            } catch (Exception e) {
                completedRetries++;

                if (completedRetries == retries) {
                    if (e instanceof TargetConnectionException storageConnectionException) {
                        throw storageConnectionException;
                    } else if (e instanceof TargetLimitException storageLimitException) {
                        throw storageLimitException;
                    } else if (e instanceof TargetQuotaExceededException storageQuotaExceededException) {
                        throw storageQuotaExceededException;
                    } else if (e instanceof TargetMethodException storageMethodException) {
                        throw storageMethodException;
                    } else {
                        throw exceptionHandler.handleFinalException(e);
                    }
                } else {
                    MakeBackup.getInstance().getLogManager().devWarn("Operation failed, retrying in " + (retryDelayMillis / 1000) + " seconds... (" + completedRetries + "/" + retries + ")");
                    MakeBackup.getInstance().getLogManager().devWarn(e);
                    if (!(e instanceof TargetLimitException || e instanceof TargetQuotaExceededException || e instanceof TargetConnectionException || e instanceof TargetMethodException)) {
                        exceptionHandler.handleRegularException(e);
                    }
                }

                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException ignored) {}
            }
        }

        throw new RuntimeException("Unexpected error in Retryable logic");
    }

    // リトライする - デフォルト回数で再試行する
    default T retry(RetriableExceptionHandler exceptionHandler) throws TargetMethodException, TargetConnectionException, TargetLimitException, TargetQuotaExceededException {
        return retry(exceptionHandler, DEFAULT_RETRIES, DEFAULT_RETRY_DELAY_MILLIS);
    }

    // リトライ例外ハンドラーインターフェース - 例外処理を定義する
    interface RetriableExceptionHandler {

        // 通常例外を処理する
        void handleRegularException(Exception e);

        // 最終例外を処理する
        RuntimeException handleFinalException(Exception e);
    }
}
