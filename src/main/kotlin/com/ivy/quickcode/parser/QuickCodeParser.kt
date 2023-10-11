package com.ivy.quickcode.parser

import com.ivy.quickcode.data.IfStatement
import com.ivy.quickcode.data.IfStatement.Condition
import com.ivy.quickcode.data.QuickCodeAst
import com.ivy.quickcode.data.RawText
import com.ivy.quickcode.data.Variable
import com.ivy.quickcode.lexer.Token

class QuickCodeParser {
    fun parse(tokens: List<Token>): ParseResult {
        return try {
            ParseResult.Success(parseInternal(tokens))
        } catch (e: Exception) {
            ParseResult.Failure(e.message ?: "unknown error")
        }
    }

    private fun parseInternal(
        tokens: List<Token>,
    ): QuickCodeAst.Begin {
        val astBuilder = AstBuilder()
        val parserScope = QCParserScope<QuickCodeAst>(tokens, initialPosition = 0)

        with(parserScope) {
            var currentToken = consumeToken()
            while (currentToken != null) {
                parseToken(astBuilder, currentToken)
                currentToken = consumeToken()
            }
        }
        return astBuilder.begin
    }


    private fun QCParserScope<QuickCodeAst>.parseToken(
        astBuilder: AstBuilder,
        token: Token,
    ) {
        when (token) {
            Token.If -> {
                parseIfStatement(astBuilder)
            }

            is Token.RawText -> {
                astBuilder.addNode(RawText(token.text))
            }

            is Token.Variable -> {
                astBuilder.addNode(Variable(token.name))
            }

            else -> {
                error("Unexpected token: $token, next token is ${consumeToken()}")
            }
        }
    }

    private fun QCParserScope<QuickCodeAst>.parseIfStatement(
        ast: AstBuilder
    ) {
        val condition = parseIfCondition()

        val thenAst = AstBuilder()
        val thenEnd = parseUntil(
            ast = thenAst,
            end = listOf(Token.ElseIf, Token.Else, Token.EndIf)
        )

        val elseAst = when (thenEnd) {
            Token.ElseIf -> AstBuilder().apply {
                parseIfStatement(this)
            }

            Token.Else -> AstBuilder().apply {
                parseUntil(
                    ast = this,
                    end = listOf(Token.EndIf)
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

    private fun QCParserScope<QuickCodeAst>.parseUntil(
        ast: AstBuilder,
        end: List<Token>
    ): Token {
        while (true) {
            val token = consumeToken()
            requireNotNull(token) {
                "Uncompleted if branch. It must end with any ${end.joinToString(", ")}."
            }
            if (token in end) {
                return token
            }
            parseToken(ast, token)
        }
    }

    private fun QCParserScope<QuickCodeAst>.parseIfCondition(
    ): Condition {
        val parser = QuickCodeIfConditionParser(tokens)
        val (condition, newPos) = requireNotNull(parser.parse(position)) {
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

