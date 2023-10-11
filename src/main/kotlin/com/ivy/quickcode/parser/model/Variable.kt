package com.ivy.quickcode.parser.model

data class Variable(
    val name: String,
    override var next: QuickCodeAst? = null
) : QuickCodeAst