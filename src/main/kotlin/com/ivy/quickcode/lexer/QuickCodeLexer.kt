package com.ivy.quickcode.lexer

class QuickCodeLexer {
    fun tokenize(text: String): List<Token> {
        val scope = LexerScope(
            text = text,
            isInsideIfCondition = false,
            position = 0,
            prevPosition = 0,
            tokens = mutableListOf(),
        )
        val rules = scope.parserRules()
        while (scope.position < scope.text.length) {
            scope.parse(rules)
        }
        return scope.tokens.filter {
            // filter empty RawText tokens
            it !is Token.RawText || it.text.isNotEmpty()
        }.concatRawTexts()
            .beautifyRawTexts()
    }

    private fun List<Token>.concatRawTexts(): List<Token> {
        val original = this
        val res = mutableListOf<Token>()
        var i = 0
        while (i < original.size) {
            val current = original[i]
            if (current is Token.RawText) {
                val combinedRawText = buildString {
                    append(current.text)
                    var next: Token?
                    do {
                        next = original.getOrNull(i + 1)
                        if (next is Token.RawText) {
                            append(next.text)
                            i++
                        } else {
                            break
                        }
                    } while (true)
                }
                res.add(Token.RawText(combinedRawText))
            } else {
                res.add(current)
            }
            i++
        }

        return res
    }

    private fun List<Token>.beautifyRawTexts(): List<Token> {
        return mapIndexed { index, item ->
            if (item is Token.RawText) {
                // previous item
                when (getOrNull(index - 1)) {
                    in listOf(Token.Then, Token.Else) -> {
                        item.copy(
                            text = item.text.dropUnnecessaryWhiteSpace()
                        )
                    }

                    Token.EndIf -> {
                        item.copy(
                            text = item.text.dropUnnecessaryWhiteSpace()
                        )
                    }

                    else -> item
                }
            } else item
        }
    }

    private fun String.dropUnnecessaryWhiteSpace(): String {
        var spacesToDrop = 0
        for (i in indices) {
            if (get(i) == ' ') {
                spacesToDrop++
            } else {
                break
            }
        }
        val cleared = this.drop(spacesToDrop)
        return when {
            cleared.startsWith("\n") -> cleared.drop(1)
            else -> cleared
        }
    }


    private fun LexerScope.parserRules(): List<() -> Boolean> {
        return listOf(
            { variable() },
            { ifCond() },
            { ifOpenBracket() },
            { ifCloseBracket() },
            { ifNot() },
            { ifBoolVar() },
            { ifAndCond() },
            { ifOrCond() },
            { thenCond() },
            { elseIfCond() },
            { elseCond() },
            { endIfCond() },
        )
    }

    private fun LexerScope.parse(
        rules: List<() -> Boolean>
    ) {
        for (rule in rules) {
            if (rule()) {
                // parsed something special
                return
            }
        }

        // default case: raw text
        val nextSpecialChar = listOf(
            Token.Variable.syntax,
            Token.If.syntax,
            Token.IfExpression.OpenBracket.syntax,
            Token.IfExpression.CloseBracket.syntax,
            Token.IfExpression.And.syntax,
            Token.IfExpression.Or.syntax,
            Token.IfExpression.Not.syntax,
            Token.IfExpression.BoolVariable.syntax,
            Token.Then.syntax,
            Token.Else.syntax,
            Token.EndIf.syntax,
        ).mapNotNull { syntax ->
            text.indexOfOrNull(syntax.tag, position)
        }.minOrNull() ?: text.length
        if (!isInsideIfCondition) {
            tokens.add(Token.RawText(text.substring(position, nextSpecialChar)))
        }
        prevPosition = position
        position = nextSpecialChar
        if (position == prevPosition) {
            /*
            Special case wasn't parsed successfully (probably not satisfied condition).
            Consider this char as a RawText
             */
            tokens.add(Token.RawText("${text[position]}"))
            position++
        }
    }

    private fun LexerScope.variable(): Boolean = parseToken(
        condition = !isInsideIfCondition,
        syntax = Token.Variable.syntax,
    ) {
        Token.Variable(name = it.trim())
    }

    private fun LexerScope.ifCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = Token.If.syntax,
    ) {
        isInsideIfCondition = true
        Token.If
    }

    private fun LexerScope.ifOpenBracket() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.OpenBracket.syntax,
    ) {
        Token.IfExpression.OpenBracket
    }

    private fun LexerScope.ifCloseBracket() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.CloseBracket.syntax,
    ) {
        Token.IfExpression.CloseBracket
    }

    private fun LexerScope.ifNot() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.Not.syntax,
    ) {
        Token.IfExpression.Not
    }

    private fun LexerScope.ifAndCond() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.And.syntax,
    ) {
        Token.IfExpression.And
    }

    private fun LexerScope.ifOrCond() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.Or.syntax,
    ) {
        Token.IfExpression.Or
    }

    private fun LexerScope.ifBoolVar() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.BoolVariable.syntax,
    ) {
        Token.IfExpression.BoolVariable(name = it.trim())
    }

    private fun LexerScope.thenCond() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.Then.syntax,
    ) {
        isInsideIfCondition = false
        Token.Then
    }

    private fun LexerScope.elseIfCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = Token.ElseIf.syntax
    ) {
        isInsideIfCondition = true
        Token.ElseIf
    }

    private fun LexerScope.elseCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = Token.Else.syntax
    ) {
        Token.Else
    }

    private fun LexerScope.endIfCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = Token.EndIf.syntax,
    ) {
        Token.EndIf
    }

    private fun LexerScope.parseToken(
        condition: Boolean,
        syntax: TokenSyntax,
        onParseToken: LexerScope.(text: String) -> Token?,
    ): Boolean {
        if (condition && text.startsWith(prefix = syntax.tag, startIndex = position)) {
            if (syntax.endTag == null) {
                onParseToken("")?.let(tokens::add)
                position += syntax.tag.length
                return true
            } else {
                val endIndex = text.indexOfOrNull(
                    string = syntax.endTag,
                    startIndex = position + syntax.tag.length
                )
                if (endIndex != null) {
                    onParseToken(
                        text.substring(
                            startIndex = position + syntax.tag.length,
                            endIndex = endIndex,
                        )
                    )?.let(tokens::add)
                    position = endIndex + syntax.endTag.length
                    return true
                }
            }
        }
        return false
    }

    private fun String.indexOfOrNull(string: String, startIndex: Int): Int? =
        indexOf(string, startIndex).takeIf { it != -1 }

    private data class LexerScope(
        val text: String,
        var isInsideIfCondition: Boolean,
        var prevPosition: Int,
        var position: Int,
        val tokens: MutableList<Token>
    )
}
