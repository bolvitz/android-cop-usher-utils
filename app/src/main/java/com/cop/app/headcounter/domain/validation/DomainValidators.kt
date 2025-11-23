package com.cop.app.headcounter.domain.validation

import com.cop.app.headcounter.domain.common.AppError
import com.cop.app.headcounter.domain.common.Result
import com.cop.app.headcounter.domain.common.ValidationFailure

/**
 * Domain-specific validators for the church attendance app
 */
object DomainValidators {

    /**
     * Validates branch creation input
     */
    fun validateBranchInput(
        name: String,
        location: String,
        code: String,
        contactEmail: String? = null,
        contactPhone: String? = null
    ): Result<Unit> {
        val errors = Validators.collectErrors(
            Validators.validateNotEmpty(name, "Branch name"),
            Validators.validateLength(name, "Branch name", maxLength = 100),
            Validators.validateNotEmpty(location, "Location"),
            Validators.validateLength(location, "Location", maxLength = 200),
            Validators.validateNotEmpty(code, "Branch code"),
            Validators.validateLength(code, "Branch code", minLength = 2, maxLength = 10),
            contactEmail?.let { Validators.validateEmail(it, "Contact email") },
            contactPhone?.let { Validators.validatePhone(it, "Contact phone") }
        )

        return if (errors.isEmpty()) {
            Result.Success(Unit)
        } else {
            Result.Error(AppError.ValidationError(errors))
        }
    }

    /**
     * Validates service type creation input
     */
    fun validateServiceTypeInput(
        name: String,
        dayType: String,
        time: String,
        description: String? = null
    ): Result<Unit> {
        val errors = Validators.collectErrors(
            Validators.validateNotEmpty(name, "Service type name"),
            Validators.validateLength(name, "Service type name", maxLength = 100),
            Validators.validateNotEmpty(dayType, "Day type"),
            Validators.validateLength(dayType, "Day type", maxLength = 50),
            Validators.validateNotEmpty(time, "Time"),
            Validators.validateLength(time, "Time", maxLength = 20),
            description?.let { Validators.validateLength(it, "Description", maxLength = 500) }
        )

        return if (errors.isEmpty()) {
            Result.Success(Unit)
        } else {
            Result.Error(AppError.ValidationError(errors))
        }
    }

    /**
     * Validates area template creation input
     */
    fun validateAreaTemplateInput(
        name: String,
        capacity: Int,
        notes: String? = null
    ): Result<Unit> {
        val errors = Validators.collectErrors(
            Validators.validateNotEmpty(name, "Area name"),
            Validators.validateLength(name, "Area name", maxLength = 100),
            Validators.validatePositive(capacity, "Capacity"),
            Validators.validateRange(capacity, "Capacity", minimum = 1, maximum = 10000),
            notes?.let { Validators.validateLength(it, "Notes", maxLength = 500) }
        )

        return if (errors.isEmpty()) {
            Result.Success(Unit)
        } else {
            Result.Error(AppError.ValidationError(errors))
        }
    }

    /**
     * Validates service creation input
     */
    fun validateServiceInput(
        branchId: String,
        serviceTypeId: String?,
        date: Long,
        countedBy: String,
        serviceName: String? = null,
        notes: String? = null
    ): Result<Unit> {
        val errors = Validators.collectErrors(
            Validators.validateNotEmpty(branchId, "Branch"),
            if (serviceTypeId != null) Validators.validateNotEmpty(serviceTypeId, "Service type") else null,
            Validators.validateNotFuture(date, "Service date"),
            Validators.validateNotEmpty(countedBy, "Counter name"),
            Validators.validateLength(countedBy, "Counter name", maxLength = 100),
            serviceName?.let { Validators.validateLength(it, "Service name", maxLength = 100) },
            notes?.let { Validators.validateLength(it, "Notes", maxLength = 1000) }
        )

        return if (errors.isEmpty()) {
            Result.Success(Unit)
        } else {
            Result.Error(AppError.ValidationError(errors))
        }
    }

    /**
     * Validates attendance count input
     */
    fun validateAttendanceCount(
        count: Int,
        capacity: Int
    ): Result<Unit> {
        val errors = Validators.collectErrors(
            Validators.validateNonNegative(count, "Count"),
            Validators.validateRange(count, "Count", maximum = 99999),
            Validators.validatePositive(capacity, "Capacity"),
            // Warning if count exceeds capacity (not an error, just informational)
            if (count > capacity) {
                ValidationFailure.Custom("Count", "Count ($count) exceeds capacity ($capacity)")
            } else null
        )

        return if (errors.isEmpty()) {
            Result.Success(Unit)
        } else {
            Result.Error(AppError.ValidationError(errors))
        }
    }

    /**
     * Validates report date range
     */
    fun validateReportDateRange(
        startDate: Long,
        endDate: Long
    ): Result<Unit> {
        val errors = Validators.collectErrors(
            Validators.validateNotFuture(startDate, "Start date"),
            Validators.validateNotFuture(endDate, "End date"),
            Validators.validateDateRange(startDate, endDate, "Date range"),
            // Validate range is not too large (e.g., max 1 year)
            if (endDate - startDate > 365L * 24 * 60 * 60 * 1000) {
                ValidationFailure.Custom("Date range", "Date range cannot exceed 1 year")
            } else null
        )

        return if (errors.isEmpty()) {
            Result.Success(Unit)
        } else {
            Result.Error(AppError.ValidationError(errors))
        }
    }

    /**
     * Validates manual count edit
     */
    fun validateManualCountEdit(
        newCount: Int,
        capacity: Int,
        isLocked: Boolean
    ): Result<Unit> {
        if (isLocked) {
            return Result.Error(
                AppError.ServiceLocked("Cannot edit count for locked service")
            )
        }

        return validateAttendanceCount(newCount, capacity)
    }

    /**
     * Validates branch code format (alphanumeric, no spaces)
     */
    fun validateBranchCode(code: String): ValidationFailure? {
        if (code.isBlank()) {
            return ValidationFailure.EmptyString("Branch code")
        }

        val codeRegex = "^[A-Z0-9]{2,10}$".toRegex()
        return if (!code.matches(codeRegex)) {
            ValidationFailure.InvalidFormat(
                "Branch code",
                "2-10 uppercase letters or numbers (e.g., MC, NB, DT)"
            )
        } else null
    }

    /**
     * Validates time format (e.g., "9:00 AM", "19:00")
     */
    fun validateTimeFormat(time: String): ValidationFailure? {
        if (time.isBlank()) {
            return ValidationFailure.EmptyString("Time")
        }

        // Accept formats like "9:00 AM", "09:00", "19:00"
        val timeRegex = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9](\\s?(AM|PM|am|pm))?$".toRegex()
        return if (!time.matches(timeRegex)) {
            ValidationFailure.InvalidFormat(
                "Time",
                "HH:MM or HH:MM AM/PM (e.g., 9:00 AM, 19:00)"
            )
        } else null
    }
}
