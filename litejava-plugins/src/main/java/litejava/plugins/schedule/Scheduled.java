package litejava.plugins.schedule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定时任务注解
 * 
 * <pre>{@code
 * @Scheduled("0 * * * * ?")  // 每分钟
 * public void everyMinute() {
 *     // ...
 * }
 * 
 * @Scheduled("0 0 2 * * ?")  // 每天凌晨2点
 * public void dailyCleanup() {
 *     // ...
 * }
 * }</pre>
 * 
 * <h2>Cron 表达式</h2>
 * <pre>
 * 秒 分 时 日 月 周
 * 0/5 * * * * ?   = 每5秒
 * 0 * * * * ?     = 每分钟
 * 0 0 * * * ?     = 每小时
 * 0 0 2 * * ?     = 每天凌晨2点
 * 0 0 0 1 * ?     = 每月1号0点
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled {
    /**
     * Cron 表达式
     */
    String value();
}
