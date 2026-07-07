package bili.dongsz.broadcastradio.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池管理器
 */
public class RadioThreadPoolManager {

    private static RadioThreadPoolManager instance;

    private final ScheduledExecutorService scheduler;

    /** 追踪所有已提交的定时任务，便于统一清理 */
    private final java.util.List<ScheduledFuture<?>> scheduledTasks = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** 已提交的线程数，用于生成唯一的线程名称 */
    private final AtomicInteger threadCounter = new AtomicInteger(0);

    private RadioThreadPoolManager() {
        this.scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "RadioPool-" + threadCounter.incrementAndGet());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1); // 略低优先级，避免抢占渲染
                return thread;
            }
        });
    }

    /** 单例获取 */
    public static synchronized RadioThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new RadioThreadPoolManager();
        }
        return instance;
    }

    /**
     * 提交一个定时任务。
     *
     * @param task          要执行的 Runnable（内部任务应在后台线程执行，仅在必要时回主线程）
     * @param initialDelay  初始延迟（毫秒）
     * @param period        执行周期（毫秒）
     * @return              ScheduledFuture 引用，调用者可用于取消单个任务
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period) {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
        scheduledTasks.add(future);
        return future;
    }

    /**
     * 提交一个单次延迟任务。
     *
     * @param task    要执行的 Runnable
     * @param delay   延迟（毫秒）
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay) {
        return scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止单个定时任务（不关闭线程池，可继续提交新任务）。
     */
    public void cancelTask(ScheduledFuture<?> future) {
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
            scheduledTasks.remove(future);
        }
    }

    /**
     * 停止所有定时任务，但保留线程池实例（用于进入无信号状态等场景）。
     */
    public void cancelAllTasks() {
        for (ScheduledFuture<?> future : scheduledTasks) {
            if (future != null) {
                future.cancel(false);
            }
        }
        scheduledTasks.clear();
    }

    /**
     * 模组卸载时的清理 —— 停止所有任务并关闭线程池。
     * 这是最终清理，调用后线程池不再可用。
     */
    public void shutdown() {
        cancelAllTasks();
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** 当前活跃任务数（用于调试） */
    public int getActiveTaskCount() {
        int count = 0;
        for (ScheduledFuture<?> future : scheduledTasks) {
            if (!future.isDone() && !future.isCancelled()) {
                count++;
            }
        }
        return count;
    }
}