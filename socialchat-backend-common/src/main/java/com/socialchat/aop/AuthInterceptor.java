package com.socialchat.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.socialchat.annotation.AuthCheck;
import com.socialchat.common.ErrorCode;
import com.socialchat.constant.UserConstant;
import com.socialchat.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class AuthInterceptor {

    @Around("@annotation(authCheck)")
    public Object checkAuth(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String authHeader = request.getHeader(UserConstant.AUTHORIZATION);
        if (StringUtils.isBlank(authHeader)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未携带 token，用户未登录");
        }
        String token = authHeader.substring(7);
        Object userVO = StpUtil.getTokenSessionByToken(token).get(UserConstant.USERINFO);
        if (userVO == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "携带 token，用户信息不存在，重新登陆");
        }
        return joinPoint.proceed();
    }
}
