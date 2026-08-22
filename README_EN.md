# MakeBackup

Simple backup mod for Minecraft servers with FTP / SFTP / Google Drive support.

Restructured from [Backuper](https://github.com/DVDishka/Backuper) (Paper) - packages `core` / `command` and local auth flow without external VPS.

## Features

- Storages: `local` / `ftp` / `sftp` / `googleDrive` (Shared Drive supported)
- Auto backup: period (minutes) or Cron (`backup.autoBackupCron`)
- Manual backup, ZIP level 0-9, max count/size limits
- Extra folders: `backup.addDirectoryToBackup: ["./mods","./config"]` - worlds are auto-added, don't add `world`
- Exclude: `excludeDirectoryFromBackup`, backup destination auto-excluded
- Logs: `MakeBackup/logs/{storageId}.log` (`MakeBackup/Backups/../logs/`)
- Google Drive uses `http://localhost:8888/Callback` local flow (`core/target/GoogleDriveTarget.java:143`) - tunnel `ssh -L 8888:localhost:8888 user@<IP>` is shown automatically

## Config Example

```json
"backup": {
  "autoBackup": true,
  "autoBackupPeriod": 1440,
  "autoBackupCron": "",
  "addDirectoryToBackup": ["./mods", "./config", "./kubejs"],
  "excludeDirectoryFromBackup": ["./mods/cache"],
  "afterBackup": "NOTHING"
},
"storages": {
  "local": { "type": "local", "enabled": true, "autoBackup": true, "backupsFolder": "./MakeBackup/Backups", "zipArchive": true },
  "googleDrive": { "type": "googleDrive", "enabled": false, "backupsFolderId": "", "auth": { "tokenFolderPath": "./config/makebackup/GoogleDrive/tokens" } }
}
```

## Commands

- `/makebackup backup <storage>` - immediate backup
- `/makebackup list <storage>` - list snapshots
- `/makebackup menu` - manage (copy/delete/zip/unzip)
- `/makebackup account link <gdrive>` - Google auth (listens on 8888)
- `/makebackup task status|query|cancel` - job control
- `/makebackup reload` - reload config

Permissions: `makebackup`, `makebackup.<storage>.backup`, `makebackup.<storage>.account` etc. (`command/Permissions.java:1`)

## License

ARR - All Rights Reserved. See [LICENSE](LICENSE). No reproduction/distribution without permission.

Author: Shiraishi Sayuki
