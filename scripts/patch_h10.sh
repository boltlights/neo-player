#!/bin/bash
sed -i 's/val match = Regex("""^(\\d+)(?:\[\\s.\\-_|:\]|$)""").find(trimmed)/val match = LEADING_NUMBER_REGEX.find(trimmed)/g' app/src/main/java/com/example/util/NaturalOrderComparator.kt
sed -i 's/object NaturalOrderComparator : Comparator<String> {/object NaturalOrderComparator : Comparator<String> {\n    private val LEADING_NUMBER_REGEX = Regex("""^(\\d+)(?:[\\s.\\-_|:]|$)""")/g' app/src/main/java/com/example/util/NaturalOrderComparator.kt
