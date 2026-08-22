/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.Snapshot;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.target.UserAuthTarget;
import com.sayuki.makebackup.command.runner.SnapshotCommand;
import com.sayuki.makebackup.command.drive.DriveLinkCommand;
import com.sayuki.makebackup.command.browse.BrowseCommand;
import com.sayuki.makebackup.command.manage.CopySnapshotCommand;
import com.sayuki.makebackup.command.manage.RemoveSnapshotCommand;
import com.sayuki.makebackup.command.manage.ManageMenuCommand;
import com.sayuki.makebackup.command.manage.ZipSnapshotCommand;
import com.sayuki.makebackup.command.manage.UnzipSnapshotCommand;
import com.sayuki.makebackup.command.system.ReloadSettingsCommand;
import com.sayuki.makebackup.command.system.CancelJobCommand;
import com.sayuki.makebackup.command.system.JobStatusCommand;
import com.sayuki.makebackup.platform.ModCommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// コマンドプロセッサクラス - コマンドツリーの登録と管理をする
public class CommandProcessor {

    private final List<ModCommandTree> trees = new ArrayList<>();
    private final ConcurrentHashMap<UUID, ConfirmableSubCommand> pendingConfirmations = new ConcurrentHashMap<>();

    // 初期化する - 各コマンドツリーを登録する
    public void init() {
        registerBackupCommandTree();
        registerListCommandTree();
        registerReloadCommandTree();
        registerMenuCommandTree();
        registerTaskCommandTree();
        registerAccountCommandTree();
        registerConfirmCommandTree();
    }

    // ツリーを取得する
    public List<ModCommandTree> getTrees() {
        return trees;
    }

    // 確認待ちを登録する
    public void registerConfirmation(UUID playerUuid, ConfirmableSubCommand command) {
        pendingConfirmations.put(playerUuid, command);
    }

    // 確認待ちを取り出す - 削除して返す
    public ConfirmableSubCommand popConfirmation(UUID playerUuid) {
        return pendingConfirmations.remove(playerUuid);
    }

    // バックアップコマンドツリーを登録する
    private void registerBackupCommandTree() {
        ModCommandTree backupCommandTree = new ModCommandTree("makebackup").withPermission(Permissions.BACKUPER.getPermission());
        backupCommandTree
                .then(ModCommandTree.literal("backup")
                        .then(ModCommandTree.string("storage").includeSuggestions(this::getMultiStorageSuggestion)

                                .executes((sender, args) -> {
                                    MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                        new SnapshotCommand(sender, args, "NOTHING").execute();
                                    });
                                })
                                .then(ModCommandTree.longArg("delaySeconds")
                                        .executes((sender, args) -> {
                                            MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                new SnapshotCommand(sender, args, "NOTHING").execute();
                                            });
                                        })
                                )
                                .then(ModCommandTree.literal("stop").withPermission(Permissions.STOP.getPermission())
                                        .executes((sender, args) -> {
                                            MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                new SnapshotCommand(sender, args, "STOP").execute();
                                            });
                                        })
                                        .then(ModCommandTree.longArg("delaySeconds")
                                                .executes((sender, args) -> {
                                                    MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                        new SnapshotCommand(sender, args, "STOP").execute();
                                                    });
                                                })
                                        )
                                )
                                .then(ModCommandTree.literal("restart").withPermission(Permissions.RESTART.getPermission())
                                        .executes((sender, args) -> {
                                            MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                new SnapshotCommand(sender, args, "RESTART").execute();
                                            });
                                        })
                                        .then(ModCommandTree.longArg("delaySeconds")
                                                .executes((sender, args) -> {
                                                    MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                        new SnapshotCommand(sender, args, "RESTART").execute();
                                                    });
                                                })
                                        )
                                )
                        )
                );
        trees.add(backupCommandTree);
    }

    // リストコマンドツリーを登録する
    private void registerListCommandTree() {
        ModCommandTree backupListCommandTree = new ModCommandTree("makebackup").withPermission(Permissions.BACKUPER.getPermission());
        backupListCommandTree
                .then(ModCommandTree.literal("list")
                        .then(ModCommandTree.string("storage")
                                .includeSuggestions(this::getSingleStorageSuggestion)

                                .executes((sender, args) -> {
                                    MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                        new BrowseCommand(true, sender, args).execute();
                                    });
                                })
                                .then(ModCommandTree.integer("pageNumber")
                                        .executes((sender, args) -> {
                                            MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                new BrowseCommand(true, sender, args).execute();
                                            });
                                        })
                                )
                        )
                );
        trees.add(backupListCommandTree);
    }

    // リロードコマンドツリーを登録する
    private void registerReloadCommandTree() {
        ModCommandTree backupReloadCommandTree = new ModCommandTree("makebackup").withPermission(Permissions.BACKUPER.getPermission());
        backupReloadCommandTree
                .then(ModCommandTree.literal("reload").withPermission(Permissions.CONFIG_RELOAD.getPermission())
                        .executes((sender, args) -> {
                            MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                new ReloadSettingsCommand(sender, args).execute();
                            });
                        })
                );
        trees.add(backupReloadCommandTree);
    }

    // メニューコマンドツリーを登録する
    private void registerMenuCommandTree() {
        ModCommandTree backupMenuCommandTree = new ModCommandTree("makebackup").withPermission(Permissions.BACKUPER.getPermission());
        backupMenuCommandTree
                .then(ModCommandTree.literal("menu")
                        .then(ModCommandTree.string("storage").includeSuggestions(this::getSingleStorageSuggestion)

                                .then(ModCommandTree.literal("copyto")
                                        .then(ModCommandTree.string("targetStorage").includeSuggestions((suggestionInfo) -> MakeBackup.getInstance().getStorageManager().getStorages().stream()
                                                        .filter(storage -> suggestionInfo.sender().hasPermission(Permissions.STORAGE.getPermission(storage)))
                                                        .map(Target::getId)
                                                        .filter(id -> !id.equals(suggestionInfo.previousArgs().get("storage"))).toList())
                                                .then(ModCommandTree.text("backupName").includeSuggestions(this::getBackupNameSuggestions)
                                                        .executes((sender, args) -> {
                                                            MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                                new CopySnapshotCommand(sender, args).execute();
                                                            });
                                                        })
                                                )
                                        )
                                )
                                .then(ModCommandTree.literal("delete")
                                        .then(ModCommandTree.text("backupName").includeSuggestions(this::getBackupNameSuggestions)
                                                .executes((sender, args) -> {
                                                    MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                        new RemoveSnapshotCommand(sender, args).execute();
                                                    });
                                                })
                                        )
                                )
                                .then(ModCommandTree.literal("unzip")
                                        .then(ModCommandTree.text("backupName").includeSuggestions(this::getBackupNameSuggestions)
                                                .executes((sender, args) -> {
                                                    MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                        new UnzipSnapshotCommand(sender, args).execute();
                                                    });
                                                })
                                        )
                                )
                                .then(ModCommandTree.literal("tozip")
                                        .then(ModCommandTree.text("backupName").includeSuggestions(this::getBackupNameSuggestions)
                                                .executes((sender, args) -> {
                                                    MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                        new ZipSnapshotCommand(sender, args).execute();
                                                    });
                                                })
                                        )
                                )
                                .then(ModCommandTree.text("backupName").includeSuggestions(this::getBackupNameSuggestions)
                                        .executes((sender, args) -> {
                                            MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                new ManageMenuCommand(sender, args).execute();
                                            });
                                        })
                                )
                        )
                );
        trees.add(backupMenuCommandTree);
    }

    // バックアップ名サジェストを取得する
    private List<String> getBackupNameSuggestions(ModCommandTree.SuggestionContext context) {
        Target storage = MakeBackup.getInstance().getStorageManager().getStorage((String) context.previousArgs().get("storage"));
        if (storage == null || !context.sender().hasPermission(Permissions.STORAGE.getPermission(storage))) return new ArrayList<>();

        return storage.getBackupManager().getBackupList().stream()
                .sorted(Snapshot::compareTo)
                .map(Snapshot::getName)
                .toList();
    }

    // タスクコマンドツリーを登録する
    private void registerTaskCommandTree() {
        ModCommandTree backupTaskCommandTree = new ModCommandTree("makebackup").withPermission(Permissions.BACKUPER.getPermission());
        backupTaskCommandTree
                .then(ModCommandTree.literal("task")
                        .then(ModCommandTree.literal("cancel")
                                .executes((sender, args) -> {
                                    MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                        new CancelJobCommand(sender, args).execute();
                                    });
                                })
                        )
                        .then(ModCommandTree.literal("status").withPermission(Permissions.STATUS.getPermission())
                                .executes((sender, args) -> {
                                    MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                        new JobStatusCommand(sender, args).execute();
                                    });
                                })
                        )
                );
        trees.add(backupTaskCommandTree);
    }

    // アカウントコマンドツリーを登録する
    private void registerAccountCommandTree() {
        ModCommandTree backupAccountCommandTree = new ModCommandTree("makebackup").withPermission(Permissions.BACKUPER.getPermission());
        backupAccountCommandTree
                .then(ModCommandTree.literal("account")
                        .then(ModCommandTree.string("storage")
                                .includeSuggestions((context) -> MakeBackup.getInstance().getStorageManager().getStorages().stream()
                                        .filter(storage -> context.sender().hasPermission(Permissions.ACCOUNT.getPermission(storage)))
                                        .filter(storage -> storage instanceof UserAuthTarget)
                                        .map(Target::getId)
                                        .toList())
                                .then(ModCommandTree.literal("link")
                                        .executes((sender, args) -> {
                                            MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                new DriveLinkCommand(sender, args).execute();
                                            });
                                        })
                                        .then(ModCommandTree.text("code")
                                                .executes((sender, args) -> {
                                                    MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                                                        new DriveLinkCommand(sender, args).execute();
                                                    });
                                                })
                                        )
                                )
                        )
                );
        trees.add(backupAccountCommandTree);
    }

    // 確認コマンドツリーを登録する
    private void registerConfirmCommandTree() {
        ModCommandTree confirmCommandTree = new ModCommandTree("makebackup").withPermission(Permissions.BACKUPER.getPermission());
        confirmCommandTree
                .then(ModCommandTree.literal("confirm")
                        .executes((sender, args) -> {
                            if (!sender.isPlayer()) return;
                            ConfirmableSubCommand command = popConfirmation(sender.asPlayer().getUniqueId());
                            if (command != null) {
                                command.execute();
                            }
                        })
                );
        trees.add(confirmCommandTree);
    }

    // 単一ストレージのサジェストを取得する
    private List<String> getSingleStorageSuggestion(ModCommandTree.SuggestionContext suggestionInfo) {
        return MakeBackup.getInstance().getStorageManager().getStorages().stream()
                .filter(storage -> suggestionInfo.sender().hasPermission(Permissions.STORAGE.getPermission(storage)))
                .map(Target::getId).toList();
    }

    // 複数ストレージのサジェストを取得する
    private List<String> getMultiStorageSuggestion(ModCommandTree.SuggestionContext suggestionInfo) {
        return MakeBackup.getInstance().getStorageManager().getStorages().stream()
                .filter(storage -> suggestionInfo.sender().hasPermission(Permissions.BACKUP.getPermission(storage)))
                .filter(storage -> {
                    String lastStorageString = suggestionInfo.currentArg().substring(!suggestionInfo.currentArg().contains("-") ? 0 : suggestionInfo.currentArg().lastIndexOf("-") + 1);
                    if (MakeBackup.getInstance().getStorageManager().getStorage(lastStorageString) != null) return true;
                    return storage.getId().startsWith(lastStorageString);
                })
                .filter(storage -> Arrays.stream(suggestionInfo.currentArg().split("-")).noneMatch(currentArgumentStorage -> currentArgumentStorage.equals(storage.getId())))
                .map(Target::getId)
                .map(id -> {

                    String currentArg = suggestionInfo.currentArg();
                    String lastStorageString = suggestionInfo.currentArg().substring(!suggestionInfo.currentArg().contains("-") ? 0 : suggestionInfo.currentArg().lastIndexOf("-") + 1);
                    if (MakeBackup.getInstance().getStorageManager().getStorage(lastStorageString) != null) currentArg += "-";

                    int lastIndex = currentArg.lastIndexOf("-") == -1 ? 0 : currentArg.lastIndexOf("-") + 1;
                    return "%s%s".formatted(currentArg.substring(0, lastIndex), id);
                })
                .toList();
    }
}
