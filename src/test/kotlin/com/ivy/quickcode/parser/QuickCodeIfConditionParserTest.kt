package com.ivy.quickcode.parser

import com.ivy.quickcode.data.IfStatement.Condition
import com.ivy.quickcode.lexer.Token
import com.ivy.quickcode.lexer.Token.IfExpression
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class QuickCodeIfConditionParserTest : FreeSpec({
    fun parser(
        vararg tokens: Token
    ): QuickCodeIfConditionParser {
        return QuickCodeIfConditionParser(tokens.toList())
    }

    fun printCondition(condition: Condition?) {
        println("Actual: $condition")
    }

    infix fun Pair<Condition, Int>?.conditionShouldBe(expected: Condition?) {
        printCondition(this?.first)
        this?.first shouldBe expected
    }

    infix fun Pair<Condition, Int>?.newPosShouldBe(expected: Int) {
        this?.second shouldBe expected
    }


    "bool var" {
        // given
        val parser = parser(
            Token.RawText("something"),
            Token.If,
            IfExpression.BoolVariable("a"),
            Token.Then,
            Token.RawText("Okay")
        )

        // when
        val res = parser.parse(position = 2)

        // then
        res conditionShouldBe Condition.BoolVar("a")
        res newPosShouldBe 4
    }

    "simple expression" {
        // given
        val parser = parser(
            Token.If,
            IfExpression.BoolVariable("cond1"),
            IfExpression.And,
            IfExpression.Not,
            IfExpression.BoolVariable("cond2"),
            Token.Then,
            Token.RawText("Okay")
        )

        // when
        val res = parser.parse(position = 1)

        // then
        res conditionShouldBe Condition.And(
            Condition.BoolVar("cond1"),
            Condition.Not(Condition.BoolVar("cond2"))
        )
        res newPosShouldBe 6
    }

    "double And" {
        // given
        val parser = parser(
            Token.If,
            IfExpression.BoolVariable("a"),
            IfExpression.And,
            IfExpression.BoolVariable("b"),
            IfExpression.And,
            IfExpression.BoolVariable("c"),
            Token.Then,
            Token.RawText("Okay")
        )

        // when
        val res = parser.parse(position = 1)

        // then
        res conditionShouldBe Condition.And(
            Condition.BoolVar("a"),
            Condition.And(
                Condition.BoolVar("b"),
                Condition.BoolVar("c"),
            )
        )
        res newPosShouldBe 7
    }

    fun testTripleOr() {
        // given
        val parser = parser(
            Token.If,
            IfExpression.BoolVariable("a"),
            IfExpression.Or,
            IfExpression.BoolVariable("b"),
            IfExpression.Or,
            IfExpression.BoolVariable("c"),
            IfExpression.Or,
            IfExpression.BoolVariable("d"),
            Token.Then,
            Token.RawText("Okay"),
            Token.Variable("name"),
        )

        // when
        val res = parser.parse(position = 1)

        // then
        res conditionShouldBe Condition.Or(
            Condition.BoolVar("a"),
            Condition.Or(
                Condition.BoolVar("b"),
                Condition.Or(
                    Condition.BoolVar("c"),
                    Condition.BoolVar("d"),
                )
            )
        )
        res newPosShouldBe 9
    }
})