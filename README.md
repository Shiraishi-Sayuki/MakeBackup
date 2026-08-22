# MakeBackup

FTP / SFTP / Google Drive 対応の Minecraft サーバー向けシンプルバックアップ MOD

[Backuper](https://github.com/DVDishka/Backuper) (Paper) をベースにパッケージを全面再構成（`core` / `command`）+ ローカル認証フロー化した多ローダー版。

## 特徴

- 保存先: `ローカル` / `FTP` / `SFTP` / `Google Drive`（共有ドライブ対応）
- 自動バックアップ: 周期（分） or Cron式 (`backup.autoBackupCron`)
- 手動バックアップ、ZIP 圧縮レベル 0-9、最大数/容量制限
- 追加フォルダ指定: `backup.addDirectoryToBackup: ["./mods","./config"]` ※ワールドは自動で入るから `world` は書かない
- 除外フォルダ: `excludeDirectoryFromBackup`, バックアップ先は自動除外
- ログ: `MakeBackup/logs/{storageId}.log`（`MakeBackup/Backups/../logs/`）
- Google Drive は `http://localhost:8888/Callback` のローカルフロー（`core/target/GoogleDriveTarget.java:143`）- `ssh -L 8888:localhost:8888 user@<IP>` は自動で表示される

## 設定例

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

## コマンド

- `/makebackup backup <storage>` - 即時バックアップ
- `/makebackup list <storage>` - 一覧
- `/makebackup menu` - GUIでコピー/削除/圧縮/解凍
- `/makebackup account link <gdrive>` - Google 認証（ローカル 8888 ポートが開く）
- `/makebackup task status|query|cancel` - ジョブ管理
- `/makebackup reload` - 設定再読み込み

権限: `makebackup`, `makebackup.<storage>.backup`, `makebackup.<storage>.account` など (`command/Permissions.java:1`)

## ライセンス

ARR - All Rights Reserved. [LICENSE](LICENSE) 参照。無断複製・配布禁止。

作者: Shiraishi Sayuki
