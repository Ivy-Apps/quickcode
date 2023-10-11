package com.ivy.quickcode.data

sealed interface QuickCodeAst {
    var next: QuickCodeAst?

    data class Begin(override var next: QuickCodeAst? = null) : QuickCodeAst
}

data class RawText(
    val text: String,
    override var next: QuickCodeAst? = null
) : QuickCodeAst

data class Variable(
    val name: String,
    override var next: QuickCodeAst? = null
) : QuickCodeAst

data class IfStatement(
    val condition: Condition,
    val thenBranch: QuickCodeAst,
    val elseBranch: QuickCodeAst? = null,
    override var next: QuickCodeAst? = null
) : QuickCodeAst {
    sealed interface Condition {
        data class BoolVar(val name: String) : Condition
        data class Not(val cond: Condition) : Condition
        data class Brackets(val cond: Condition) : Condition
        data class And(val cond1: Condition, val cond2: Condition) : Condition
        data class Or(val cond1: Condition, val cond2: Condition) : Condition
    }
}