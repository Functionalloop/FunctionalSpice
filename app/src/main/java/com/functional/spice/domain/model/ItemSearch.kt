package com.functional.spice.domain.model

import java.util.Locale

/**
 * Menu search used by the Counter and the Menu manager.
 *
 * Case-insensitive; every typed word must appear somewhere in the item's name or Konkani
 * subtitle, in any order ("chai cut" finds "Cutting chai"). Items whose name starts with the
 * query come first; otherwise the menu order is kept.
 */
object ItemSearch {

    private fun words(query: String): List<String> =
        query.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotEmpty() }

    fun matches(item: MenuItem, query: String): Boolean {
        val terms = words(query)
        if (terms.isEmpty()) return true
        val haystack = "${item.name} ${item.konkaniSubtitle}".lowercase(Locale.ROOT)
        return terms.all { it in haystack }
    }

    fun filter(items: List<MenuItem>, query: String): List<MenuItem> {
        if (query.isBlank()) return items
        val q = query.trim().lowercase(Locale.ROOT)
        // sortedBy is stable, so the menu order is kept within each group
        return items.filter { matches(it, query) }
            .sortedBy { if (it.name.lowercase(Locale.ROOT).startsWith(q)) 0 else 1 }
    }
}
