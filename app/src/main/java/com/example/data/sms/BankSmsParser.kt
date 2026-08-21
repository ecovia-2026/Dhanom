package com.example.data.sms

import com.example.data.model.ExpenseNecessity
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType

/**
 * Parses Indian bank / UPI / card transaction SMS into a TransactionEntity.
 * Handles "debited", "credited", "Rs.500", "INR 500", "UPI", card "XXXX1234",
 * and common Indian merchants. Returns null when the SMS is not a transaction.
 */
object BankSmsParser {

    data class SmsTx(
        val amount: Double,
        val type: TransactionType,
        val merchant: String,
        val category: TransactionCategory,
        val accountHint: String
    )

    fun parse(body: String): SmsTx? {
        val text = body.trim()
        val lower = text.lowercase()
        if (!isBankSms(lower)) return null

        val amount = extractAmount(text) ?: return null
        if (amount <= 0) return null

        val isCredit = lower.contains("credited") || lower.contains("credit") ||
                lower.contains("received") || lower.contains("refund") || lower.contains("cashback")
        val isDebit = lower.contains("debited") || lower.contains("debit") ||
                lower.contains("spent") || lower.contains("paid") || lower.contains("payment") ||
                lower.contains("purchased") || lower.contains("withdraw")

        val type = when {
            isCredit -> TransactionType.INCOME
            isDebit -> TransactionType.EXPENSE
            else -> null
        } ?: return null

        // Account hint: UPI / card / account
        val accountHint = when {
            lower.contains("upi") -> "UPI"
            lower.contains("card") || Regex("""\bx{3,}\d{2,}""").containsMatchIn(lower) -> "Card"
            lower.contains("account") || lower.contains("a/c") -> "Bank"
            else -> "Bank"
        }

        // Merchant / title
        val merchant = extractMerchant(text, lower) ?: categoryGuess(lower).displayName

        val category = if (type == TransactionType.INCOME) {
            when {
                lower.contains("salary") -> TransactionCategory.SALARY
                lower.contains("refund") || lower.contains("cashback") -> TransactionCategory.OTHER
                lower.contains("interest") || lower.contains("dividend") -> TransactionCategory.INVESTMENT_RETURN
                else -> TransactionCategory.OTHER
            }
        } else {
            categoryGuess(lower)
        }

        return SmsTx(amount, type, merchant, category, accountHint)
    }

    fun toEntity(sms: SmsTx, now: Long = System.currentTimeMillis()): TransactionEntity =
        TransactionEntity(
            title = sms.merchant,
            amount = sms.amount,
            type = sms.type,
            category = sms.category,
            necessity = sms.category.defaultNecessity,
            account = sms.accountHint,
            merchant = sms.merchant,
            timestamp = now,
            notes = "Auto-logged from bank SMS"
        )

    private fun isBankSms(lower: String): Boolean {
        val markers = listOf("debited", "credited", "debit", "credit", "upi", "payment",
            "purchased", "withdraw", "refund", "cashback", "spent", "available balance",
            "ac/", "a/c", "rs.", "inr", "balance", "bank", "card")
        return markers.count { lower.contains(it) } >= 1 && (lower.contains("rs") || lower.contains("inr") || lower.contains("₹"))
    }

    private fun extractAmount(text: String): Double? {
        // "Rs.500" / "Rs 500" / "INR 500" / "₹500" / "500.00"
        Regex("""(?i)(?:rs\.?|inr|₹|rs)\s*([\d,]+(?:\.\d{1,2})?)""").find(text)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        // fallback: any number near debit/credit keywords
        Regex("""(?i)(?:debited|credited|debit|credit|paid|spent|received)[^\d]{0,20}([\d,]+(?:\.\d{1,2})?)""").find(text)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        return null
    }

    private fun extractMerchant(text: String, lower: String): String? {
        // "at MERCHANT on" / "to MERCHANT" / "at MERCHANT"
        val m = Regex("""(?i)(?:at|to|via|on)\s+([A-Za-z0-9&.'\- ]{3,30}?)(?:\s+(?:on|via|for|ref|from|using|avail))""").find(text)
        return m?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun categoryGuess(lower: String): TransactionCategory = when {
        lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("restaurant") -> TransactionCategory.DINING
        lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("shopping") -> TransactionCategory.SHOPPING
        lower.contains("uber") || lower.contains("ola") || lower.contains("petrol") || lower.contains("fuel") || lower.contains("irctc") || lower.contains("metro") -> TransactionCategory.TRANSPORTATION
        lower.contains("electric") || lower.contains("bill") || lower.contains("recharge") || lower.contains("airtel") || lower.contains("jio") || lower.contains("wifi") || lower.contains("broadband") -> TransactionCategory.UTILITIES
        lower.contains("rent") || lower.contains("landlord") -> TransactionCategory.HOUSING
        lower.contains("hospital") || lower.contains("pharmacy") || lower.contains("medic") || lower.contains("doctor") -> TransactionCategory.HEALTHCARE
        lower.contains("netflix") || lower.contains("spotify") || lower.contains("prime") || lower.contains("subscription") -> TransactionCategory.SUBSCRIPTIONS
        lower.contains("lic") || lower.contains("premium") || lower.contains("insurance") -> TransactionCategory.INSURANCE
        lower.contains("sip") || lower.contains("mutual") || lower.contains("invest") || lower.contains("zerodha") || lower.contains("groww") -> TransactionCategory.INVESTMENT
        lower.contains("loan") || lower.contains("emi") -> TransactionCategory.OTHER
        else -> TransactionCategory.OTHER
    }
}
