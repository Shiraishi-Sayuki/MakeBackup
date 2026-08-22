/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core;

import com.sayuki.makebackup.MakeBackup;
import org.quartz.*;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// スケジューラサービス - タスクのスケジュール実行を管理する
public class SchedulerService {

    private org.quartz.Scheduler quartzScheduler;
    private ExecutorService mainExecutorService;
    private ScheduledExecutorService mainScheduler;

    // 初期化する - Quartzとスレッドプールをセットアップする
    public void init() {
        try {
            if (DirectSchedulerFactory.getInstance().getAllSchedulers().stream().noneMatch(scheduler -> {
                try {
                    return scheduler.getSchedulerName().equals("makebackup");
                } catch (SchedulerException e) {
                    throw new RuntimeException(e);
                }
            })) {
                DirectSchedulerFactory.getInstance().createScheduler("makebackup", "main", new SimpleThreadPool(1, 5), new RAMJobStore());
            }

            this.quartzScheduler = DirectSchedulerFactory.getInstance().getScheduler("makebackup");
            this.quartzScheduler.start();
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to initialize Quartz Scheduler, automatic backups will not work");
            MakeBackup.getInstance().getLogManager().warn(e);
        }

        if (MakeBackup.getInstance().getConfigManager().getServerConfig().getThreadNumber() == 0) {
            this.mainExecutorService = Executors.newWorkStealingPool();
            this.mainScheduler = Executors.newSingleThreadScheduledExecutor();
        } else {
            this.mainExecutorService = Executors.newWorkStealingPool(MakeBackup.getInstance().getConfigManager().getServerConfig().getThreadNumber());
            this.mainScheduler = Executors.newSingleThreadScheduledExecutor();
        }
    }

    // 遅延実行する - 指定tick後にタスクを実行する
    public void runGlobalRegionDelayed(Runnable task, long delayTicks) {
        try {
            mainScheduler.schedule(withModContextClassLoader(task), delayTicks * 50L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to schedule delayed task");
            MakeBackup.getInstance().getLogManager().warn(e);
        }
    }

    // 繰り返し実行する - 定期的にタスクを実行する
    public void runGlobalRegionRepeatingTask(Runnable task, long delayTicks, long periodTicks) {
        try {
            mainScheduler.scheduleAtFixedRate(withModContextClassLoader(task), delayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to schedule repeating task");
            MakeBackup.getInstance().getLogManager().warn(e);
        }
    }

    // 非同期で実行する
    public CompletableFuture<Void> runAsync(Runnable task) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(withModContextClassLoader(task), mainExecutorService);
        future.exceptionally(e -> {
            MakeBackup.getInstance().getLogManager().warn("An error occurred in an async task");
            MakeBackup.getInstance().getLogManager().warn("%s\n%s".formatted(e, Arrays.toString(e.getStackTrace())));
            return null;
        });
        return future;
    }

    // MODクラスローダーでラップする - コンテキストを保持する
    private static Runnable withModContextClassLoader(Runnable task) {
        return () -> {
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(MakeBackup.class.getClassLoader());
            try {
                task.run();
            } finally {
                Thread.currentThread().setContextClassLoader(previous);
            }
        };
    }

    // 破棄する - スケジューラをシャットダウンする
    public void destroy() {
        try {
            this.mainScheduler.shutdownNow();
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to cancel scheduler tasks");
            MakeBackup.getInstance().getLogManager().warn(e);
        }
        try {
            this.quartzScheduler.shutdown(false);
        } catch (SchedulerException e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to shutdown Quartz Scheduler");
            MakeBackup.getInstance().getLogManager().warn(e);
        }
        this.mainExecutorService.shutdownNow();
    }

    // Cronで定期実行する - Cron式でジョブを登録する
    public CronTrigger runCronScheduledJob(Class<? extends Job> job, String jobName, String jobGroup, CronExpression cronExpression) {
        try {

            JobDetail jobDetail = JobBuilder.newJob(job).withIdentity(jobName, jobGroup).build();
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobName, jobGroup)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .forJob(jobDetail)
                    .build();
            quartzScheduler.scheduleJob(jobDetail, trigger);
            return trigger;

        } catch (SchedulerException e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to run Cron Scheduled Job");
            MakeBackup.getInstance().getLogManager().warn(e);
            return null;
        }
    }
}
