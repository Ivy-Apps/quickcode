package com.ivy.quickcode.parser

import com.ivy.quickcode.data.IfStatement
import com.ivy.quickcode.data.QuickCodeAst
import com.ivy.quickcode.data.RawText
import com.ivy.quickcode.data.Variable
import com.ivy.quickcode.lexer.QuickCodeToken
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class QuickCodeParserTest : FreeSpec({
    fun printIndent(text: String, indent: Int) {
        println(" ".repeat(indent) + text)
    }

    fun QuickCodeAst.printAst(
        indent: Int = 0,
    ) {
        when (this) {
            is QuickCodeAst.Begin -> {
                printIndent("Begin", indent)
            }

            is IfStatement -> {
                printIndent("If: $condition", indent)
                thenBranch.printAst(indent = indent + 4)
                elseBranch?.printAst(indent = indent + 4)
            }

            is RawText -> {
                printIndent("RawText: $text", indent)
            }

            is Variable -> {
                printIndent("Variable: $name", indent)
            }
        }
        next?.printAst(indent)
    }

    infix fun ParseResult.shouldBe(expected: QuickCodeAst) {
        println("Actual:")
        (this as ParseResult.Success).ast.printAst()
        println("-------")
        println()
        println("Expected:")
        expected.printAst()
        println("-------")
        this.ast shouldBe expected
    }

    fun buildAst(vararg ast: QuickCodeAst): QuickCodeAst {
        val begin = QuickCodeAst.Begin()
        var current: QuickCodeAst = begin
        for (node in ast) {
            current.next = node
            current = node
        }
        return begin
    }

    lateinit var parser: QuickCodeParser

    beforeEach {
        parser = QuickCodeParser()
    }


    "simple if" {
        // given
        val tokens = listOf(
            QuickCodeToken.RawText("println(\"Hello, "),
            QuickCodeToken.Variable("name"),
            QuickCodeToken.RawText("!\""),
            QuickCodeToken.If,
            QuickCodeToken.IfExpression.BoolVariable("a"),
            QuickCodeToken.IfExpression.And,
            QuickCodeToken.IfExpression.Not,
            QuickCodeToken.IfExpression.BoolVariable("b"),
            QuickCodeToken.Then,
            QuickCodeToken.RawText("test"),
            QuickCodeToken.EndIf,
        )

        // when
        val res = parser.parse(tokens)

        // then
        res shouldBe buildAst(
            RawText("println(\"Hello, "),
            Variable("name"),
            RawText("!\""),
            IfStatement(
                condition = IfStatement.Condition.And(
                    IfStatement.Condition.BoolVar("a"),
                    IfStatement.Condition.Not(IfStatement.Condition.BoolVar("b"))
                ),
                thenBranch = buildAst(
                    RawText("test")
                )
            )
        )
    }

    "if else" {
        // given
        val tokens = listOf(
            QuickCodeToken.If,
            QuickCodeToken.IfExpression.BoolVariable("day"),
            QuickCodeToken.Then,
            QuickCodeToken.RawText("Good day, "),
            QuickCodeToken.Variable("name"),
            QuickCodeToken.RawText("!"),
            QuickCodeToken.Else,
            QuickCodeToken.RawText("Good night!"),
            QuickCodeToken.EndIf,
        )

        // when
        val res = parser.parse(tokens)

        // then
        res shouldBe buildAst(
            IfStatement(
                condition = IfStatement.Condition.BoolVar("day"),
                thenBranch = buildAst(
                    RawText("Good day, "),
                    Variable("name"),
                    RawText("!")
                ),
                elseBranch = buildAst(
                    RawText("Good night!")
                )
            )
        )
    }
})