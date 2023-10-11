package com.ivy.quickcode

import arrow.core.Either
import arrow.core.raise.either
import com.ivy.quickcode.interpreter.QuickCodeInterpreter
import com.ivy.quickcode.interpreter.model.QCVariable
import com.ivy.quickcode.interpreter.model.QCVariableValue
import com.ivy.quickcode.lexer.QuickCodeLexer
import com.ivy.quickcode.parser.QuickCodeParser
import com.ivy.quickcode.parser.model.IfStatement
import com.ivy.quickcode.parser.model.QuickCodeAst
import com.ivy.quickcode.parser.model.RawText
import com.ivy.quickcode.parser.model.Variable

class QuickCodeCompiler {
    private val lexer = QuickCodeLexer()
    private val parser = QuickCodeParser()

    fun execute(
        codeTemplate: String,
        vars: Map<String, QCVariableValue>
    ): Either<String, String> = either {
        val tokens = lexer.tokenize(codeTemplate)
        val ast = parser.parse(tokens).bind()
        val interpreter = QuickCodeInterpreter(vars)
        interpreter.evaluate(ast)
    }

    fun compile(codeTemplate: String): Either<String, CompilationOutput> = either {
        val tokens = lexer.tokenize(codeTemplate)
        val ast = parser.parse(tokens).bind()
        val variables = ast.extractAllVars().fixVariableConflicts()
        CompilationOutput(ast, variables)
    }

    private fun List<QCVariable>.fixVariableConflicts(): List<QCVariable> {
        val namesSet = mutableSetOf<String>()
        val res = mutableListOf<QCVariable>()
        for (variable in this) {
            if (variable.name in namesSet) {
                // there's a conflict
                if (variable is QCVariable.Bool) {
                    // Bools have higher priority
                    res.remove(res.find { it.name == variable.name })
                    res.add(variable)
                }
            } else {
                res.add(variable)
            }
            namesSet.add(variable.name)
        }
        return res
    }

    private fun QuickCodeAst.extractAllVars(): List<QCVariable> {
        return when (this) {
            is QuickCodeAst.Begin -> emptyList()
            is IfStatement -> {
                condition.extractBoolVars() +
                        thenBranch.extractAllVars() +
                        (elseBranch?.extractAllVars() ?: emptyList())
            }

            is RawText -> emptyList()
            is Variable -> {
                listOf(QCVariable.Str(name))
            }
        } + (next?.extractAllVars() ?: emptyList())
    }

    private fun IfStatement.Condition.extractBoolVars(): List<QCVariable.Bool> {
        return when (this) {
            is IfStatement.Condition.And -> {
                cond1.extractBoolVars() + cond2.extractBoolVars()
            }

            is IfStatement.Condition.BoolVar -> {
                listOf(QCVariable.Bool(name))
            }

            is IfStatement.Condition.Brackets -> {
                cond.extractBoolVars()
            }

            is IfStatement.Condition.Not -> {
                cond.extractBoolVars()
            }

            is IfStatement.Condition.Or -> {
                cond1.extractBoolVars() + cond2.extractBoolVars()
            }
        }
    }

    data class CompilationOutput(
        val ast: QuickCodeAst,
        val variables: List<QCVariable>
    )
}