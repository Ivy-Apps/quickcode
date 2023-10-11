package com.ivy.quickcode.parser

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.ivy.quickcode.lexer.model.QuickCodeToken
import com.ivy.quickcode.parser.model.IfStatement
import com.ivy.quickcode.parser.model.IfStatement.Condition
import com.ivy.quickcode.parser.model.QuickCodeAst
import com.ivy.quickcode.parser.model.RawText
import com.ivy.quickcode.parser.model.Variable

class QuickCodeParser {
    fun parse(
        tokens: List<QuickCodeToken>
    ): Either<String, QuickCodeAst> = either {
        val scope = QCParserScope<QuickCodeAst>(tokens, initialPosition = 0)
        with(scope) { parseInternal() }
    }

    context(Raise<String>, QCParserScope<QuickCodeAst>)
    private fun parseInternal(): QuickCodeAst.Begin {
        val ast = AstBuilder().apply {
            var currentToken = consumeToken()
            while (currentToken != null) {
                parseToken(this, currentToken)
                currentToken = consumeToken()
            }
        }
        return ast.begin
    }

    context(Raise<String>)
    private fun QCParserScope<QuickCodeAst>.parseToken(
        astBuilder: AstBuilder,
        token: QuickCodeToken,
    ) {
        when (token) {
            QuickCodeToken.If -> {
                parseIfStatement(astBuilder)
            }

            is QuickCodeToken.RawText -> {
                astBuilder.addNode(RawText(token.text))
            }

            is QuickCodeToken.Variable -> {
                astBuilder.addNode(Variable(token.name))
            }

            else -> {
                raise("Unexpected token: $token, next token is ${consumeToken()}")
            }
        }
    }

    context(Raise<String>)
    private fun QCParserScope<QuickCodeAst>.parseIfStatement(
        ast: AstBuilder
    ) {
        val condition = parseIfCondition()

        val thenAst = AstBuilder()
        val thenEnd = parseUntil(
            ast = thenAst,
            end = listOf(QuickCodeToken.ElseIf, QuickCodeToken.Else, QuickCodeToken.EndIf)
        )

        val elseAst = when (thenEnd) {
            QuickCodeToken.ElseIf -> AstBuilder().apply {
                parseIfStatement(this)
            }

            QuickCodeToken.Else -> AstBuilder().apply {
                parseUntil(
                    ast = this,
                    end = listOf(QuickCodeToken.EndIf)
                )
            }

            else -> null
        }

        val ifStm = IfStatement(
            condition = condition,
            thenBranch = thenAst.begin,
            elseBranch = elseAst?.begin,
        )
        ast.addNode(ifStm)
    }

    context(Raise<String>)
    private fun QCParserScope<QuickCodeAst>.parseUntil(
        ast: AstBuilder,
        end: List<QuickCodeToken>
    ): QuickCodeToken {
        while (true) {
            val token = consumeToken()
            ensureNotNull(token) {
                "Uncompleted if branch. It must end with any ${end.joinToString(", ")}."
            }
            if (token in end) {
                return token
            }
            parseToken(ast, token)
        }
    }

    context(Raise<String>)
    private fun QCParserScope<QuickCodeAst>.parseIfCondition(
    ): Condition {
        val parser = QuickCodeIfConditionParser(tokens)
        val (condition, newPos) = ensureNotNull(parser.parse(position)) {
            """
                Invalid if condition! At '${locationDescription()}'.
                Check for errors in the variables like '{' instead of '{{'.
            """.trimIndent()
        }
        changePosition(newPos)
        return condition
    }

    private data class AstBuilder(
        val begin: QuickCodeAst.Begin = QuickCodeAst.Begin(),
        private var current: QuickCodeAst = begin
    ) {
        fun addNode(node: QuickCodeAst) {
            current.next = node
            current = node
        }
    }
}