package com.ivy.quickcode.lexer

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class QuickCodeLexerTest : FreeSpec({
    fun List<Token>.printActual() {
        println("Actual result:")
        for ((index, token) in this.withIndex()) {
            println("#$index: $token")
        }
        println("------")
    }

    infix fun List<Token>.shouldBe(expectedTokens: List<Token>) {
        printActual()

        for (index in this.indices) {
            val actualToken = this[index]
            val expected = expectedTokens[index]
            println("#$index: $actualToken")
            actualToken shouldBe expected
        }
    }

    lateinit var lexer: QuickCodeLexer

    beforeEach {
        lexer = QuickCodeLexer()
    }

    "empty string" {
        // given
        val text = ""

        // when
        val tokens = lexer.tokenize(text)

        // then
        tokens.size shouldBe 0
    }

    "raw text" {
        // given
        val text = "Hello, World!"

        // when
        val tokens = lexer.tokenize(text)

        // then
        tokens shouldBe listOf(
            Token.RawText("Hello, World!")
        )
    }

    "variable" {
        // given
        val text = "{{variable}}"

        // when
        val tokens = lexer.tokenize(text)

        // then
        tokens shouldBe listOf(
            Token.Variable("variable")
        )
    }

    "if condition" {
        // given
        val text = "#if ({{var1}} AND {{var2}}) OR NOT {{var3}}"

        // when
        val tokens = lexer.tokenize(text)

        // then
        tokens shouldBe listOf(
            Token.If,
            Token.IfExpression.OpenBracket,
            Token.IfExpression.BoolVariable("var1"),
            Token.IfExpression.And,
            Token.IfExpression.BoolVariable("var2"),
            Token.IfExpression.CloseBracket,
            Token.IfExpression.Or,
            Token.IfExpression.Not,
            Token.IfExpression.BoolVariable("var3"),
        )
    }


    "mixed" {
        // given
        val text = """
            class {{name}}ViewModel : ViewModel() {
               @Composable
               fun content(): {{name}}State {
                   //TODO:
               }
                  
               #if {{hasEvents}} AND NOT ({{legacy}} OR {{old}}) #then
               fun eventHandling(event: {{name}}Event) {
                   // TODO:
               }
               #endif
            }
        """.trimIndent()

        // when
        val tokens = lexer.tokenize(text)

        // then
        tokens.printActual()
    }
})