package com.ivy.quickcode.lexer

class QuickCodeLexer {
    fun tokenize(text: String): List<Token> {
        val state = LexerState(
            text = text,
            isInsideIfCondition = false,
            position = 0,
            prevPosition = 0,
            tokens = mutableListOf(),
        )
        val rules = state.parserRules()
        // parse until the end of the text is reached
        while (state.position < state.text.length) {
            state.parseStep(rules)
        }
        return state.tokens
            .filterEmptyRawTexts()
            .concatConsecutiveRawTexts()
            .beautifyRawTexts()
    }

    private fun LexerState.parserRules(): List<() -> Boolean> {
        // Note: rules must be sorted by priority
        // the ones at top have precedence
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

    private fun LexerState.parseStep(
        rules: List<() -> Boolean>
    ) {
        // Try all rules, if none succeeds assume then it's default
        for (rule in rules) {
            val succeed = rule.invoke()
            if (succeed) {
                // parsed special syntax
                // finish the parse step
                return
            }
        }

        // default case: raw text
        // no rules have succeeded
        val nextSpecialCharIndex = listOf(
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
            tokens.add(Token.RawText(text.substring(position, nextSpecialCharIndex)))
        }
        prevPosition = position
        position = nextSpecialCharIndex

        if (position == prevPosition) {
            // Cycle!!
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

    /**
     * Represents the current state of the Lexer.
     * @param text an immutable representation of the text being parsed
     * @param isInsideIfCondition mutable flag indicating whether we're
     * inside #if {{condition}} #then condition
     * @param position the current position up to which the [text] is parsed
     * @param prevPosition the previous [position], used to prevent cycles
     * @param tokens mutable list containing all parsed tokens
     */
    private data class LexerState(
        val text: String,
        var isInsideIfCondition: Boolean,
        var position: Int,
        var prevPosition: Int,
        val tokens: MutableList<Token>
    )

    private fun List<Token>.filterEmptyRawTexts(): List<Token> {
        return this.filter {
            // filter empty RawText tokens
            it !is Token.RawText || it.text.isNotEmpty()
        }
    }

    private fun List<Token>.concatConsecutiveRawTexts(): List<Token> {
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
            if (this[i] == ' ') {
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
}
