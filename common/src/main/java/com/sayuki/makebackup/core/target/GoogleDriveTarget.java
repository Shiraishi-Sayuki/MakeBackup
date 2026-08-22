/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.fluent.Request;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.SnapshotManager;
import com.sayuki.makebackup.core.settings.GoogleDriveSettings;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;
import com.sayuki.makebackup.core.target.error.TargetLimitException;
import com.sayuki.makebackup.core.target.error.TargetMethodException;
import com.sayuki.makebackup.core.target.error.TargetQuotaExceededException;
import com.sayuki.makebackup.core.target.support.Retryable;
import com.sayuki.makebackup.core.target.support.TransferProgressInputStream;
import com.sayuki.makebackup.core.target.support.TransferProgressListener;
import com.sayuki.makebackup.core.helper.ObfuscationHelper;
import com.sayuki.makebackup.core.helper.UiHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

// GoogleDriveターゲットクラス - Googleドライブを操作する
public class GoogleDriveTarget implements UserAuthTarget {

    @Setter
    private String id = null;
    private final GoogleDriveSettings config;
    private final SnapshotManager backupManager;

    private Credential credential = null;

    private final GoogleDriveConnectionFactory mainClient;
    private final TargetTraceLogger protocolLogger;

    private static final String APPLICATION_NAME = "BACKUPER";

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final List<String> DRIVE_SCOPES = List.of(DriveScopes.DRIVE_FILE);
    private static final NetHttpTransport NET_HTTP_TRANSPORT = new NetHttpTransport();
    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    Cache<Pair<String, String>, List<File>> cacheLs = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .build();

    private final com.sayuki.makebackup.core.target.support.Retryable.RetriableExceptionHandler retriableExceptionHandler = new com.sayuki.makebackup.core.target.support.Retryable.RetriableExceptionHandler() {

        final int RATE_LIMIT_DELAY_MILLIS = 10000;

        @Override
        // 通常例外を処理する
        public void handleRegularException(Exception e) throws TargetMethodException, TargetConnectionException, TargetLimitException, TargetQuotaExceededException {
            if (e instanceof GoogleJsonResponseException googleJsonResponseException) {
                if (googleJsonResponseException.getDetails().getErrors() != null && googleJsonResponseException.getDetails().getErrors().stream().anyMatch(errorInfo -> errorInfo.getReason().equals("rateLimitExceeded") || errorInfo.getReason().equals("userRateLimitExceeded"))) {
                    MakeBackup.getInstance().getLogManager().devWarn("Rate limit exceeded, retry in %s seconds...".formatted(RATE_LIMIT_DELAY_MILLIS / 1000));
                    try {
                        Thread.sleep(RATE_LIMIT_DELAY_MILLIS);
                    } catch (Exception ignored) {

                    }
                }
            }
        }

        @Override
        // 最終例外を処理する
        public RuntimeException handleFinalException(Exception e) throws TargetMethodException, TargetConnectionException, TargetLimitException, TargetQuotaExceededException {
            if (e instanceof GoogleJsonResponseException googleJsonResponseException) {
                if (googleJsonResponseException.getDetails().getCode() == 401) {
                    return new TargetConnectionException(getStorage(), "Failed to authorize user in Google Drive");
                }
                if (googleJsonResponseException.getDetails().getErrors() != null) {
                    if (googleJsonResponseException.getDetails().getErrors().stream().anyMatch(errorInfo -> errorInfo.getReason().equals("storageQuotaExceeded"))) {
                        return new TargetLimitException(getStorage(), "Target space limit exceeded");
                    }
                    if (googleJsonResponseException.getDetails().getErrors().stream().anyMatch(errorInfo -> errorInfo.getReason().equals("rateLimitExceeded") || errorInfo.getReason().equals("userRateLimitExceeded"))) {
                        return new TargetQuotaExceededException(getStorage(), "Target rate limit exceeded");
                    }
                }
            }
            return new TargetMethodException(getStorage(), e.getMessage(), e);
        }

        // ストレージを取得する
        public Target getStorage() {
            return GoogleDriveTarget.this;
        }
    };

    // コンストラクタ - GoogleDrive設定で初期化する
    public GoogleDriveTarget(GoogleDriveSettings config) {
        this.config = config;
        this.backupManager = new SnapshotManager(this);
        this.mainClient = new GoogleDriveConnectionFactory(this);
        this.protocolLogger = config.isProtocolLogging() ? new TargetTraceLogger(config.getId()) : null;
    }

    // クライアントシークレットを読み込む
    private static GoogleClientSecrets loadClientSecrets() throws TargetConnectionException {
        try {
            InputStream stream = MakeBackup.getInstance().getResource("google_cred.txt");

            if (stream == null) {
                throw new TargetConnectionException(null, "google_cred.txt is missing inside the mod jar!");
            }

            return JSON_FACTORY.fromString(ObfuscationHelper.decrypt(IOUtils.toString(stream)), GoogleClientSecrets.class);
        } catch (TargetConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new TargetConnectionException(null, "Failed to load google_cred.txt: " + e.getMessage(), e);
        }
    }

    // 強制的に認証する
    public void authorizeForced(ModCommandSender sender) throws TargetConnectionException {
        try {
            this.credential = null;

            GoogleClientSecrets clientSecrets = loadClientSecrets();

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    NET_HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, DRIVE_SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(config.getTokenFolder()))
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setHost("localhost").setPort(8888).build();
            AuthorizationCodeInstalledApp app = new AuthorizationCodeInstalledApp(flow, receiver) {
                @Override
                // 認証URLを処理する
                protected void onAuthorization(com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl authorizationUrl) throws java.io.IOException {
                    String url = authorizationUrl.build();
                    Component header = Component.empty().append(Component.text("Account linking").decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD));
                    Component message = Component.empty()
                            .append(Component.text("Open this link in your browser to authorize:"))
                            .append(Component.newline())
                            .append(Component.text(url).clickEvent(ClickEvent.openUrl(url)).decorate(TextDecoration.UNDERLINED).color(NamedTextColor.AQUA))
                            .append(Component.newline()).append(Component.newline())
                            .append(Component.text("External server? If browser shows connection failed, copy ?code= from URL bar and run:").color(NamedTextColor.YELLOW))
                            .append(Component.newline())
                            .append(Component.text("/makebackup account link " + id + " <code>").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.text("You can paste full URL too.").color(NamedTextColor.GRAY));
                    UiHelper.sendFramedMessage(header, message, sender);
                    MakeBackup.getInstance().getLogManager().log("Authorize URL: " + url, sender);
                    try { super.onAuthorization(authorizationUrl); } catch (Exception ignored) {}
                }
            };
            this.credential = app.authorize(id);
            if (this.credential == null) throw new TargetConnectionException(this, "Failed to authorize user in %s storage".formatted(id));

            Component header = Component.empty()
                    .append(Component.text("Account linking"));

            Component message = Component.empty()
                    .append(Component.text("Account has been successfully linked to %s storage".formatted(id)));

            UiHelper.sendFramedMessage(header, message, sender);

        } catch (Exception e) {
            Component header = Component.empty()
                    .append(Component.text("Account linking"));

            Component message = Component.empty()
                    .append(Component.text("Failed to link account to %s storage:".formatted(this.id))
                            .color(NamedTextColor.RED));

            UiHelper.sendFramedMessage(header, message, sender);
            throw new TargetConnectionException(this, "Failed to authorize user in %s storage".formatted(id), e);
        }
    }

    // コードで認証する
    public void authorizeWithCode(String code, ModCommandSender sender) throws TargetConnectionException {
        try {
            this.credential = null;

            String cleanCode = code.trim();
            if (cleanCode.contains("code=")) {
                cleanCode = cleanCode.substring(cleanCode.indexOf("code=") + 5);
                if (cleanCode.contains("&")) cleanCode = cleanCode.substring(0, cleanCode.indexOf("&"));
                try { cleanCode = java.net.URLDecoder.decode(cleanCode, java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ignored) {}
            }
            cleanCode = cleanCode.trim();
            GoogleClientSecrets clientSecrets = loadClientSecrets();
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    NET_HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, DRIVE_SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(config.getTokenFolder()))
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();
            TokenResponse response = flow.newTokenRequest(cleanCode).setRedirectUri("http://localhost:8888/Callback").execute();
            this.credential = flow.createAndStoreCredential(response, id);
            if (this.credential == null) throw new TargetConnectionException(this, "Failed to authorize user in %s storage".formatted(id));
            Component header = Component.empty().append(Component.text("Account linking").decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD));
            Component message = Component.empty().append(Component.text("Account has been successfully linked to %s storage via code".formatted(id)).color(NamedTextColor.GREEN));
            UiHelper.sendFramedMessage(header, message, sender);
            MakeBackup.getInstance().getLogManager().log("Account linked via manual code for %s storage".formatted(id), sender);
        } catch (Exception e) {
            Component header = Component.empty().append(Component.text("Account linking"));
            Component message = Component.empty().append(Component.text("Failed to link with code for %s storage:".formatted(this.id)).color(NamedTextColor.RED));
            UiHelper.sendFramedMessage(header, message, sender);
            throw new TargetConnectionException(this, "Failed to authorize with code for %s storage".formatted(id), e);
        }
    }

    // サーバーIPを検出する
    private static String detectServerIp() {
        try {

            String ip = org.apache.http.client.fluent.Request.Get("https://api.ipify.org").connectTimeout(3000).socketTimeout(3000).execute().returnContent().asString().trim();
            if (ip != null && !ip.isEmpty() && ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+.*")) return ip;
        } catch (Exception ignored) {}
        try {

            String ip = java.net.InetAddress.getLocalHost().getHostAddress();
            if (ip != null && !ip.isEmpty() && !ip.equals("127.0.0.1")) return ip;
        } catch (Exception ignored) {}
        return "YOUR_SERVER_IP";
    }

    // 認証済みならクレデンシャルを返す
    Credential returnCredentialIfAuthorized() throws TargetConnectionException {
        try {
            boolean checkConnection = credential == null;

            GoogleClientSecrets clientSecrets = loadClientSecrets();

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    NET_HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, DRIVE_SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(config.getTokenFolder()))
                    .setApprovalPrompt("force")
                    .setAccessType("offline")
                    .build();

            credential = flow.loadCredential(id);

            if (credential != null
                    && (credential.getRefreshToken() != null
                    || credential.getExpiresInSeconds() == null
                    || credential.getExpiresInSeconds() > 60)) {

                if (checkConnection) {
                    try {
                        Drive service = new Drive.Builder(NET_HTTP_TRANSPORT, JSON_FACTORY, credential)
                                .setApplicationName(APPLICATION_NAME)
                                .setHttpRequestInitializer(httpRequest -> {
                                    credential.initialize(httpRequest);
                                    httpRequest.setConnectTimeout(300 * 60000);
                                    httpRequest.setReadTimeout(300 * 60000);
                                })
                                .build();

                        com.google.api.services.drive.model.File driveFile = service.files().get("").setSupportsAllDrives(true).execute();
                        driveFile.getName();

                    } catch (GoogleJsonResponseException e) {
                        if (e.getStatusCode() != 404) {
                            credential = null;
                            return null;
                        }
                    } catch (Exception e) {
                        credential = null;
                        return null;
                    }
                }

                return credential;
            }
            credential = null;
            return null;

        } catch (Exception e) {
            credential = null;
            throw new TargetConnectionException(this, "Failed to authorize user in Google Drive", e);
        }
    }

    @Override
    // IDを取得する
    public String getId() {
        return this.id;
    }

    @Override
    // タイプを取得する
    public TargetType getType() {
        return TargetType.GOOGLE_DRIVE;
    }

    @Override
    // 設定を取得する
    public GoogleDriveSettings getConfig() {
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
            return true;
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Not authorized in Google Drive", sender);
            MakeBackup.getInstance().getLogManager().warn(e);
            return false;
        }
    }

    // プロパティを追加する
    public void addProperty(String fileId, String key, String value) throws TargetQuotaExceededException, TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            Drive service = mainClient.getClient();
            logOperation("FILES.GET", "id=%s fields=appProperties".formatted(fileId));

            Map<String, String> appProperties = service.files().get(fileId).setSupportsAllDrives(true).setFields("appProperties").execute().getAppProperties();
            appProperties.put(key, value);
            logOperation("FILES.UPDATE", "id=%s fields=appProperties appProperty=%s".formatted(fileId, key));
            service.files().update(fileId, new com.google.api.services.drive.model.File()
                            .setAppProperties(appProperties)).setSupportsAllDrives(true)
                    .setFields("appProperties")
                    .execute();
            cacheLs.invalidateAll();
            return null;
        }).retry(retriableExceptionHandler);
    }

    @Override
    // 一覧を取得する
    public List<String> ls(String driveFileId) throws TargetQuotaExceededException {

        return ls(driveFileId, null).stream().map(com.google.api.services.drive.model.File::getName).distinct().toList();
    }

    // 一覧を取得する（クエリ付き）
    public List<com.google.api.services.drive.model.File> ls(String driveFileId, String query) throws TargetQuotaExceededException, TargetMethodException, TargetConnectionException {
        try {
            return cacheLs.get(Pair.of(driveFileId, query), () -> {
                return ((Retryable<List<com.google.api.services.drive.model.File>>) () -> {
                    String pageToken = null;
                    List<com.google.api.services.drive.model.File> driveFiles = new ArrayList<>();
                    Drive service = mainClient.getClient();

                    do {
                        logOperation("FILES.LIST", "parent=%s query=%s pageToken=%s".formatted(driveFileId, query, pageToken));
                        Drive.Files.List lsRequest = service.files().list().setSupportsAllDrives(true).setIncludeItemsFromAllDrives(true)
                                .setFields("nextPageToken, files(id, name)")
                                .setPageSize(1000)
                                .setPageToken(pageToken);
                        String q = "appProperties has { key='backuper' and value='true' }";
                        if (query != null) {
                            q = "%s and %s".formatted(q, query);
                        }

                        if (driveFileId != null && driveFileId.equals("drive")) {
                            lsRequest = lsRequest.setSpaces("drive");
                        }

                        if (driveFileId != null && !driveFileId.isEmpty() && !driveFileId.equals("drive")) {
                            q = "%s and '%s' in parents".formatted(q, driveFileId);
                        }
                        lsRequest = lsRequest.setQ(q);

                        FileList driveFileList = lsRequest.execute();

                        driveFiles.addAll(driveFileList.getFiles());

                        pageToken = driveFileList.getNextPageToken();

                    } while (pageToken != null);

                    return driveFiles;
                }).retry(retriableExceptionHandler);
            });
        } catch (ExecutionException e) {
            throw new TargetMethodException(this, "Execution exception on ls", e);
        }
    }

    @Override
    // パスを解決する
    public String resolve(String path, String fileName) {
        if (fileName.isEmpty()) return path;

        com.google.api.services.drive.model.File file = getFileByName(fileName, path);
        if (file == null) {
            return null;
        }
        return file.getId();
    }

    @Override
    // 存在を確認する
    public boolean exists(String path) throws TargetMethodException, TargetConnectionException {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return ((Retryable<Boolean>) () -> {
            Drive service = mainClient.getClient();
            try {
                logOperation("FILES.GET", "id=%s fields=id".formatted(path));
                service.files().get(path).setSupportsAllDrives(true)
                        .setFields("id")
                        .execute();
                return true;
            } catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() == 404) {
                    return false;
                }
                throw e;
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ディレクトリサイズを取得する
    public long getDirByteSize(String path) throws TargetQuotaExceededException, TargetMethodException, TargetConnectionException {
        return ((Retryable<Long>) () -> {
            if (isDir(path)) {
                long size = 0;

                List<String> files = ls(path);
                for (String file : files) {
                    size += getDirByteSize(getFileByName(file, path).getId());
                }
                return size;
            } else {
                Drive service = mainClient.getClient();
                logOperation("FILES.GET", "id=%s fields=size".formatted(path));

                com.google.api.services.drive.model.File driveFile = service.files().get(path).setSupportsAllDrives(true).setFields("size").execute();
                Long size = driveFile.getSize();
                return size != null ? size : 0;
            }
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ファイルか確認する
    public boolean isFile(String driveFileId) throws TargetMethodException, TargetConnectionException, TargetQuotaExceededException {
        return ((Retryable<Boolean>) () -> {
            Drive service = mainClient.getClient();
            logOperation("FILES.GET", "id=%s fields=mimeType".formatted(driveFileId));
            return !service.files().get(driveFileId).setSupportsAllDrives(true)
                    .setFields("mimeType")
                    .execute()
                    .getMimeType()
                    .equals(FOLDER_MIME_TYPE);
        }).retry(retriableExceptionHandler);
    }

    @Override
    // パスからファイル名を取得する
    public String getFileNameFromPath(String path) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<String>) () -> {
            logOperation("FILES.GET", "id=%s fields=name".formatted(path));
            return mainClient.getClient().files().get(path).setFields("name").execute().getName();
        }).retry(retriableExceptionHandler);

    }

    @Override
    // 親パスを取得する
    public String getParentPath(String path) throws TargetMethodException, TargetConnectionException {
        return ((Retryable<String>) () -> {
            logOperation("FILES.GET", "id=%s fields=parents".formatted(path));
            return mainClient.getClient().files().get(path).setFields("parents").execute().getParents().get(0);
        }).retry(retriableExceptionHandler);

    }

    // 名前でファイルを取得する
    public com.google.api.services.drive.model.File getFileByName(String fileName, String parentId) throws TargetQuotaExceededException, TargetMethodException, TargetConnectionException {
        return ((Retryable<com.google.api.services.drive.model.File>) () -> {
            Drive service = mainClient.getClient();

            String q = "";
            q += "name = '%s'".formatted(fileName);
            q += " and appProperties has { key='backuper' and value='true' }";

            Drive.Files.List lsRequest = service.files().list().setSupportsAllDrives(true).setIncludeItemsFromAllDrives(true);

            if (parentId != null && !parentId.isEmpty()) {
                q += " and '%s' in parents".formatted(parentId);
            }

            lsRequest.setQ(q);
            lsRequest.setFields("files(mimeType, size, name, id, parents, appProperties)");

            logOperation("FILES.LIST", "name=%s parent=%s".formatted(fileName, parentId));
            FileList driveFileList = lsRequest.execute();
            return !driveFileList.getFiles().isEmpty() ? driveFileList.getFiles().get(0) : null;
        }).retry(retriableExceptionHandler);
    }

    // ディレクトリを作成する
    public void createDir(String folderName, String parentFolderId) throws TargetQuotaExceededException, TargetLimitException, TargetMethodException, TargetConnectionException {
        createDir(folderName, parentFolderId, new HashMap<>());
    }

    // ディレクトリを作成する（プロパティ付き）
    public void createDir(String folderName, String parentFolderId, Map<String, String> properties) throws TargetQuotaExceededException, TargetLimitException, TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            Drive service = mainClient.getClient();

            properties.put("backuper", "true");

            com.google.api.services.drive.model.File driveFileMeta = new com.google.api.services.drive.model.File();
            driveFileMeta.setName(folderName);
            driveFileMeta.setAppProperties(properties);
            if (!Objects.equals(parentFolderId, "")) {
                driveFileMeta.setParents(List.of(parentFolderId));
            }
            driveFileMeta.setMimeType(FOLDER_MIME_TYPE);
            logOperation("FILES.CREATE", "folderName=%s parent=%s".formatted(folderName, parentFolderId));
            com.google.api.services.drive.model.File createdFile = service.files().create(driveFileMeta).setSupportsAllDrives(true)
                    .setFields("id, mimeType")
                    .execute();
            if (createdFile.getId() == null || !FOLDER_MIME_TYPE.equals(createdFile.getMimeType())) {
                throw new TargetMethodException(this, "Directory creation verification failed: %s".formatted(folderName));
            }
            cacheLs.invalidateAll();

            return null;
        }).retry(retriableExceptionHandler);
    }

    // ファイルをアップロードする
    public void uploadFile(InputStream sourceStream, String newFileName, String targetParentDir, TransferProgressListener progressListener) throws TargetMethodException, TargetConnectionException, TargetLimitException, TargetQuotaExceededException {
        ((Retryable<Void>) () -> {
            Drive service = mainClient.getClient();

            Map<String, String> fileAppProperties = new HashMap<>();
            fileAppProperties.put("backuper", "true");

            com.google.api.services.drive.model.File driveFileMeta = new com.google.api.services.drive.model.File();
            driveFileMeta.setAppProperties(fileAppProperties);
            driveFileMeta.setName(newFileName);
            if (!Objects.equals(targetParentDir, "")) {
                driveFileMeta.setParents(List.of(targetParentDir));
            }

            com.google.api.client.http.InputStreamContent contentStream = new InputStreamContent("", new TransferProgressInputStream(sourceStream, progressListener));
            contentStream.setCloseInputStream(false);
            Drive.Files.Create driveFileCreate = service.files()
                    .create(driveFileMeta, contentStream).setSupportsAllDrives(true)
                    .setUploadType("resumable")
                    .setFields("id, parents, appProperties");
            driveFileCreate.getMediaHttpUploader().setChunkSize(MediaHttpUploader.DEFAULT_CHUNK_SIZE);

            logOperation("FILES.CREATE", "fileName=%s parent=%s uploadType=resumable".formatted(newFileName, targetParentDir));

            com.google.api.services.drive.model.File createdFile = driveFileCreate.execute();
            if (createdFile.getId() == null || !exists(createdFile.getId())) {
                throw new TargetMethodException(this, "Upload verification failed: %s".formatted(newFileName));
            }
            cacheLs.invalidateAll();
            return null;
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ファイルをダウンロードする
    public InputStream downloadFile(String sourcePath, TransferProgressListener progressListener) throws TargetQuotaExceededException, TargetMethodException, TargetConnectionException {
        return ((Retryable<InputStream>) () -> {
            Drive service = mainClient.getClient();

            Drive.Files.Get getDriveFile = service.files().get(sourcePath).setSupportsAllDrives(true);
            getDriveFile.getMediaHttpDownloader().setChunkSize(MediaHttpDownloader.MAXIMUM_CHUNK_SIZE);
            logOperation("FILES.GET_MEDIA", "id=%s".formatted(sourcePath));

            return new TransferProgressInputStream(getDriveFile.executeMediaAsInputStream(), progressListener);
        }).retry(retriableExceptionHandler);
    }

    @Override
    // 削除する
    public void delete(String id) throws TargetQuotaExceededException, TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            Drive service = mainClient.getClient();
            logOperation("FILES.DELETE", "id=%s".formatted(id));
            service.files().delete(id).setSupportsAllDrives(true).execute();
            if (exists(id)) {
                throw new TargetMethodException(this, "Delete verification failed: %s".formatted(id));
            }
            cacheLs.invalidateAll();
            return null;
        }).retry(retriableExceptionHandler);
    }

    @Override
    // ファイル名を変更する
    public void renameFile(String fileId, String newFileName) throws TargetQuotaExceededException, TargetMethodException, TargetConnectionException {
        ((Retryable<Void>) () -> {
            Drive service = mainClient.getClient();

            logOperation("FILES.UPDATE", "id=%s name=%s".formatted(fileId, newFileName));
            com.google.api.services.drive.model.File renamedFile = service.files().update(fileId, new com.google.api.services.drive.model.File()
                            .setName(newFileName)).setSupportsAllDrives(true)
                    .setFields("id, name")
                    .execute();
            if (renamedFile.getId() == null || !newFileName.equals(renamedFile.getName())) {
                throw new TargetMethodException(this, "Rename verification failed for \"%s\" to \"%s\"".formatted(fileId, newFileName));
            }
            cacheLs.invalidateAll();
            return null;
        }).retry(retriableExceptionHandler);
    }

    @Override
    // 速度係数を取得する
    public int getStorageSpeedMultiplier() {
        return 15;
    }

    @Override
    // 破棄する
    public void destroy() {
        mainClient.disconnect();
        credential = null;
        if (protocolLogger != null) {
            protocolLogger.close();
        }
    }

    @Override
    // ダウンロード完了を処理する
    public void downloadCompleted() throws TargetMethodException, TargetConnectionException {

    }

    // 操作をログ出力する
    private void logOperation(String operation, String message) {
        if (protocolLogger != null) {
            protocolLogger.logOperation(operation, message);
        }
    }

    // 認証アプリクラス - 認証フローを管理する
    private static class MyAuthorizationCodeInstalledApp {

        private final Target storage;
        private final AuthorizationCodeFlow flow;
        private final String authServiceUrl;

        // コンストラクタ - 初期化する
        public MyAuthorizationCodeInstalledApp(Target storage, AuthorizationCodeFlow flow, String authServiceUrl) {
            this.storage = storage;
            this.flow = flow;
            this.authServiceUrl = authServiceUrl;
        }

        // 認証を通知する
        protected void onAuthorization(String id, ModCommandSender sender) {

            String url = "%s/authgd?id=%s".formatted(authServiceUrl, id);

            Component header = Component.empty()
                    .append(Component.text("Account linking"));

            Component message = Component.empty()
                    .append(Component.space())
                    .append(Component.text(url)
                            .clickEvent(ClickEvent.openUrl(url))
                            .decorate(TextDecoration.UNDERLINED));

            UiHelper.sendFramedMessage(header, message, sender);
        }

        // 認証を実行する
        public Credential authorize(String userId, boolean force, ModCommandSender sender) {

            if (!force) {
                try {

                    Credential credential = flow.loadCredential(userId);
                    if (credential != null
                            && (credential.getRefreshToken() != null
                            || credential.getExpiresInSeconds() == null
                            || credential.getExpiresInSeconds() > 60)) {
                        return credential;
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load Google Drive credentials", e);
                }
            }

            String id = userId + generateId();
            onAuthorization(id, sender);
            String response = null;

            int t = 0;
            try {
                while (t < 300) {

                    String result;

                    try {
                        result = Request.Get("%s/getgd?id=%s".formatted(authServiceUrl, id)).execute().returnContent().asString();
                    } catch (Exception e) {
                        throw new TargetConnectionException(storage, "Failed to connect to AuthGD or AuthGD is down." +
                                "Please let the developer know if you are sure that your network connection is ok", e);
                    }

                    if (!result.equals("null") && !result.equals("wrong")) {
                        response = result;
                        break;
                    }

                    Thread.sleep(1000);
                    t++;
                }
            } catch (Exception e) {
                throw new TargetConnectionException(storage, "Failed to get authGD server response", e);
            }
            if (t >= 300) {
                throw new TargetConnectionException(storage, "AuthGD response timeout");
            }

            Gson gson = new GsonBuilder().create();

            HashMap<String, Object> responseJson = gson.fromJson(response, HashMap.class);

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken((String) responseJson.get("access_token"));
            tokenResponse.setScope((String) responseJson.get("scope"));
            tokenResponse.setTokenType((String) responseJson.get("token_type"));
            tokenResponse.setTokenType((String) responseJson.get("token_type"));
            tokenResponse.setExpiresInSeconds(((Double) responseJson.get("expires_in")).longValue());
            tokenResponse.setRefreshToken((String) responseJson.get("refresh_token"));

            try {
                return flow.createAndStoreCredential(tokenResponse, userId);
            } catch (IOException e) {
                throw new TargetConnectionException(storage, "Failed to save Google Drive credentials", e);
            }
        }

        // IDを生成する
        private String generateId() {

            StringBuilder id = new StringBuilder();
            Random rand = new Random();
            for (int i = 0; i < 16; i++) {

                int r = rand.nextInt(0, 62);

                if (r < 10) {
                    id.append((char) ('0' + r));
                } else if (r - 10 < 26) {
                    id.append((char) ('A' + r - 10));
                } else {
                    id.append((char) ('a' + r - 36));
                }
            }
            return id.toString();
        }
    }

    // GoogleDrive進捗リスナークラス - 進捗を通知する
    private static class GoogleDriveStorageProgressListener implements MediaHttpUploaderProgressListener, MediaHttpDownloaderProgressListener {

        private final TransferProgressListener progressListener;
        long progress = 0;

        // コンストラクタ - リスナーで初期化する
        public GoogleDriveStorageProgressListener(TransferProgressListener progressListener) {
            this.progressListener = progressListener;
        }

        @Override
        // アップロード進捗を通知する
        public void progressChanged(MediaHttpUploader mediaHttpUploader) {
            progressListener.incrementProgress(mediaHttpUploader.getNumBytesUploaded() - progress);
            progress = mediaHttpUploader.getNumBytesUploaded();
        }

        @Override
        // ダウンロード進捗を通知する
        public void progressChanged(MediaHttpDownloader mediaHttpDownloader) {
            progressListener.incrementProgress(mediaHttpDownloader.getNumBytesDownloaded() - progress);
            progress = mediaHttpDownloader.getNumBytesDownloaded();
        }
    }
}
