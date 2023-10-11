package com.ivy.quickcode.lexer

import com.ivy.quickcode.lexer.model.QuickCodeToken
import com.ivy.quickcode.lexer.model.TokenSyntax

/**
 * Transforms a text ([String]) into a list of [QuickCodeToken]
 * that can be processed by the parser.
 */
class QuickCodeLexer {
    /**
     * Transforms a [text] ([String]) into a list of [QuickCodeToken]
     * that can be processed by the parser.
     * @param text the QuickCode template being parsed
     */
    fun tokenize(text: String): List<QuickCodeToken> {
        val state = LexerState(
            text = text,
            isInsideIfCondition = false,
            position = 0,
            prevPosition = 0,
            tokens = mutableListOf(),
        )
        val syntaxRules = state.syntaxRules()
        // parse until the end of the text is reached
        while (state.position < state.text.length) {
            state.parseStep(syntaxRules)
        }
        return state.tokens
            .filterEmptyRawTexts()
            .mergeConsecutiveRawTexts()
            .beautifyRawTexts()
    }

    private fun LexerState.syntaxRules(): List<() -> Boolean> {
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
        syntaxRules: List<() -> Boolean>
    ) {
        // Try all rules, if none succeeds assume then it's default
        for (rule in syntaxRules) {
            val succeed = rule.invoke()
            if (succeed) {
                // parsed special syntax
                // finish the parse step
                return
            }
        }

        // default case: raw text
        // no special syntax detected
        val nextSpecialCharIndex = listOf(
            QuickCodeToken.Variable.syntax,
            QuickCodeToken.If.syntax,
            QuickCodeToken.IfExpression.OpenBracket.syntax,
            QuickCodeToken.IfExpression.CloseBracket.syntax,
            QuickCodeToken.IfExpression.And.syntax,
            QuickCodeToken.IfExpression.Or.syntax,
            QuickCodeToken.IfExpression.Not.syntax,
            QuickCodeToken.IfExpression.BoolVariable.syntax,
            QuickCodeToken.Then.syntax,
            QuickCodeToken.Else.syntax,
            QuickCodeToken.EndIf.syntax,
        ).mapNotNull { syntax ->
            text.indexOfOrNull(syntax.tag, position)
        }.minOrNull() ?: text.length

        if (!isInsideIfCondition) {
            // Raw text inside #if {{condition}} #then must be ignored
            tokens.add(QuickCodeToken.RawText(text.substring(position, nextSpecialCharIndex)))
        }

        prevPosition = position // used to prevent cycles
        position = nextSpecialCharIndex

        if (position == prevPosition) {
            // Cycle detected! Consume one char to break it.
            tokens.add(QuickCodeToken.RawText("${text[position]}"))
            position++
        }
    }

    private fun LexerState.variable(): Boolean = parseToken(
        condition = !isInsideIfCondition,
        syntax = QuickCodeToken.Variable.syntax,
    ) { content ->
        QuickCodeToken.Variable(name = content.trim())
    }

    private fun LexerState.ifCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = QuickCodeToken.If.syntax,
    ) {
        isInsideIfCondition = true
        QuickCodeToken.If
    }

    private fun LexerState.ifOpenBracket() = parseToken(
        condition = isInsideIfCondition,
        syntax = QuickCodeToken.IfExpression.OpenBracket.syntax,
    ) {
        QuickCodeToken.IfExpression.OpenBracket
    }

    private fun LexerState.ifCloseBracket() = parseToken(
        condition = isInsideIfCondition,
        syntax = QuickCodeToken.IfExpression.CloseBracket.syntax,
    ) {
        QuickCodeToken.IfExpression.CloseBracket
    }

    private fun LexerState.ifNot() = parseToken(
        condition = isInsideIfCondition,
        syntax = QuickCodeToken.IfExpression.Not.syntax,
    ) {
        QuickCodeToken.IfExpression.Not
    }

    private fun LexerState.ifAndCond() = parseToken(
        condition = isInsideIfCondition,
        syntax = QuickCodeToken.IfExpression.And.syntax,
    ) {
        QuickCodeToken.IfExpression.And
    }

    private fun LexerState.ifOrCond() = parseToken(
        condition = isInsideIfCondition,
        syntax = QuickCodeToken.IfExpression.Or.syntax,
    ) {
        QuickCodeToken.IfExpression.Or
    }

    private fun LexerState.ifBoolVar() = parseToken(
        condition = isInsideIfCondition,
        syntax = QuickCodeToken.IfExpression.BoolVariable.syntax,
    ) { content ->
        QuickCodeToken.IfExpression.BoolVariable(name = content.trim())
    }

    private fun LexerState.thenCond() = parseToken(
        condition = isInsideIfCondition,
        syntax = QuickCodeToken.Then.syntax,
    ) {
        isInsideIfCondition = false
        QuickCodeToken.Then
    }

    private fun LexerState.elseIfCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = QuickCodeToken.ElseIf.syntax
    ) {
        isInsideIfCondition = true
        QuickCodeToken.ElseIf
    }

    private fun LexerState.elseCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = QuickCodeToken.Else.syntax
    ) {
        QuickCodeToken.Else
    }

    private fun LexerState.endIfCond() = parseToken(
        condition = !isInsideIfCondition,
        syntax = QuickCodeToken.EndIf.syntax,
    ) {
        QuickCodeToken.EndIf
    }

    private fun LexerState.parseToken(
        condition: Boolean,
        syntax: TokenSyntax,
        createToken: LexerState.(content: String) -> QuickCodeToken,
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
        val tokens: MutableList<QuickCodeToken>
    )

    private fun List<QuickCodeToken>.filterEmptyRawTexts(): List<QuickCodeToken> {
        return this.filter {
            // filter empty RawText tokens
            it !is QuickCodeToken.RawText || it.text.isNotEmpty()
        }
    }

    private fun List<QuickCodeToken>.mergeConsecutiveRawTexts(): List<QuickCodeToken> {
        val original = this
        val res = mutableListOf<QuickCodeToken>()
        var i = 0
        while (i < original.size) {
            val current = original[i]
            if (current is QuickCodeToken.RawText) {
                val combinedRawText = buildString {
                    append(current.text)
                    var next: QuickCodeToken?
                    do {
                        next = original.getOrNull(i + 1)
                        if (next is QuickCodeToken.RawText) {
                            append(next.text)
                            i++
                        } else {
                            break
                        }
                    } while (true)
                }
                res.add(QuickCodeToken.RawText(combinedRawText))
            } else {
                res.add(current)
            }
            i++
        }

        return res
    }

    private fun List<QuickCodeToken>.beautifyRawTexts(): List<QuickCodeToken> {
        return mapIndexed { index, item ->
            if (item is QuickCodeToken.RawText) {
                // previous item
                when (getOrNull(index - 1)) {
                    in listOf(QuickCodeToken.Then, QuickCodeToken.Else) -> {
                        item.copy(
                            text = item.text.dropUnnecessaryWhiteSpace()
                        )
                    }

                    QuickCodeToken.EndIf -> {
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
