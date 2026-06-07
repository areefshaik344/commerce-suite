# Security Hardening Report

Findings from the Phase 9 audit. Severity classes: **BLOCKER / HIGH / MEDIUM / LOW**.

## 1. Strengths (verified in code)

| Area | Implementation |
|------|----------------|
| Password hashing | BCrypt(strength=12) in `SecurityConfig` |
| Stateless auth | JWT access + refresh, rotation on use |
| RBAC | `app_role` enum + `user_roles` table + `has_role()` SECURITY DEFINER (per project memory) |
| Method security | `@EnableMethodSecurity` + `@PreAuthorize` on controllers |
| Ownership guards | `*OwnershipGuard` classes (settlement, payout, product) reject cross-tenant access |
| Idempotency | `Idempotency-Key` header + 24 h dedupe (`PAYMENT_IDEMPOTENCY.md`) |
| Audit log | Append-only, `REVOKE DELETE`, signed by service definer (Phase 8.3) |
| Webhook signing | HMAC-SHA256 with timestamp + nonce, replay window (Phase 8.5) |
| Money safety | Integer paise, CHECK ≥ 0, currency pinned INR |
| Append-only finance | Payment/refund/settlement history `REVOKE DELETE` |
| Outbox isolation | Consumers in `REQUIRES_NEW`; failures cannot poison source TX |

## 2. Findings

### BLOCKER

_None at the application layer._ Deployment-layer blockers tracked in `PRODUCTION_READINESS_REPORT.md`.

### HIGH

| ID | Finding | Remediation |
|----|---------|-------------|
| S-H1 | No rate limiting on auth/payment endpoints | Add Bucket4j + Redis; 5/min per IP+email for login, 3/hour for password reset |
| S-H2 | No account lockout / brute-force protection | Track failed logins; lock after N attempts in M minutes; alert on bursts |
| S-H3 | Security headers absent (CSP, HSTS, XFO, Referrer-Policy, Permissions-Policy) | Add `HeaderWriter` chain in `SecurityConfig` |
| S-H4 | HIBP password check not enabled | Wire HIBP k-anonymity API on signup + password reset |
| S-H5 | No MFA for admin / finance roles | Add TOTP enrolment + step-up auth on admin endpoints |
| S-H6 | Outbound webhook egress not pinned (SSRF risk on user-supplied URLs) | Validate destination URL: deny RFC1918, link-local, metadata IPs; resolve+pin |
| S-H7 | No dependency scanning gate in CI | Add OWASP dep-check / Snyk; fail on HIGH+ |

### MEDIUM

| ID | Finding | Remediation |
|----|---------|-------------|
| S-M1 | CSRF disabled — acceptable for pure JWT, but document and forbid cookie-borne auth | Add ADR; lint guard |
| S-M2 | Notification template variables not HTML-sanitised (XSS in HTML email) | Run user-supplied vars through OWASP HTML sanitiser before render |
| S-M3 | Refresh token rotation reuse-detection not asserted in tests | Add IT covering reuse → invalidate family |
| S-M4 | JWT signing key rotation procedure undocumented | Document dual-`kid` rotation in `OPERATIONS_RUNBOOK.md` (done) |
| S-M5 | Admin endpoints accept full email/PII in path/query → leak via access logs | Use IDs only; strip PII from `MDC` |
| S-M6 | No automated SAST | Add `spotbugs` + `semgrep` rules |
| S-M7 | Audit export contains PII without re-auth | Require step-up before export |

### LOW

| ID | Finding | Remediation |
|----|---------|-------------|
| S-L1 | Verbose error messages on validation failures | Generic message + correlation id |
| S-L2 | No CAPTCHA on signup | Optional, behind feature flag |
| S-L3 | Session list endpoint shows IP/UA without truncation | Mask IPs for display |
| S-L4 | No content-length limit on uploads beyond container default | Enforce per-route limits |

## 3. Compliance checkpoints

- **GDPR:** Export and deletion paths exist in `src/lib/gdpr.ts`; backend equivalents must enforce retention exceptions for financial records (7 y retention in `AUDIT_MODULE.md`).
- **PCI:** No PAN stored — gateway tokens only. Verify by code search before each release.
- **Audit retention:** Policies exist (`AuditRetentionPolicy`); confirm scheduled job in prod.

## 4. Recommended sprint sequence

1. Sprint A: S-H1, S-H2, S-H3 (network surface).
2. Sprint B: S-H4, S-H5 (account security).
3. Sprint C: S-H6, S-H7 (egress + supply chain).
4. Sprint D: Mediums.

After Sprints A–C the platform reaches the **READY FOR PRODUCTION** security bar.