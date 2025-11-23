package com.eventmonitor.core.domain.models

/**
 * Temporary type aliases for backward compatibility during refactoring.
 * These allow the code to compile while we transition from church-specific
 * terminology to generic event terminology.
 *
 * TODO: Remove these aliases once all references are updated.
 */

// ServiceType → EventType
typealias ServiceType = EventType

// AreaType → ZoneType
typealias AreaType = ZoneType
