/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.helper.Helper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// デフォルトコンフィグクラス - 初期設定値を生成する
public class DefaultConfigs {

    public static final double CONFIG_VERSION = 14.0;

    // コンフィグディレクトリパスを取得する
    static String configDirPath() {
        MakeBackup makeBackup = MakeBackup.getInstance();
        return makeBackup != null ? makeBackup.getModDir().getPath() : "./config/makebackup";
    }

    // バックアップフォルダパスを取得する
    static String backupsFolderPath() {
        MakeBackup makeBackup = MakeBackup.getInstance();
        if (makeBackup != null) {
            return makeBackup.getModDir().getParentFile().getParentFile().toPath()
                    .resolve("MakeBackup").resolve("Backups").toString();
        }
        return "MakeBackup/Backups";
    }

    // マスターコンフィグを作成する
    public static ConfigSection masterConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_comment", List.of(
                "DO NOT CHANGE",
                "DO NOT CHANGE"
        ));
        data.put("configVersion", CONFIG_VERSION);
        data.put("lastBackup", 0L);
        data.put("lastChange", 0L);
        data.put("backup", backupConfig().getData());
        data.put("server", serverConfig().getData());

        Map<String, Object> storages = new LinkedHashMap<>();
        storages.put("local", localConfig().getData());
        storages.put("googleDrive", googleDriveConfig().getData());
        storages.put("ftp", ftpConfig().getData());
        storages.put("sftp", sftpConfig().getData());
        data.put("storages", storages);

        return new ConfigSection(data, null);
    }

    // バックアップコンフィグを作成する
    public static ConfigSection backupConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_comment", List.of(
                "(true/false) Automatic backup once in a specified period",
                "(MINUTES > 0 or -1) Snapshot period. To use this value, you need to set autoBackupCron to an empty string and autoBackup to true",
                "(Expression) Auto-backup schedule expression. If it's defined, it'll be used instead of autoBackupPeriod. Use this site to generate your one: http://www.cronmaker.com",
                "(Expression) Set a format for backup file names (see java.time docs). It must contain information about both time and date",
                "(Path list) Additional folders/files to backup - WORLDS ARE AUTO-BACKED UP, DO NOT add 'world' here",
                "  Use relative path from server jar (e.g. './mods', './config', './kubejs', './local') or absolute path (e.g. '/home/server/mods' or 'C:/server/mods')",
                "  Use forward slashes '/' even on Windows. Check existence with 'ls -la <path>' from server dir. Example:",
                "  addDirectoryToBackup: ['./mods', './config', './kubejs']",
                "  Use ['*'] to backup everything in server root (excluding backup dest and excluded list)",
                "(Path list) Folders/files to EXCLUDE from backup. If you added 'folder1' above but want to skip 'folder1/file1', list it here. Backup destination is always excluded.",
                "  excludeDirectoryFromBackup: ['./mods/cache', './logs']",
                "(true/false) Sometimes errors may occur while creating a backup. When this option is enabled, such backups will be deleted",
                "(true/false) The backup will only occur if the world has been changed since the last backup. If the world has not been changed, this backup cycle will be skipped",
                "(STOP, RESTART, NOTHING) What to do after an automatic backup",
                "(true/false) (True recommended) MakeBackup will mark all world folders as Read-Only to prevent folder changing that may cause the backup crash. True value may cause access denied errors during the backup (you should just ignore that)"
        ));
        data.put("autoBackup", true);
        data.put("autoBackupPeriod", 1440L);
        data.put("autoBackupCron", "");
        data.put("backupFileNameFormat", "dd-MM-yyyy HH-mm-ss");
        data.put("addDirectoryToBackup", List.of());
        data.put("excludeDirectoryFromBackup", List.of());
        data.put("deleteBrokenBackups", true);
        data.put("skipDuplicateBackup", true);
        data.put("afterBackup", "NOTHING");
        data.put("setWorldsReadOnly", false);
        return new ConfigSection(data, "backup");
    }

    // サーバーコンフィグを作成する
    public static ConfigSection serverConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_comment", List.of(
                "(SECONDS < backupPeriod * 60 or -1) A notification about the server restart will be sent to all players on the server {alertTimeBeforeRestart} seconds before the restart. -1 to disable notifications",
                "(true/false) Notifications will be sent only if the server is restarted or stopped after the backup",
                "(String) Snapshot alert message. (%d is a number of seconds placeholder)",
                "(String) Snapshot and restart the alert message. (%d is a number of seconds placeholder)",
                "(Path) Size cache file",
                "(>= 0) Target number of threads that MakeBackup will use in parallel (It'll try, but it isn't a hard limitation) (0 to automatically set it equal to the number of threads on the server)",
                "(true/false) Better logging (Some statistic and other information for debugging, you probably don't need it)"
        ));
        data.put("alertTimeBeforeRestart", 60L);
        data.put("alertOnlyServerRestart", true);
        data.put("alertBackupMessage", "Server will be backed up in %d second(s)");
        data.put("alertBackupRestartMessage", "Server will be backed up and restarted in %d second(s)");
        data.put("sizeCacheFile", configDirPath() + "/sizeCache.json");
        data.put("threadNumber", 0);
        data.put("betterLogging", false);
        return new ConfigSection(data, "server");
    }

    // ローカルコンフィグを作成する
    public static ConfigSection localConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_comment", List.of(
                "(true/false) Enable local storage to use it via MakeBackup",
                "(true/false) With automatic backup, backups will be saved to specified local storage. Works only if 'enabled: true'",
                "(Path) Full directory where backups will be stored",
                "(>= 0) Max backups in backups folder, 0 to make it unlimited",
                "(MB >= 0) Max backups folder weight, 0 to make it unlimited",
                "(true/false) Should backups be packaged in a zip archive",
                "(0 - 9) archive compression level. A higher value may reduce file size but may also increase the time required to archive and decompress"
        ));
        data.put("type", "local");
        data.put("enabled", true);
        data.put("autoBackup", true);
        data.put("backupsFolder", backupsFolderPath());
        data.put("maxBackupsNumber", 0);
        data.put("maxBackupsWeight", 0);
        data.put("zipArchive", true);
        data.put("zipCompressionLevel", 5);

        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("_comment", List.of("(true/false) Write local storage operations to logs/{storageId}.log"));
        debug.put("protocolLogging", true);
        data.put("debug", debug);
        return new ConfigSection(data, "local");
    }

    // FTPコンフィグを作成する
    public static ConfigSection ftpConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_comment", List.of(
                "(true/false) Enable FTP storage to use it via MakeBackup",
                "(true/false) With automatic backup, backups will be saved to a specified FTP server. Works only if 'enabled: true'",
                "(Path) FTP server directory where backups will be stored",
                "(Symbol) Path separator symbol used on FTP SERVER. For example, '/' on UNIX systems. (It is usually '/' even on Windows servers, so change it to '\\' only if it does not work with '/')",
                "(>= 0) Max backups in backups folder, 0 to make it unlimited",
                "(MB >= 0) Max backups folder weight, 0 to make it unlimited",
                "(true/false) Should backups be packaged in a zip archive",
                "(0 - 9) archive compression level. A higher value may reduce file size but may also increase the time required to archive and decompress"
        ));
        data.put("type", "ftp");
        data.put("enabled", false);
        data.put("autoBackup", true);
        data.put("backupsFolder", "./");
        data.put("pathSeparatorSymbol", Helper.isWindows ? "\\" : "/");
        data.put("maxBackupsNumber", 0);
        data.put("maxBackupsWeight", 0);
        data.put("zipArchive", true);
        data.put("zipCompressionLevel", 5);

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("_comment", List.of(
                "(Address) FTP server address",
                "(Port) FTP server port",
                "(Username) FTP server username to use for authentication",
                "(Password) FTP server password to use for authentication"
        ));
        auth.put("address", "");
        auth.put("port", 21);
        auth.put("username", "");
        auth.put("password", "");
        data.put("auth", auth);

        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("_comment", List.of("(true/false) Write FTP protocol commands and replies to logs/{storageId}.log"));
        debug.put("protocolLogging", true);
        data.put("debug", debug);
        return new ConfigSection(data, "ftp");
    }

    // SFTPコンフィグを作成する
    public static ConfigSection sftpConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_comment", List.of(
                "(true/false) Enable SFTP storage to use it via MakeBackup",
                "(true/false) With automatic backup, backups will be saved to a specified SFTP server. Works only if 'enabled: true'",
                "(Path) SFTP server directory where backups will be stored",
                "(Symbol) Path separator symbol used on SFTP SERVER. For example, '/' on UNIX systems and '\\' on windows",
                "(>= 0) Max backups in backups folder, 0 to make it unlimited",
                "(MB >= 0) Max backups folder weight, 0 to make it unlimited",
                "(true/false) Should backups be packaged in a zip archive",
                "(0 - 9) archive compression level. A higher value may reduce file size but may also increase the time required to archive and decompress"
        ));
        data.put("type", "sftp");
        data.put("enabled", false);
        data.put("autoBackup", true);
        data.put("backupsFolder", "./");
        data.put("pathSeparatorSymbol", Helper.isWindows ? "\\" : "/");
        data.put("maxBackupsNumber", 0);
        data.put("maxBackupsWeight", 0);
        data.put("zipArchive", true);
        data.put("zipCompressionLevel", 5);

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("_comment", List.of(
                "(Address) SFTP server address",
                "(Port) SFTP server port",
                "(password/key) SFTP server authentication type",
                "(Username) SFTP server username to use for authentication",
                "(Password) SFTP server password to use for authentication",
                "(Absolute Path) Local path to key file if 'authType: key'",
                "(true/false) Do you want to specify local knownHostsFile?",
                "(Path) Path to local knownHostsFile if 'useKnownHostsFile: true'",
                "(Path)"
        ));
        auth.put("address", "");
        auth.put("port", 22);
        auth.put("authType", "password");
        auth.put("username", "");
        auth.put("password", "");
        auth.put("keyFilePath", "");
        auth.put("useKnownHostsFile", false);
        auth.put("knownHostsFilePath", "");
        auth.put("sshConfigFilePath", "");
        data.put("auth", auth);

        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("_comment", List.of("(true/false) Write SFTP operations and JSch debug messages to logs/{storageId}.log"));
        debug.put("protocolLogging", true);
        data.put("debug", debug);
        return new ConfigSection(data, "sftp");
    }

    // GoogleDriveコンフィグを作成する
    public static ConfigSection googleDriveConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_comment", List.of(
                "(true/false) Enable Google Drive storage to use it via MakeBackup",
                "(true/false) With automatic backup, backups will be saved to specified Google Drive. Works only if 'enabled: true'",
                "Google Drive folder ID where backups will be stored",
                "Do you want MakeBackup to create its own folder in the specified in `backupsFolderId` directory to store backups there?",
                "(>= 0) Max backups in backups folder, 0 to make it unlimited",
                "(MB >= 0) Max backups folder weight, 0 to make it unlimited",
                "(true/false) Should backups be packaged in a zip archive",
                "(0 - 9) archive compression level. A higher value may reduce file size but may also increase the time required to archive and decompress"
        ));
        data.put("type", "googleDrive");
        data.put("enabled", true);
        data.put("autoBackup", true);
        data.put("backupsFolderId", "");
        data.put("createBackuperFolder", true);
        data.put("maxBackupsNumber", 0);
        data.put("maxBackupsWeight", 0);
        data.put("zipArchive", true);
        data.put("zipCompressionLevel", 5);

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("_comment", List.of("Directory where you want to store your Google authentication tokens"));
        auth.put("authServiceUrl", "https://auth.backuper-mc.com");
        auth.put("tokenFolderPath", configDirPath() + "/GoogleDrive/tokens");
        data.put("auth", auth);

        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("_comment", List.of("(true/false) Write Google Drive API operations to logs/{storageId}.log"));
        debug.put("protocolLogging", true);
        data.put("debug", debug);
        return new ConfigSection(data, "googleDrive");
    }
}
