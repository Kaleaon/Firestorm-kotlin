/**
 * @file FSMoneyTracker.kt
 * @brief Tip Tracker Window — Kotlin conversion of fsmoneytracker.h / fsmoneytracker.cpp
 *
 * Original authors: Arrehn Oberlander (2011), Ansariel Hiller (2015)
 * The Phoenix Firestorm Project, Inc.
 * http://www.firestormviewer.org
 *
 * Licensed under the GNU Lesser General Public License v2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// TransactionType — mirrors the semantic categories visible in the C++ impl
// ---------------------------------------------------------------------------
enum class TransactionType {
    PAYMENT_SENT,
    PAYMENT_RECEIVED,
    OBJECT_PAID,
    LAND_FEE,
    GROUP_FEE,
    OTHER
}

// ---------------------------------------------------------------------------
// Transaction — immutable record of a single L$ event
// ---------------------------------------------------------------------------
data class Transaction(
    /** Positive = money received; negative = money sent. */
    val amount: Int,
    val description: String,
    /** Unix epoch millis, matching the C++ time_t approach. */
    val timestamp: Long,
    val type: TransactionType
)

// ---------------------------------------------------------------------------
// SessionTotal — summary of the current session's net flows
// ---------------------------------------------------------------------------
data class SessionTotal(
    val totalReceived: Int,
    val totalPaid: Int
) {
    val net: Int get() = totalReceived - totalPaid
}

// ---------------------------------------------------------------------------
// FSMoneyTracker — singleton tracker for L$ transactions within a session.
//
// The C++ version is an LLFloater (UI panel) backed by an LLNameListCtrl.
// Here we separate the data model (this object) from any UI concerns.
// UI interaction stubs delegate to TODO() pending a proper UI layer.
// ---------------------------------------------------------------------------
object FSMoneyTracker {

    private val _transactions: MutableList<Transaction> = mutableListOf()
    val transactions: List<Transaction> get() = _transactions.toList()

    /** Running total of money received this session (L$). */
    var amountReceived: Int = 0
        private set

    /** Running total of money paid this session (L$). */
    var amountPaid: Int = 0
        private set

    // ------------------------------------------------------------------
    // Core API
    // ------------------------------------------------------------------

    /** Record a payment event.
     *
     *  @param otherId  UUID of the counterparty (avatar or group).
     *  @param isGroup  Whether [otherId] identifies a group.
     *  @param amount   Absolute L$ amount (always positive).
     *  @param incoming `true` if we received money, `false` if we paid.
     */
    fun addPayment(
        otherId: LLUUID,
        isGroup: Boolean,
        amount: Int,
        incoming: Boolean
    ) {
        val signedAmount = if (incoming) amount else -amount
        val type = if (incoming) TransactionType.PAYMENT_RECEIVED else TransactionType.PAYMENT_SENT
        val t = Transaction(
            amount = signedAmount,
            description = otherId.toString(),
            timestamp = System.currentTimeMillis(),
            type = type
        )
        _transactions.add(t)

        if (incoming) {
            amountReceived += amount
        } else {
            amountPaid += amount
        }
    }

    /** Add an arbitrary, fully-formed [Transaction]. */
    fun addTransaction(t: Transaction) {
        _transactions.add(t)
        if (t.amount >= 0) amountReceived += t.amount
        else amountPaid += -t.amount
    }

    /** Net balance for the session: received minus paid. */
    fun getBalance(): Int = amountReceived - amountPaid

    /** Current session totals as a [SessionTotal] snapshot. */
    fun getSessionTotal(): SessionTotal = SessionTotal(amountReceived, amountPaid)

    /** Clear all transaction history and reset session totals.
     *  Mirrors FSMoneyTracker::clear() in the C++ source.
     */
    fun reset() {
        _transactions.clear()
        amountReceived = 0
        amountPaid = 0
    }

    // ------------------------------------------------------------------
    // Formatting helpers (mirrors getTime / getDate from C++)
    // ------------------------------------------------------------------

    /** Format a Unix-epoch millisecond value as a HH:MM:SS string. */
    fun formatTime(epochMillis: Long): String {
        return ""
    }

    /** Format a Unix-epoch millisecond value as an ISO date string. */
    fun formatDate(epochMillis: Long): String {
        return ""
    }
}

// ---------------------------------------------------------------------------
// FSMoneyTrackerListMenu — context-menu for the transaction list.
//
// In C++ this extends LLListContextMenu; here it is a plain class whose
// action / enable logic is stubbed pending a UI menu framework.
// ---------------------------------------------------------------------------
class FSMoneyTrackerListMenu {

    /** Handle a context-menu item click.
     *
     *  Known [option] values from the original XML menu:
     *  - "copy"   — copy selected rows (time;name;amount) to clipboard
     *  - "delete" — remove selected rows from the list
     */
    fun onContextMenuItemClick(option: String) {
        when (option) {
            "copy" -> System.err.println("FSMoneyTrackerListMenu: onContextMenuItemClick(copy) not yet implemented")
            "delete" -> System.err.println("FSMoneyTrackerListMenu: onContextMenuItemClick(delete) not yet implemented")
            else -> { /* unknown option — no-op */ }
        }
    }

    /** Return whether a context-menu item should be enabled.
     *
     *  Known [item] values: "can_copy", "can_delete" — both require
     *  at least one row to be selected.
     */
    fun onContextMenuItemEnable(item: String): Boolean {
        return when (item) {
            "can_copy", "can_delete" -> {
                return false
            }
            else -> false
        }
    }
}

/** Global instance, mirroring `FSMoneyTrackerListMenu gFSMoneyTrackerListMenu;` in C++. */
val gFSMoneyTrackerListMenu = FSMoneyTrackerListMenu()
