# Bot Actor System

## Description

This Akka program runs a set of bots. For each bot, a root actor is created.
Each root actor runs the bot behavior exactly once and then terminates.

## Compilation

It is assumed that this Maven module is compiled according to the instructions
given in the top-level
[README.md](../README.md)
file.

## Execution of the Bot(s) with Maven

It is assumed that you have compiled
everything according
to the instructions given in the top-level
[Readme.md](../README.md)
file in the root of this project.

To execute the Bot actor application, type
in the `bot` subdirectory:

```console
mvn -q exec:java
```

Output:

```
-- a lot of output --
...
```
