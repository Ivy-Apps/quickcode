package com.ivy.quickcode.parser

import com.ivy.quickcode.lexer.Token

class QCParserScope<T>(
    val tokens: List<Token>,
    initialPosition: Int,
) {
    var position = initialPosition
        private set

    fun or(
        a: QCParserScope<T>.() -> T?,
        b: QCParserScope<T>.() -> T?,
    ): T? {
        val aScope = QCParserScope<T>(tokens, position)
        val resA = aScope.a()
        if (resA != null) {
            position = aScope.position
            return resA
        }

        val bScope = QCParserScope<T>(tokens, position)
        val resB = bScope.b()
        if (resB != null) {
            position = bScope.position
            return resB
        }

        return null
    }

    fun consumeToken(): Token? {
        return tokens.getOrNull(position++).run {
            if (this is Token.Then) null else this
        }
    }

    fun locationDescription(): String {
        return buildString {
            prevToken()?.let {
                append(it.toString())
                append(" ")
            }
            currentToken()?.let {
                append(it.toString())
                append(" ")
            }
            nextToken()?.let {
                append(it.toString())
                append(" ")
            }
        }
    }

    fun prevToken(): Token? {
        return tokens.getOrNull(position - 1)
    }

    fun currentToken(): Token? {
        return tokens.getOrNull(position)
    }

    fun nextToken(): Token? {
        return tokens.getOrNull(position + 1)
    }

    fun changePosition(position: Int) {
        this.position = position
    }

}