package com.myself.teamfiles.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myself.teamfiles.common.annotation.OperationLog;
import com.myself.teamfiles.module.log.mapper.OperationLogMapper;
import com.myself.teamfiles.security.JwtContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.myself.teamfiles.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationLog annotation = ((MethodSignature) joinPoint.getSignature())
                .getMethod().getAnnotation(OperationLog.class);

        com.myself.teamfiles.module.log.entity.OperationLog logEntity =
                new com.myself.teamfiles.module.log.entity.OperationLog();
        logEntity.setModule(annotation.module());
        logEntity.setOperation(annotation.operation());
        logEntity.setMethod(joinPoint.getSignature().toShortString());

        try {
            Long userId = JwtContextHolder.getUserId();
            if (userId != null) {
                logEntity.setUserId(userId);
                logEntity.setUsername(JwtContextHolder.getUsername());
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logEntity.setIp(request.getRemoteAddr());
            }

            Object[] args = joinPoint.getArgs();
            if (args.length > 0) {
                try {
                    String params = objectMapper.writeValueAsString(args);
                    if (params.length() > 2000) {
                        params = params.substring(0, 2000);
                    }
                    logEntity.setRequestParams(params);
                } catch (Exception ignored) {
                }
            }

            Object result = joinPoint.proceed();

            logEntity.setResult(1);
            logEntity.setExecutionTime(System.currentTimeMillis() - startTime);

            return result;
        } catch (Exception e) {
            logEntity.setResult(0);
            logEntity.setExecutionTime(System.currentTimeMillis() - startTime);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 2000) {
                errorMsg = errorMsg.substring(0, 2000);
            }
            logEntity.setErrorMsg(errorMsg);
            throw e;
        } finally {
            try {
                operationLogMapper.insert(logEntity);
            } catch (Exception e) {
                log.error("Failed to save operation log", e);
            }
        }
    }
}
