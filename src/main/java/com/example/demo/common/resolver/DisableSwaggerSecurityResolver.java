package com.example.demo.common.resolver;

import com.example.demo.common.annotation.DisableSwaggerSecurity;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Collections;

@Component
public class DisableSwaggerSecurityResolver implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        DisableSwaggerSecurity annotation = handlerMethod.getMethodAnnotation(DisableSwaggerSecurity.class);
        if (annotation != null) {
            operation.setSecurity(Collections.emptyList());
        }
        return operation;
    }
}
