package com.ivy.quickcode

import com.ivy.quickcode.data.QCVariableValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
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
    println("----------------")
    println(result)
}

fun readFileContent(relativePath: String): String {
    val path = Paths.get(relativePath)
    return Files.readString(path)
}