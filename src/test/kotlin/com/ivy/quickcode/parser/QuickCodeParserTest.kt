package com.ivy.quickcode.parser

import com.ivy.quickcode.data.IfStatement
import com.ivy.quickcode.data.QuickCodeAst
import com.ivy.quickcode.data.RawText
import com.ivy.quickcode.data.Variable
import com.ivy.quickcode.lexer.Token
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
            Token.RawText("println(\"Hello, "),
            Token.Variable("name"),
            Token.RawText("!\""),
            Token.If,
            Token.IfExpression.BoolVariable("a"),
            Token.IfExpression.And,
            Token.IfExpression.Not,
            Token.IfExpression.BoolVariable("b"),
            Token.Then,
            Token.RawText("test"),
            Token.EndIf,
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
            Token.If,
            Token.IfExpression.BoolVariable("day"),
            Token.Then,
            Token.RawText("Good day, "),
            Token.Variable("name"),
            Token.RawText("!"),
            Token.Else,
            Token.RawText("Good night!"),
            Token.EndIf,
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