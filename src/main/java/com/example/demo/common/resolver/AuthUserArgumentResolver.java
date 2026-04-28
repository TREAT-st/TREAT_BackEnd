package com.example.demo.common.resolver;

import com.example.demo.common.annotation.AuthUser;
import com.example.demo.common.exception.ErrorStatus;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.user.entity.User;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class)
                && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        User user = (User) authentication.getPrincipal();
        AuthUser annotation = parameter.getParameterAnnotation(AuthUser.class);

        if (!ClassUtils.isAssignable(parameter.getParameterType(), user.getClass())) {
            if (annotation != null && annotation.errorOnInvalidType()) {
                throw new GeneralException(ErrorStatus._ASSIGNABLE_PARAMETER);
            }
        }

        return user;
    }
}
