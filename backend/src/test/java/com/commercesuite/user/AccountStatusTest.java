package com.commercesuite.user;

import com.commercesuite.user.entity.AccountStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountStatusTest {
    @Test void bannedAndDeactivatedCannotLogin() {
        assertFalse(AccountStatus.BANNED.canLogin());
        assertFalse(AccountStatus.DEACTIVATED.canLogin());
        assertTrue (AccountStatus.SUSPENDED.canLogin()); // suspended can still log in, just can't act
    }
    @Test void actionableMatchesFrontend() {
        assertTrue (AccountStatus.ACTIVE.isActionable());
        assertTrue (AccountStatus.PENDING_VERIFICATION.isActionable());
        assertFalse(AccountStatus.SUSPENDED.isActionable());
        assertFalse(AccountStatus.BANNED.isActionable());
        assertFalse(AccountStatus.DEACTIVATED.isActionable());
    }
}
