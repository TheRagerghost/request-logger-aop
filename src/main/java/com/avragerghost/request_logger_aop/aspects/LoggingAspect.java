package com.avragerghost.request_logger_aop.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.avragerghost.request_logger_aop.config.LoggingLevel;
import com.avragerghost.request_logger_aop.config.LoggingProperties;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_DIM = "\u001B[2m";
    private static final String ANSI_RESET = "\u001B[0m";
    private final LoggingProperties properties;

    public LoggingAspect(LoggingProperties properties) {
        this.properties = properties;
    }

    @Pointcut("within(@com.avragerghost.request_logger_aop.aspects.annotations.LoggableService *)")
    public void loggableServiceMethods() {
    }

    @Pointcut("within(@com.avragerghost.request_logger_aop.aspects.annotations.LoggableController *) || @annotation(com.avragerghost.request_logger_aop.aspects.annotations.LoggableControllerMethod)")
    public void loggableControllerMethods() {
    }

    private boolean shouldLog(LoggingLevel level) {
        if (!properties.isEnabled()) {
            return false;
        }
        LoggingLevel configuredLevel = properties.getLoggingLevel();
        return configuredLevel == LoggingLevel.ALL || configuredLevel == level;
    }

    @Around("@annotation(com.avragerghost.request_logger_aop.aspects.annotations.LogExecTime)")
    public Object logExecTime(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!shouldLog(LoggingLevel.DEBUG)) {
            return joinPoint.proceed();
        }
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        logger.debug(
                ANSI_DIM + "[DEBUG]" + ANSI_RESET + " Метод {} выполнен за "
                        + ANSI_YELLOW + "{}ms" + ANSI_RESET,
                joinPoint.getSignature().getName(), endTime - startTime);
        return result;
    }

    @AfterThrowing(pointcut = "loggableServiceMethods()", throwing = "e")
    public void handleTaskServiceException(JoinPoint joinPoint, Exception e) {
        if (!shouldLog(LoggingLevel.ERROR)) {
            return;
        }
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        logger.error(ANSI_RED + "[ERROR]" + ANSI_RESET + "\nОшибка в методе {} с аргументами: {}\nСообщение: {}",
                methodName, args, e);
    }

    @Before("loggableControllerMethods() || @annotation(com.avragerghost.request_logger_aop.aspects.annotations.LoggableRequest)")
    public void logRequest(JoinPoint joinPoint) {
        if (!shouldLog(LoggingLevel.DEBUG)) {
            return;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            Object[] args = joinPoint.getArgs();
            String body = args.length > 0 ? args[0] != null ? args[0].toString() : "null" : "none";
            logger.debug(ANSI_DIM + "[DEBUG]" + ANSI_RESET + " Запрос: {} {} с телом: {}", method, uri, body);
        }
    }

    @AfterReturning(pointcut = "loggableControllerMethods() || @annotation(com.avragerghost.request_logger_aop.aspects.annotations.LoggableResponse)", returning = "result")
    public void logResponse(JoinPoint joinPoint, Object result) {
        if (!shouldLog(LoggingLevel.INFO) && !shouldLog(LoggingLevel.WARNING) && !shouldLog(LoggingLevel.ERROR)) {
            return;
        }
        String methodName = joinPoint.getSignature().getName();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String uri = attributes != null ? attributes.getRequest().getRequestURI() : "unknown";

        if (result instanceof ResponseEntity<?> responseEntity) {
            int statusCode = responseEntity.getStatusCode().value();
            Object body = responseEntity.getBody();
            String className = body != null ? body.getClass().getSimpleName() : "null";
            String logMessage = body != null
                    ? ANSI_GREEN + "[" + className + "]" + ANSI_RESET + " -> " + body.toString()
                    : "null";

            if (statusCode >= 200 && statusCode < 300) {
                if (shouldLog(LoggingLevel.INFO)) {
                    logger.info(ANSI_BLUE + "[ INFO]" + ANSI_RESET + " Ответ на {}: HTTP {} {}", uri, statusCode,
                            logMessage);
                }
            } else if (statusCode == 404) {
                if (shouldLog(LoggingLevel.ERROR)) {
                    logger.error(ANSI_RED + "[ERROR]" + ANSI_RESET + " Ответ на {}: HTTP {} {}", uri, statusCode,
                            logMessage);
                }
            } else {
                if (shouldLog(LoggingLevel.WARNING)) {
                    logger.warn(ANSI_YELLOW + "[ WARN]" + ANSI_RESET + " Ответ на {}: HTTP {} {}", uri, statusCode,
                            logMessage);
                }
            }
        } else if (result == null) {
            if (shouldLog(LoggingLevel.WARNING)) {
                logger.warn(ANSI_YELLOW + "[ WARN]" + ANSI_RESET + " Ответ на {}: null от метода {}", uri, methodName);
            }
        } else {
            if (shouldLog(LoggingLevel.INFO)) {
                String className = result.getClass().getSimpleName();
                String logMessage = ANSI_BLUE + "[" + className + "]" + ANSI_RESET + " -> " + result.toString();
                logger.info(ANSI_BLUE + "[ INFO]" + ANSI_RESET + " Ответ на {}: {}", uri, logMessage);
            }
        }
    }

}
