package com.ivy.quickcode.data

sealed interface QCVariableValue {
    data class Str(val value: String) : QCVariableValue
    data class Bool(val value: Boolean) : QCVariableValue
}