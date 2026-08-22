/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target.support;

import java.io.IOException;
import java.io.InputStream;

// 転送進捗付き入力ストリームクラス - 読み込み進捗を追跡する
public class TransferProgressInputStream extends InputStream {

    private final InputStream inputStream;
    private final TransferProgressListener progressListener;

    // コンストラクタ - ストリームとリスナーで初期化する
    public TransferProgressInputStream(InputStream inputStream, TransferProgressListener progressListener) {
        this.inputStream = inputStream;
        this.progressListener = progressListener;
    }

    @Override
    // 読み込む - 1バイト読み込む
    public int read() throws IOException {

        int result = inputStream.read();
        if (result != -1) {
            progressListener.incrementProgress(1);
        }
        return result;
    }

    @Override
    // 読み込む - バイト配列に読み込む
    public int read(byte[] b) throws IOException {

        int bytesRead = inputStream.read(b);
        if (bytesRead > 0) {
            progressListener.incrementProgress(bytesRead);
        }
        return bytesRead;
    }

    @Override
    // 読み込む - オフセット指定で読み込む
    public int read(byte[] b, int off, int len) throws IOException {

        int bytesRead = inputStream.read(b, off, len);
        if (bytesRead > 0) {
            progressListener.incrementProgress(bytesRead);
        }
        return bytesRead;
    }

    @Override
    // 閉じる
    public void close() throws IOException {
        super.close();
        inputStream.close();
    }
}
