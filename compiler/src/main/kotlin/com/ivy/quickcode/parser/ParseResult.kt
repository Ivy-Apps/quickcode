package com.ivy.quickcode.parser

import com.ivy.quickcode.data.QuickCodeAst

sealed interface ParseResult {
    data class Success(
        val ast: QuickCodeAst
    ) : ParseResult

    data class Failure(
        val errorMsg: String
    ) : ParseResult
}