package com.ivy.quickcode.data

sealed interface QCVariable {
    val name: String

    data class Str(override val name: String) : QCVariable
    data class Bool(override val name: String) : QCVariable
}