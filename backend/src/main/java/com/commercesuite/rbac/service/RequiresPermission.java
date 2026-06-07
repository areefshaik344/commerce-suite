package com.commercesuite.rbac.service;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String[] value();
    /** When true, ALL listed permissions are required. Default: ANY. */
    boolean all() default false;
}
