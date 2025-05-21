package com.avragerghost.request_logger_aop.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.avragerghost.request_logger_aop.config.LoggingLevel;
import com.avragerghost.request_logger_aop.config.LoggingProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
public class LoggingAspect {

    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_DIM = "\u001B[2m";
    private static final String ANSI_RESET = "\u001B[0m";
    private final LoggingProperties properties;
    private final ObjectMapper objectMapper;

    public LoggingAspect(LoggingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
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
        System.out.println(
                ANSI_DIM + "[DEBUG]" + ANSI_RESET + " Method " + joinPoint.getSignature().getName() + " executed in "
                        + ANSI_YELLOW + (endTime - startTime) + "ms" + ANSI_RESET);
        return result;
    }

    @AfterThrowing(pointcut = "loggableServiceMethods()", throwing = "e")
    public void handleTaskServiceException(JoinPoint joinPoint, Exception e) {
        if (!shouldLog(LoggingLevel.ERROR)) {
            return;
        }
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        System.out.println(ANSI_RED + "[ERROR]" + ANSI_RESET + "\nError in method " + methodName + " w/ args: "
                + args + "\nMessage: " + e);
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
            String body;
            try {
                body = args.length > 0 && args[0] != null ? objectMapper.writeValueAsString(args[0]) : "none";
            } catch (JsonProcessingException e) {
                body = "error serializing request body";
                System.out.println(
                        ANSI_DIM + "[DEBUG]" + ANSI_RESET + "Failed to serialize request body: " + e.getMessage());
            }
            System.out.println(
                    ANSI_DIM + "[DEBUG]" + ANSI_RESET + " Request: " + method + " " + uri + " w/ body: " + body);
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
            String logMessageBody;

            try {
                logMessageBody = body != null
                        ? ANSI_BLUE + "[" + className + "]" + ANSI_RESET + " -> "
                                + objectMapper.writeValueAsString(body)
                        : "null";
            } catch (JsonProcessingException e) {
                logMessageBody = "error serializing response body";
            }

            String logMessage = "Response at " + uri + ": HTTP " + statusCode + " " + logMessageBody;

            if (statusCode >= 200 && statusCode < 300) {
                if (shouldLog(LoggingLevel.INFO)) {
                    System.out.println(ANSI_BLUE + "[ INFO]" + ANSI_RESET + " " + logMessage);
                }
            } else if (statusCode == 404) {
                if (shouldLog(LoggingLevel.ERROR)) {
                    System.out.println(ANSI_RED + "[ERROR]" + ANSI_RESET + " " + logMessage);
                }
            } else {
                if (shouldLog(LoggingLevel.WARNING)) {
                    System.out.println(ANSI_YELLOW + "[ WARN]" + ANSI_RESET + " " + logMessage);
                }
            }
        } else if (result == null) {
            if (shouldLog(LoggingLevel.WARNING)) {
                System.out.println(
                        ANSI_YELLOW + "[ WARN]" + ANSI_RESET + " Response at " + uri + ": null in method "
                                + methodName);
            }
        } else {
            if (shouldLog(LoggingLevel.INFO)) {
                String className = result.getClass().getSimpleName();
                String logMessage = "Response at " + uri + ": " + ANSI_BLUE + "[" + className + "]" + ANSI_RESET
                        + " -> "
                        + result.toString();
                System.out.println(ANSI_BLUE + "[ INFO]" + ANSI_RESET + " " + logMessage);
            }
        }
    }

}
