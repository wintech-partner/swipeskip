package io.github.kutlu24.swipeskip.billing.noop

import kotlinx.coroutines.test.runTest
import io.github.kutlu24.swipeskip.billing.api.RestoreResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoopEntitlementsTest {
    @Test fun `everything is unlocked and no store is offered`() = runTest {
        val e = NoopEntitlements()
        assertTrue(e.isPremium.value)
        assertFalse(e.isStoreAvailable)
        assertEquals(RestoreResult.UNAVAILABLE, e.restorePurchases())
    }
}
