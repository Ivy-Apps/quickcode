package com.ivy.quickcode

import arrow.core.Either
import arrow.core.identity
import arrow.core.raise.either
import com.ivy.quickcode.interpreter.model.QCVariableValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name

fun main(args: Array<String>) {
    either {
        val input = parseInput(args).bind()
        println("Input:")
        println(input)

        val compiler = QuickCodeCompiler()
        val result = compiler.execute(input.templateText, input.variables)
        println("----------------")
        if (result is Either.Right) {
            produceOutputFile(
                templatePath = args[0],
                result = result.value,
            )
        }
        println("----------------")
        println(
            result.fold(
                ifLeft = { "Compilation error: $it" },
                ifRight = ::identity,
            )
        )
    }.onLeft {
        println("Input error: $it")
    }
}

private fun parseInput(
    args: Array<String>
): Either<String, Input> = either {
    if (args.size != 2) {
        raise("Invalid arguments! Pass exactly 2 arguments.")
    }
    val templateText = readFileContent(args[0]).bind()
    val inputJson = readFileContent(args[1]).bind()
    val rawInput: Map<String, JsonPrimitive> = Json.decodeFromString(inputJson)
    val variables = rawInput.map { (key, value) ->
        key to when {
            value.isString -> QCVariableValue.Str(value.content)
            value.booleanOrNull != null -> QCVariableValue.Bool(value.boolean)
            else -> error("Unsupported input type \"$key\"")
        }
    }.toMap()
    Input(templateText, variables)
}


private fun readFileContent(
    relativePath: String
): Either<String, String> = Either.catch {
    val path = Paths.get(relativePath)
    Files.readString(path)
}.mapLeft { it.toString() }

private fun produceOutputFile(templatePath: String, result: String) {
    val path = Paths.get(templatePath)
    val fileName = path.fileName.name
    val outputFilename = fileName.dropLast(3)
    Files.writeString(
        Paths.get(outputFilename),
        result
    )
    println("'$outputFilename' created.")
}

data class Input(
    val templateText: String,
    val variables: Map<String, QCVariableValue>
)