package com.smartagri.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DeadlockRetryAspect {

    private static final Logger log = LoggerFactory.getLogger(DeadlockRetryAspect.class);

    @Around("@annotation(retry)")
    public Object retryOnDeadlock(ProceedingJoinPoint pjp, RetryOnDeadlock retry) throws Throwable {
        int maxRetries = retry.maxRetries();
        long delayMs = retry.delayMs();
        int attempt = 0;
        while (true) {
            try {
                return pjp.proceed();
            } catch (DeadlockLoserDataAccessException
                     | CannotAcquireLockException
                     | LockAcquisitionException e) {
                attempt++;
                if (attempt > maxRetries) throw e;
                long sleep = delayMs * (1L << (attempt - 1));
                log.warn("检测到死锁, 方法={}, 第{}次重试, 等待{}ms",
                        ((MethodSignature) pjp.getSignature()).getName(),
                        attempt, sleep);
                Thread.sleep(sleep);
            } catch (DataAccessException e) {
                if (e.getCause() != null && e.getCause().getMessage() != null
                        && e.getCause().getMessage().toUpperCase().contains("DEADLOCK")) {
                    attempt++;
                    if (attempt > maxRetries) throw e;
                    long sleep = delayMs * (1L << (attempt - 1));
                    log.warn("检测到死锁(DataAccessException), 方法={}, 第{}次重试, 等待{}ms",
                            ((MethodSignature) pjp.getSignature()).getName(),
                            attempt, sleep);
                    Thread.sleep(sleep);
                    continue;
                }
                throw e;
            }
        }
    }
}
