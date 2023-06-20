# QuickCode
A simple and elegant language for generating dynamic code templates using **{{variables}}** and **#if**s.
```
println("Hello, {{name}}!")
#if {{morning}} #then
println("Good morning :)")
#endif
```

It's designed to be learned at a single glance. Use:
- **{{variableName}}** to create a variable for your template.
- **#if {{booleanName}} #then** for parts in the template which will appear only if `boolName` is true.

When you evaluate/execute your template [Compose Hammer](https://plugins.jetbrains.com/plugin/21912-compose-hammer) will ask
you to provide values for the defined variables.

## Syntax

**Keywords:**
- `{{varName}}`: creates a variable. If it's inside an `#if` condition becomes a boolean.
- `#if`: start an if condition.
- `#then`: ends an if condition and starts the template that'll appear only if the condition is true.
- `#endif`: ends the template that'll appear if the if condition is true.
- `#else`: an alternative to `#endif` for creating if-else statements. _Note `#else` must always end with `#endif`._
- `#elseif`: used to create else-if statements. Its condition must end with `#then` just like for `#if`.

**If-condition operators:**
- `AND`: logical AND like `&&` in many languages.
- `OR`: logical OR like `||` in many languages.
- `NOT`: logical NOT like `!`.
- `()`: brackets used for operations priority.

That's it! Now you're ready to create your own code templates.

> Your only limitation is your imagination!

## Demo

_TBD:_ I'm not that creative so feel free to submit me some PRs with ideas!

**For example:**

```
fun main(#if {{args}} #then args: Array<String>#endif) {
#if {{logging}} AND NOT {{prod}} #then
  println("program started.")
#endif

  println("Hello, {{firstName}} {{lastName}}!")
#if {{likeDogs}} #then
  println("Here's a dog.")
#elseif {{likeCats}} #then
  println("Here's a cat.")
#else
  println("No animals for you, sir.")
#endif

#if ({{logging}} AND NOT {{prod}}) OR {{forcedDebug}} #then
  println("program finished.")
#endif
}
```

**When executed with:**

```json
{
   "firstName":"John",
   "lastName":"Wick",
   "args":true,
   "logging":true,
   "likeDogs":true
}
```

**Will produce:**

```kotlin
fun main(args: Array<String>) {
  println("program started.")

  println("Hello, John Wick!")
  println("Here's a dog.")

  println("program finished.")
}
```
