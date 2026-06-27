package com.snapreport.sdk.util

/**
 * Provides the current time.  Swap out in tests.
 */
sealed class TimeProvider {

    /** Returns the current epoch time in microseconds. */
    abstract fun nowMicros(): Long

    /** Returns the current epoch time in milliseconds. */
    fun nowMillis(): Long = nowMicros() / 1_000

    /** Production implementation backed by [System.currentTimeMillis]. */
    object System : TimeProvider() {
        override fun nowMicros(): Long = java.lang.System.currentTimeMillis() * 1_000
    }

    /** Test-friendly stub that returns a fixed value. */
    class Fixed(private val micros: Long) : TimeProvider() {
        override fun nowMicros(): Long = micros
    }
}

