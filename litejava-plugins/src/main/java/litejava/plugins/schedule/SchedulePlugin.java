package litejava.plugins.schedule;

import litejava.Plugin;
import litejava.plugin.ClassScanner;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * 定时任务插件 - 基于 Quartz 实现
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * scheduler:
 *   packages: example.controller
 * </pre>
 * 
 * <h2>方式一：注解</h2>
 * <pre>{@code
 * @Scheduled("0 * * * * ?")
 * public void statsTask() { ... }
 * }</pre>
 * 
 * <h2>方式二：编程式</h2>
 * <pre>{@code
 * schedule.cron("0/5 * * * * ?", () -> System.out.println("每5秒"));
 * }</pre>
 */
public class SchedulePlugin extends Plugin {
    
    public Scheduler scheduler;
    
    /** 扫描包（逗号分隔） */
    public String packages;
    
    /** 实例提供者（用于获取类实例） */
    public Function<Class<?>, Object> instanceProvider;
    
    private int jobCounter = 0;
    private int registeredCount = 0;
    
    @Override
    public void config() {
        packages = app.conf.getString("scheduler", "packages", packages);
        
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to start scheduler", e);
        }
        
        // 在服务器启动后扫描 @Scheduled 注解（确保 DI 已初始化）
        if (packages != null && !packages.isEmpty()) {
            app.onStarted(this::scanScheduledMethods);
        }
    }
    
    private void scanScheduledMethods() {
        ClassScanner scanner = ClassScanner.getInstance();
        
        for (String pkg : packages.split(",")) {
            String trimmed = pkg.trim();
            if (!trimmed.isEmpty()) {
                for (Class<?> clazz : scanner.scan(trimmed)) {
                    registerScheduledMethods(clazz);
                }
            }
        }
        
        if (registeredCount > 0) {
            app.log.info("SchedulePlugin: " + registeredCount + " scheduled task(s) registered");
        }
    }
    
    private void registerScheduledMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            Scheduled scheduled = method.getAnnotation(Scheduled.class);
            if (scheduled != null) {
                String cronExpr = scheduled.value();
                if (!cronExpr.isEmpty()) {
                    Object instance = getInstance(clazz);
                    if (instance != null) {
                        cron(cronExpr, () -> {
                            try {
                                method.setAccessible(true);
                                method.invoke(instance);
                            } catch (Exception e) {
                                app.log.error("Scheduled task failed: " + clazz.getSimpleName() + "." + method.getName() + " - " + e.getMessage());
                            }
                        });
                        registeredCount++;
                        app.log.info("  -> " + clazz.getSimpleName() + "." + method.getName() + "() [" + cronExpr + "]");
                    }
                }
            }
        }
    }
    
    private Object getInstance(Class<?> clazz) {
        if (instanceProvider != null) {
            try {
                return instanceProvider.apply(clazz);
            } catch (Exception e) {
                // fallback
            }
        }
        Plugin guice = app.plugins.get("GuicePlugin");
        if (guice != null) {
            try {
                Method getMethod = guice.getClass().getMethod("get", Class.class);
                return getMethod.invoke(guice, clazz);
            } catch (Exception e) {
                // fallback
            }
        }
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public void uninstall() {
        try {
            if (scheduler != null) scheduler.shutdown(true);
        } catch (SchedulerException e) {
            app.log.warn("Scheduler shutdown error: " + e.getMessage());
        }
    }
    
    /**
     * 添加 Cron 任务
     */
    public String cron(String cronExpr, Runnable task) {
        String jobId = "job_" + (++jobCounter);
        try {
            JobDataMap data = new JobDataMap();
            data.put("task", task);
            
            JobDetail job = JobBuilder.newJob(RunnableJob.class)
                .withIdentity(jobId)
                .setJobData(data)
                .build();
            
            Trigger trigger = TriggerBuilder.newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpr))
                .build();
            
            scheduler.scheduleJob(job, trigger);
            return jobId;
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to schedule job", e);
        }
    }
    
    /**
     * 取消任务
     */
    public void cancel(String jobId) {
        try {
            scheduler.deleteJob(JobKey.jobKey(jobId));
        } catch (SchedulerException e) {
            app.log.warn("Failed to cancel job " + jobId + ": " + e.getMessage());
        }
    }
    
    public static class RunnableJob implements Job {
        @Override
        public void execute(JobExecutionContext ctx) {
            Runnable task = (Runnable) ctx.getJobDetail().getJobDataMap().get("task");
            if (task != null) task.run();
        }
    }
}
