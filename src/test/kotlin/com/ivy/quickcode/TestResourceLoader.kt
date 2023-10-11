package com.ivy.quickcode

import io.kotest.core.spec.style.FreeSpec

fun FreeSpec.loadFile(resourceName: String): String {
    return this::class.java.classLoader.getResourceAsStream(resourceName)?.use {
        it.reader().readText()
    } ?: throw IllegalArgumentException("Resource not found: $resourceName")
}
