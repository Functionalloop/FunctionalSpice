package com.functional.spice.domain.model

enum class Category(val displayName: String) {
    ALL("All"),
    HOT_TEA("Hot tea"),
    SNACKS("Snacks"),
    COLD_DRINKS("Cold drinks");

    companion object {
        /** Categories an item can belong to (everything except the ALL filter). */
        val menuCategories: List<Category> get() = entries.filter { it != ALL }

        fun fromString(value: String): Category {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: ALL
        }
    }
}
