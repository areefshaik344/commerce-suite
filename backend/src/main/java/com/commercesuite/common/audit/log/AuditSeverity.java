package com.commercesuite.common.audit.log;

/**
 * Audit severity ladder. Order matters for filtering ({@code >= WARNING}, etc.).
 *
 * <ul>
 *   <li>{@link #INFO}     — routine, expected business activity.</li>
 *   <li>{@link #WARNING}  — noteworthy lifecycle change (approval, refund).</li>
 *   <li>{@link #HIGH}     — financially / contractually significant action
 *                            (settlement lock, vendor suspension).</li>
 *   <li>{@link #CRITICAL} — security / integrity event requiring investigation.</li>
 * </ul>
 */
public enum AuditSeverity { INFO, WARNING, HIGH, CRITICAL }