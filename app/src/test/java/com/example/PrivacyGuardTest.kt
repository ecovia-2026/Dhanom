package com.example

import com.example.domain.privacy.PrivacyGuard
import org.junit.Assert.*
import org.junit.Test

class PrivacyGuardTest {

    @Test
    fun stripsPanAndAccountDigits() {
        val q = PrivacyGuard.sanitizeOutgoingQuestion("My PAN is ABCDE1234F and a/c 123456789012")
        assertFalse(q.contains("ABCDE1234F"))
        assertFalse(q.contains("123456789012"))
        assertTrue(q.contains("[PAN]"))
        assertTrue(q.contains("[ACCT]"))
    }

    @Test
    fun rejectsLedgerDump() {
        val dump = "REAL DATA:\n- Recent:\n- Swiggy 450"
        assertFalse(PrivacyGuard.isSafeForCloud(dump))
        assertTrue(PrivacyGuard.isSafeForCloud(PrivacyGuard.cloudSystemPrompt()))
        assertTrue(PrivacyGuard.isSafeForCloud("Should I prepay a 8.5% home loan?"))
    }

    @Test
    fun cloudPromptHasNoUserRecords() {
        val p = PrivacyGuard.cloudSystemPrompt()
        assertFalse(p.contains("REAL DATA"))
        assertFalse(p.contains("Swiggy"))
        assertTrue(p.contains("SAME language"))
    }
}
