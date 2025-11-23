package com.eventmonitor.core.domain.common

/**
 * Sealed class representing all possible errors in the application
 */
sealed class AppError(open val message: String) {

    // Validation Errors
    data class ValidationError(val errors: List<ValidationFailure>) : AppError(
        errors.joinToString(", ") { it.message }
    ) {
        constructor(vararg error: ValidationFailure) : this(error.toList())
    }

    // Database Errors
    data class NotFound(val resource: String, val id: String) : AppError(
        "$resource with id '$id' not found"
    )

    data class AlreadyExists(val resource: String, val field: String, val value: String) : AppError(
        "$resource with $field '$value' already exists"
    )

    data class DatabaseError(override val message: String) : AppError(message)

    data class ConstraintViolation(val constraint: String, override val message: String) : AppError(message)

    // Business Logic Errors
    data class ServiceLocked(val serviceId: String) : AppError(
        "Service is locked and cannot be modified"
    )

    data class InvalidOperation(override val message: String) : AppError(message)

    data class HasDependencies(
        val resource: String,
        val dependencyCount: Int,
        val dependencyType: String
    ) : AppError(
        "Cannot delete $resource because it has $dependencyCount associated $dependencyType"
    )

    // Network/Sync Errors
    data class NetworkError(override val message: String) : AppError(message)

    data class SyncError(override val message: String) : AppError(message)

    // Unknown/Generic Errors
    data class UnknownError(override val message: String) : AppError(message)

    /**
     * Convert to user-friendly message
     */
    fun toUserMessage(): String = when (this) {
        is ValidationError -> "Please fix the following:\n${errors.joinToString("\n") { "• ${it.message}" }}"
        is NotFound -> "The requested $resource was not found."
        is AlreadyExists -> "A $resource with that $field already exists."
        is DatabaseError -> "A database error occurred. Please try again."
        is ConstraintViolation -> message
        is ServiceLocked -> "This service is locked and cannot be changed."
        is InvalidOperation -> message
        is HasDependencies -> "Cannot delete because it's being used by $dependencyCount $dependencyType."
        is NetworkError -> "Network error: $message"
        is SyncError -> "Sync error: $message"
        is UnknownError -> "An unexpected error occurred: $message"
    }
}

/**
 * Represents a single validation failure
 */
sealed class ValidationFailure(val field: String, val message: String) {

    // Required field validations
    data class RequiredField(val fieldName: String) : ValidationFailure(
        fieldName,
        "$fieldName is required"
    )

    // String validations
    data class EmptyString(val fieldName: String) : ValidationFailure(
        fieldName,
        "$fieldName cannot be empty"
    )

    data class TooShort(val fieldName: String, val minLength: Int) : ValidationFailure(
        fieldName,
        "$fieldName must be at least $minLength characters"
    )

    data class TooLong(val fieldName: String, val maxLength: Int) : ValidationFailure(
        fieldName,
        "$fieldName cannot exceed $maxLength characters"
    )

    data class InvalidFormat(val fieldName: String, val expected: String) : ValidationFailure(
        fieldName,
        "$fieldName has invalid format. Expected: $expected"
    )

    // Number validations
    data class NegativeNumber(val fieldName: String) : ValidationFailure(
        fieldName,
        "$fieldName cannot be negative"
    )

    data class ZeroValue(val fieldName: String) : ValidationFailure(
        fieldName,
        "$fieldName cannot be zero"
    )

    data class TooSmall(val fieldName: String, val minimum: Int) : ValidationFailure(
        fieldName,
        "$fieldName must be at least $minimum"
    )

    data class TooLarge(val fieldName: String, val maximum: Int) : ValidationFailure(
        fieldName,
        "$fieldName cannot exceed $maximum"
    )

    // Date validations
    data class FutureDate(val fieldName: String) : ValidationFailure(
        fieldName,
        "$fieldName cannot be in the future"
    )

    data class PastDate(val fieldName: String) : ValidationFailure(
        fieldName,
        "$fieldName cannot be in the past"
    )

    data class InvalidDateRange(val fieldName: String) : ValidationFailure(
        fieldName,
        "End date must be after start date"
    )

    // Business logic validations
    data class DuplicateValue(val fieldName: String, val value: String) : ValidationFailure(
        fieldName,
        "$fieldName '$value' already exists"
    )

    data class InvalidReference(val fieldName: String, val reference: String) : ValidationFailure(
        fieldName,
        "Invalid $fieldName: $reference does not exist"
    )

    // Custom validation
    data class Custom(val fieldName: String, val errorMessage: String) : ValidationFailure(
        fieldName,
        errorMessage
    )
}
