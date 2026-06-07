package com.commercesuite.rbac.service;

import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {
    private final ActorContextHolder holder;

    @Before("@annotation(rp)")
    public void check(RequiresPermission rp) {
        ActorContext a = holder.require();
        boolean ok = rp.all()
                ? java.util.Arrays.stream(rp.value()).allMatch(a::hasPermission)
                : java.util.Arrays.stream(rp.value()).anyMatch(a::hasPermission);
        if (!ok) throw AppException.forbidden("Missing required permission");
    }
}
