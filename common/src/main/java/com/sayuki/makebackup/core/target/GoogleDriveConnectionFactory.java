/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;

// GoogleDriveコネクションファクトリークラス - GoogleDrive接続を管理する
public class GoogleDriveConnectionFactory {

    private final GoogleDriveTarget storage;

    private Drive driveService = null;

    private final String APPLICATION_NAME = "BACKUPER";

    private final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final NetHttpTransport NET_HTTP_TRANSPORT = new NetHttpTransport();

    // コンストラクタ - 初期化する
    GoogleDriveConnectionFactory(GoogleDriveTarget storage) {
        this.storage = storage;
    }

    // クライアントを取得する
    synchronized Drive getClient() throws TargetConnectionException {
        if (driveService != null) {
            try {

                driveService.files().get("").setFields("name").execute().getName();
                return driveService;
            } catch (Exception ignored) {

                driveService = null;
            }
        }

        try {

            Credential credential = storage.returnCredentialIfAuthorized();
            if (credential == null) {
                throw new TargetConnectionException(storage, "Not authorized in Google Drive!");
            }

            driveService = new Drive.Builder(NET_HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .setHttpRequestInitializer(httpRequest -> {
                        credential.initialize(httpRequest);
                        httpRequest.setConnectTimeout(300 * 60000);
                        httpRequest.setReadTimeout(300 * 60000);
                    })
                    .build();

            return driveService;
        } catch (Exception e) {
            throw new TargetConnectionException(storage, "Not authorized in Google Drive!", e);
        }
    }

    // 切断する
    void disconnect() {
        driveService = null;
    }
}
