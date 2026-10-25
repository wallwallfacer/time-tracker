package dev.timetracker.core

object LaunchDecision {
    /** A fresh launch or a new intent is a real press; system recreation (saved state present) is not. */
    fun shouldRecord(savedInstanceStateIsNull: Boolean, fromNewIntent: Boolean): Boolean =
        fromNewIntent || savedInstanceStateIsNull
}
