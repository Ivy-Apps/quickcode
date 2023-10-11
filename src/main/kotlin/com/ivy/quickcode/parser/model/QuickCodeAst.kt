package com.ivy.quickcode.parser.model

sealed interface QuickCodeAst {
    var next: QuickCodeAst?

    data class Begin(override var next: QuickCodeAst? = null) : QuickCodeAst
}
