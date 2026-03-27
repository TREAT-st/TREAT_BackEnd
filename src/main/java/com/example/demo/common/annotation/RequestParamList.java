package com.example.demo.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParamList {
    String value(); // 원래 `@RequestParam`에서 쓰이는 값
    String delimiter() default ","; // 기본 구분자 `+`
    boolean nullable() default true;
}
