package org.lastrix.collagemaker.app;

import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lastrix on 8/21/14.
 */
public class AppScheduler {
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private static final Scheduler SCHEDULER = Schedulers.from(EXECUTOR_SERVICE);

    public static ExecutorService getExecutorService() {
        return EXECUTOR_SERVICE;
    }

    public static Scheduler getScheduler() {
        return SCHEDULER;
    }
}
