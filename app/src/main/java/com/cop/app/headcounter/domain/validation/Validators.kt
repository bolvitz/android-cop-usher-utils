package com.cop.app.headcounter.domain.validation

import com.cop.app.headcounter.domain.common.ValidationFailure

/**
 * Validation utilities for common input validation patterns
 */
object Validators {

    /**
     * Validates that a string is not empty
     */
    fun validateNotEmpty(value: String, fieldName: String): ValidationFailure? {
        return if (value.isBlank()) {
            ValidationFailure.EmptyString(fieldName)
        } else null
    }

    /**
     * Validates string length
     */
    fun validateLength(
        value: String,
        fieldName: String,
        minLength: Int? = null,
        maxLength: Int? = null
    ): ValidationFailure? {
        minLength?.let {
            if (value.length < it) {
                return ValidationFailure.TooShort(fieldName, it)
            }
        }
        maxLength?.let {
            if (value.length > it) {
                return ValidationFailure.TooLong(fieldName, it)
            }
        }
        return null
    }

    /**
     * Validates that a number is positive
     */
    fun validatePositive(value: Int, fieldName: String): ValidationFailure? {
        return when {
            value < 0 -> ValidationFailure.NegativeNumber(fieldName)
            value == 0 -> ValidationFailure.ZeroValue(fieldName)
            else -> null
        }
    }

    /**
     * Validates that a number is non-negative (can be zero)
     */
    fun validateNonNegative(value: Int, fieldName: String): ValidationFailure? {
        return if (value < 0) {
            ValidationFailure.NegativeNumber(fieldName)
        } else null
    }

    /**
     * Validates number range
     */
    fun validateRange(
        value: Int,
        fieldName: String,
        minimum: Int? = null,
        maximum: Int? = null
    ): ValidationFailure? {
        minimum?.let {
            if (value < it) {
                return ValidationFailure.TooSmall(fieldName, it)
            }
        }
        maximum?.let {
            if (value > it) {
                return ValidationFailure.TooLarge(fieldName, it)
            }
        }
        return null
    }

    /**
     * Validates that a date is not in the future
     */
    fun validateNotFuture(timestamp: Long, fieldName: String): ValidationFailure? {
        return if (timestamp > System.currentTimeMillis()) {
            ValidationFailure.FutureDate(fieldName)
        } else null
    }

    /**
     * Validates that a date is not in the past
     */
    fun validateNotPast(timestamp: Long, fieldName: String): ValidationFailure? {
        return if (timestamp < System.currentTimeMillis()) {
            ValidationFailure.PastDate(fieldName)
        } else null
    }

    /**
     * Validates date range (start before end)
     */
    fun validateDateRange(startDate: Long, endDate: Long, fieldName: String = "Date range"): ValidationFailure? {
        return if (startDate > endDate) {
            ValidationFailure.InvalidDateRange(fieldName)
        } else null
    }

    /**
     * Validates email format (basic)
     */
    fun validateEmail(email: String, fieldName: String = "Email"): ValidationFailure? {
        if (email.isBlank()) return null // Allow empty emails (optional field)

        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return if (!email.matches(emailRegex)) {
            ValidationFailure.InvalidFormat(fieldName, "valid email address")
        } else null
    }

    /**
     * Validates phone number format (basic)
     */
    fun validatePhone(phone: String, fieldName: String = "Phone"): ValidationFailure? {
        if (phone.isBlank()) return null // Allow empty phones (optional field)

        // Allow common formats: (123) 456-7890, 123-456-7890, 1234567890, +1-234-567-8900
        val phoneRegex = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s\\.]?[(]?[0-9]{1,4}[)]?[-\\s\\.]?[0-9]{1,9}$".toRegex()
        return if (!phone.matches(phoneRegex)) {
            ValidationFailure.InvalidFormat(fieldName, "valid phone number")
        } else null
    }

    /**
     * Collects all validation failures
     */
    fun collectErrors(vararg validators: ValidationFailure?): List<ValidationFailure> {
        return validators.filterNotNull()
    }

    /**
     * Validates and returns Result
     */
    inline fun <T> validate(
        value: T,
        vararg validators: ValidationFailure?
    ): com.cop.app.headcounter.domain.common.Result<T> {
        val errors = collectErrors(*validators)
        return if (errors.isEmpty()) {
            com.cop.app.headcounter.domain.common.Result.Success(value)
        } else {
            com.cop.app.headcounter.domain.common.Result.Error(
                com.cop.app.headcounter.domain.common.AppError.ValidationError(errors)
            )
        }
    }
}
