package com.ivy.quickcode

import arrow.core.Either
import arrow.core.identity
import com.ivy.quickcode.interpreter.model.QCVariableValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name

fun main(args: Array<String>) {
    // TODO: Extract this as a class, handle errors cases and test it
    if (args.size != 2) {
        println("Invalid arguments!")
        return
    }
    val template = readFileContent(args[0])
    val inputJson = readFileContent(args[1])
    val rawInput: Map<String, JsonPrimitive> = Json.decodeFromString(inputJson)
    val input = rawInput.map { (key, value) ->
        key to when {
            value.isString -> QCVariableValue.Str(value.content)
            value.booleanOrNull != null -> QCVariableValue.Bool(value.boolean)
            else -> error("Unsupported input type \"$key\"")
        }
    }.toMap()

    println("Input:")
    println(input)

    val compiler = QuickCodeCompiler()
    val result = compiler.execute(template, input)
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
}

fun readFileContent(relativePath: String): String {
    val path = Paths.get(relativePath)
    return Files.readString(path)
}

fun produceOutputFile(templatePath: String, result: String) {
    val path = Paths.get(templatePath)
    val fileName = path.fileName.name
    val outputFilename = fileName.dropLast(3)
    Files.writeString(
        Paths.get(outputFilename),
        result
    )
    println("'$outputFilename' created.")
}