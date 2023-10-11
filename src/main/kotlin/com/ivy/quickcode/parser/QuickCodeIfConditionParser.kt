package com.ivy.quickcode.parser

import com.ivy.quickcode.data.IfStatement
import com.ivy.quickcode.lexer.Token

typealias IfCondParserScope = QCParserScope<IfStatement.Condition>

class QuickCodeIfConditionParser(
    private val tokens: List<Token>,
) {
    fun parse(
        position: Int
    ): Pair<IfStatement.Condition, Int>? {
        val scope = IfCondParserScope(tokens, position)
        val condition = scope.condition() ?: return null
        if (scope.currentToken() == Token.Then) {
            scope.consumeToken()
        }
        return condition to scope.position
    }

    private fun IfCondParserScope.condition(): IfStatement.Condition? {
        return or(
            a = { andExpr() },
            b = {
                or(
                    a = { orExpr() },
                    b = { term() },
                )
            }
        )
    }

    private fun IfCondParserScope.brackets(): IfStatement.Condition.Brackets? {
        if (consumeToken() !is Token.IfExpression.OpenBracket) return null
        val cond = condition() ?: return null
        if (consumeToken() !is Token.IfExpression.CloseBracket) return null
        return IfStatement.Condition.Brackets(cond)
    }

    private fun IfCondParserScope.andExpr(): IfStatement.Condition.And? {
        val cond1 = term() ?: return null
        if (consumeToken() !is Token.IfExpression.And) return null
        val cond2 = condition() ?: return null
        return IfStatement.Condition.And(
            cond1 = cond1,
            cond2 = cond2
        )
    }

    private fun IfCondParserScope.orExpr(): IfStatement.Condition.Or? {
        val cond1 = term() ?: return null
        if (consumeToken() !is Token.IfExpression.Or) return null
        val cond2 = condition() ?: return null
        return IfStatement.Condition.Or(
            cond1 = cond1,
            cond2 = cond2
        )
    }

    private fun IfCondParserScope.notExpr(): IfStatement.Condition.Not? {
        if (consumeToken() !is Token.IfExpression.Not) return null
        val cond = condition() ?: return null
        return IfStatement.Condition.Not(cond)
    }

    private fun IfCondParserScope.boolVar(): IfStatement.Condition.BoolVar? {
        return (consumeToken() as? Token.IfExpression.BoolVariable)?.let {
            IfStatement.Condition.BoolVar(it.name)
        }
    }

    private fun IfCondParserScope.term(): IfStatement.Condition? {
        return or(
            a = { brackets() },
            b = {
                or(
                    a = { notExpr() },
                    b = { boolVar() }
                )
            }
        )
    }
}