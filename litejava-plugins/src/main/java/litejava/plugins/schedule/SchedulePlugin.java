package litejava.plugins.schedule;

import litejava.Plugin;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * 定时任务插件 - 基于 Quartz 实现
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.quartz-scheduler</groupId>
 *     <artifactId>quartz</artifactId>
 *     <version>2.3.2</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * SchedulePlugin schedule = new SchedulePlugin();
 * app.use(schedule);
 * app.listen();
 * 
 * // 每5秒执行
 * schedule.cron("0/5 * * * * ?", () -> System.out.println("每5秒"));
 * 
 * // 每天凌晨2点执行
 * schedule.cron("0 0 2 * * ?", () -> cleanupOldData());
 * 
 * // 每小时执行
 * schedule.cron("0 0 * * * ?", () -> syncData());
 * 
 * // 取消任务
 * String jobId = schedule.cron("0/10 * * * * ?", () -> doSomething());
 * schedule.cancel(jobId);
 * }</pre>
 * 
 * <h2>Cron 表达式</h2>
 * <pre>
 * 秒 分 时 日 月 周
 * 0/5 * * * * ?   = 每5秒
 * 0 0 2 * * ?     = 每天凌晨2点
 * 0 30 * * * ?    = 每小时30分
 * 0 0 0 1 * ?     = 每月1号0点
 * </pre>
 */
public class SchedulePlugin extends Plugin {
    
    /** 默认实例（单例访问） */
    public static SchedulePlugin instance;
    
    public Scheduler scheduler;
    private int jobCounter = 0;
    
    public SchedulePlugin() {
        instance = this;
    }
    
    @Override
    public void config() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to start scheduler", e);
        }
    }
    
    @Override
    public void uninstall() {
        try {
            if (scheduler != null) scheduler.shutdown(true);
        } catch (SchedulerException e) {
            // ignore
        }
    }
    
    /**
     * 添加 Cron 任务
     * @return jobId 用于取消任务
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
            // ignore
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
