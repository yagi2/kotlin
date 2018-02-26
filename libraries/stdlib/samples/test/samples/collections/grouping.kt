/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package samples.collections

import samples.*
import kotlin.test.*

class Grouping {
    @Sample
    fun aggregateEvenAndOdd() {
        val intList = listOf(0, 1, 2, 3, 4, 5, 6)

        // to multiply even number by 10
        val aggregated = intList.groupingBy { it % 2 == 0 }.aggregate { key, acc: Int?, element, _ ->
            when (key) {
                true -> element * 10 + (acc ?: 0)
                else -> element + (acc ?: 0)
            }
        }

        assertPrints(aggregated, "{true=120, false=9}")
    }

    @Sample
    fun aggregateEvenAndOddToMap() {
        val intList = listOf(0, 1, 2, 3, 4, 5, 6)
        val aggregated = mutableMapOf<Boolean, Int>()

        // to multiply even number by 10
        intList.groupingBy { it % 2 == 0 }.aggregateTo(aggregated, { key, acc: Int?, element, _ ->
            when (key) {
                true -> element * 10 + (acc ?: 0)
                else -> element + (acc ?: 0)
            }
        })

        assertTrue(aggregated[true] == 120)
        assertTrue(aggregated[false] == 9)
    }

    @Sample
    fun foldWithComputedInitialValue() {

    }

    @Sample
    fun foldWithComputedInitialValueToMap() {

    }

    @Sample
    fun foldByEvenLengthStringWithConstantInitialValue() {
        val fruits = listOf("apple", "apricot", "banana", "blueberry", "cherry", "coconut")

        // collect only even length Strings
        val evenFruits = fruits.groupingBy { it.first() }
            .fold(listOf<String>()) { acc, e -> if (e.length % 2 == 0) acc + e else acc }

        assertPrints(evenFruits, "{a=[], b=[banana], c=[cherry]}")
    }

    @Sample
    fun foldByEvenLengthStringWithConstantInitialValueToMap() {
        val fruits = listOf("apple", "apricot", "banana", "blueberry", "cherry", "coconut")
        val evenFruits = mutableMapOf<Char, List<String>>()

        // collect only even length Strings
        fruits.groupingBy { it.first() }
            .foldTo(evenFruits ,listOf()) { acc, e -> if (e.length % 2 == 0) acc + e else acc }

        assertTrue(evenFruits['a'] == emptyList<String>())
        assertTrue(evenFruits['b'] == listOf("banana"))
        assertTrue(evenFruits['c'] == listOf("cherry"))
    }

    @Sample
    fun reduceByMaxOfContainsVowels() {
        val animals = listOf("raccoon", "reindeer", "cow", "camel", "giraffe", "goat")

        // grouping by first char and collect only max of contains vowels
        val maxVowels = animals.groupingBy { it.first() }
            .reduce { _, a, b ->
                if (a.count{ it in "aeiou" } >= b.count{ it in "aeiou" }) {
                    a
                } else {
                    b
                }
            }

        assertPrints(maxVowels, "{r=reindeer, c=camel, g=giraffe}")
    }

    @Sample
    fun reduceByMaxOfContainsVowelsToMap() {
        val animals = listOf("raccoon", "reindeer", "cow", "camel", "giraffe", "goat")
        val maxVowels = mutableMapOf<Char, String>()

        // grouping by first char and collect only max of contains vowels
        animals.groupingBy { it.first() }
            .reduceTo(maxVowels, { _, a, b ->
                if (a.count{ it in "aeiou" } >= b.count{ it in "aeiou" }) {
                    a
                } else {
                    b
                }
            })

        assertTrue(maxVowels['r'] == "reindeer")
        assertTrue(maxVowels['c'] == "camel")
        assertTrue(maxVowels['g'] == "giraffe")
    }
}