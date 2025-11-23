package com.cop.app.headcounter.domain.models

enum class ItemCategory(val displayName: String, val icon: String) {
    ELECTRONICS("Electronics", "devices"),
    CLOTHING("Clothing", "checkroom"),
    DOCUMENTS("Documents", "description"),
    ACCESSORIES("Accessories", "watch"),
    BAGS("Bags & Luggage", "work"),
    PERSONAL_ITEMS("Personal Items", "person"),
    KEYS("Keys", "key"),
    WALLETS("Wallets", "account_balance_wallet"),
    JEWELRY("Jewelry", "diamond"),
    TOYS("Toys", "toys"),
    BOOKS("Books & Media", "menu_book"),
    SPORTS_EQUIPMENT("Sports Equipment", "sports"),
    OTHER("Other", "category");

    companion object {
        fun fromString(value: String): ItemCategory {
            return entries.find { it.name == value } ?: OTHER
        }
    }
}
