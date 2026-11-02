package com.mottainai.mongoapi.security;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import com.mottainai.mongoapi.dto.rag.RagDocumentRequest;

@Aspect @Component
public class TenantAuthorizationAspect {
    private final TenantAuthorization authorization;
    public TenantAuthorizationAspect(TenantAuthorization authorization) { this.authorization=authorization; }
    @Around("within(com.mottainai.mongoapi.controller..*) && @within(org.springframework.web.bind.annotation.RestController)")
    public Object check(ProceedingJoinPoint call) throws Throwable {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth==null || !(auth.getPrincipal() instanceof Jwt jwt)) throw new AccessDeniedException("Access denied");
        String[] names=((MethodSignature)call.getSignature()).getParameterNames();
        Object[] values=call.getArgs();
        boolean found=false;
        for (int i=0; i<values.length; i++) {
            if (names != null && "empresaId".equals(names[i])) {
                authorization.authorize(jwt, (Long)values[i]); found=true;
            }
            if (values[i] instanceof RagDocumentRequest request) {
                authorization.authorize(jwt, request.empresaId()); found=true;
            }
        }
        if (!found) throw new AccessDeniedException("Tenant required");
        return call.proceed();
    }
}
