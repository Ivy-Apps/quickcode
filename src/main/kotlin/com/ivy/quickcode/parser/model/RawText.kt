package com.ivy.quickcode.parser.model

data class RawText(
    val text: String,
    override var next: QuickCodeAst? = null
) : QuickCodeAst