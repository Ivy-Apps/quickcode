package com.ivy.quickcode.lexer

class QuickCodeLexer {
    fun tokenize(text: String): List<Token> {
        val scope = LexerState(
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


    private fun LexerState.parserRules(): List<() -> Boolean> {
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

    private fun LexerState.parse(
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

    private fun LexerState.variable(): Boolean = parseToken(
        condition = !isInsideIfCondition,
        syntax = Token.Variable.syntax,
    ) {
        Token.Variable(name = it.trim())
    }

    private fun LexerState.ifCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = Token.If.syntax,
    ) {
        isInsideIfCondition = true
        Token.If
    }

    private fun LexerState.ifOpenBracket() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.OpenBracket.syntax,
    ) {
        Token.IfExpression.OpenBracket
    }

    private fun LexerState.ifCloseBracket() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.CloseBracket.syntax,
    ) {
        Token.IfExpression.CloseBracket
    }

    private fun LexerState.ifNot() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.Not.syntax,
    ) {
        Token.IfExpression.Not
    }

    private fun LexerState.ifAndCond() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.And.syntax,
    ) {
        Token.IfExpression.And
    }

    private fun LexerState.ifOrCond() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.Or.syntax,
    ) {
        Token.IfExpression.Or
    }

    private fun LexerState.ifBoolVar() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.IfExpression.BoolVariable.syntax,
    ) {
        Token.IfExpression.BoolVariable(name = it.trim())
    }

    private fun LexerState.thenCond() = parseToken(
        condition = isInsideIfCondition,
        syntax = Token.Then.syntax,
    ) {
        isInsideIfCondition = false
        Token.Then
    }

    private fun LexerState.elseIfCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = Token.ElseIf.syntax
    ) {
        isInsideIfCondition = true
        Token.ElseIf
    }

    private fun LexerState.elseCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = Token.Else.syntax
    ) {
        Token.Else
    }

    private fun LexerState.endIfCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = Token.EndIf.syntax,
    ) {
        Token.EndIf
    }

    private fun LexerState.parseToken(
        condition: Boolean,
        syntax: TokenSyntax,
        createToken: LexerState.(content: String) -> Token,
    ): Boolean {
        if (!condition) return false
        if (!text.startsWith(prefix = syntax.tag, startIndex = position)) {
            // start tag not present, do nothing
            return false
        }

        // Start tag present

        if (syntax.endTag != null) {
            // need to check if the end tag is there
            val endTagIndex = text.indexOfOrNull(
                string = syntax.endTag,
                startIndex = position + syntax.tag.length
            )
            if (endTagIndex != null) {
                // get the text between the start and end tags
                // e.g. {{ content here }}
                val content = text.substring(
                    startIndex = position + syntax.tag.length,
                    endIndex = endTagIndex,
                )
                tokens.add(createToken(content))
                position = endTagIndex + syntax.endTag.length
                return true
            }
        } else {
            // no end tag needed, add the token
            tokens.add(createToken(""))
            position += syntax.tag.length
            return true
        }

        return false
    }

    private fun String.indexOfOrNull(string: String, startIndex: Int): Int? =
        indexOf(string, startIndex).takeIf { it != -1 }

    private data class LexerState(
        val text: String,
        var isInsideIfCondition: Boolean,
        var prevPosition: Int,
        var position: Int,
        val tokens: MutableList<Token>
    )
}
