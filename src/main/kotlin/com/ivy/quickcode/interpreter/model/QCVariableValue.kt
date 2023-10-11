package com.ivy.quickcode.interpreter.model

sealed interface QCVariableValue {
    data class Str(val value: String) : QCVariableValue
    data class Bool(val value: Boolean) : QCVariableValue
}