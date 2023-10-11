package com.ivy.quickcode

import com.ivy.quickcode.interpreter.model.QCVariable
import com.ivy.quickcode.interpreter.model.QCVariableValue
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class QuickCodeCompilerTest : FreeSpec({
    lateinit var compiler: QuickCodeCompiler

    fun fileTestCase(
        folder: String,
        case: String,
        vars: Map<String, QCVariableValue>
    ) {
        // given
        val template = loadFile("compiler/$folder/$case.txt")

        // when
        println(compiler.compile(template))
        val res = compiler.execute(template, vars)

        // then
        val expected = loadFile("compiler/$folder/${case}_expected.txt")
        res.shouldBeRight() shouldBe expected
    }


    beforeEach {
        compiler = QuickCodeCompiler()
    }

    "example 1" {
        fileTestCase(
            folder = "sample",
            case = "1",
            vars = mapOf(
                "isViewModel" to QCVariableValue.Bool(true),
                "className" to QCVariableValue.Str("MyClass")
            )
        )
    }

    "Variable 0" {
        fileTestCase(
            folder = "variable",
            case = "0",
            vars = mapOf(
                "name" to QCVariableValue.Str("QuickCode"),
            )
        )
    }

    "If 0" {
        fileTestCase(
            folder = "if",
            case = "0",
            vars = mapOf(
                "a" to QCVariableValue.Bool(true),
            )
        )
    }

    "If 1" {
        fileTestCase(
            folder = "if",
            case = "1",
            vars = mapOf(
                "a" to QCVariableValue.Bool(false),
            )
        )
    }

    "If-Else 0" {
        fileTestCase(
            folder = "if",
            case = "ifelse0",
            vars = mapOf(
                "a" to QCVariableValue.Bool(true),
            )
        )
    }

    "If-Else 1" {
        fileTestCase(
            folder = "if",
            case = "ifelse1",
            vars = mapOf(
                "a" to QCVariableValue.Bool(false),
            )
        )
    }

    "Else-if 0" {
        fileTestCase(
            folder = "elseif",
            case = "0",
            vars = mapOf(
                "a" to QCVariableValue.Bool(true),
                "b" to QCVariableValue.Bool(true),
            )
        )
    }

    "Else-if 1" {
        fileTestCase(
            folder = "elseif",
            case = "1",
            vars = mapOf(
                "a" to QCVariableValue.Bool(false),
                "b" to QCVariableValue.Bool(true),
            )
        )
    }

    "Else-if 2" {
        fileTestCase(
            folder = "elseif",
            case = "2",
            vars = mapOf(
                "a" to QCVariableValue.Bool(false),
                "b" to QCVariableValue.Bool(false),
            )
        )
    }

    "Stacked Ifs 1" {
        fileTestCase(
            folder = "stacked_ifs",
            case = "1",
            vars = mapOf(
                "a" to QCVariableValue.Bool(true),
                "b" to QCVariableValue.Bool(true),
            )
        )
    }

    "Stacked Ifs 2" {
        fileTestCase(
            folder = "stacked_ifs",
            case = "2",
            vars = mapOf(
                "a" to QCVariableValue.Bool(false),
                "b" to QCVariableValue.Bool(true),
            )
        )
    }

    "Readme Example 0 - case 0" {
        fileTestCase(
            folder = "readme",
            case = "0_0",
            vars = mapOf(
                "firstName" to QCVariableValue.Str("John"),
                "lastName" to QCVariableValue.Str("Wick"),
                "args" to QCVariableValue.Bool(true),
                "logging" to QCVariableValue.Bool(true),
                "likeDogs" to QCVariableValue.Bool(true),
            )
        )
    }

    "resolves variable conflicts" {
        // given
        val code = """
            println("Hello, {{name}}!")
            #if {{name}} #then
            // {{name}}
            #endif
            // Bye, {{name}}!
        """.trimIndent()

        // when
        val res = compiler.compile(code)

        // res
        res.shouldBeRight().variables shouldBe listOf(
            QCVariable.Bool("name")
        )
    }
})