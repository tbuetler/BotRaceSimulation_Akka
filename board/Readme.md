# Board Actor System

## Description

This Akka program constitutes the board of the
Bot-Race actor system.

## Execution of the Board with Maven

It is assumed that you have compiled
everything according
to the instructions given in the top-level
[README.md](../README.md)
file in the root of this project.

To execute the board GUI application, type
in the `board` subdirectory:

```console
mvn -q exec:java
```

Ouptut:
```
-- a lot of output --
Oct 05, 2023 2:08:53 PM com.sun.javafx.application.PlatformImpl startup
WARNING: Unsupported JavaFX configuration: classes were loaded from 'unnamed module @b4de166'
-- a lot of output --
```

You can ignore the `WARNING` (or `WARNUNG` if your
locale is set to German). It occurs as we start
the GUI as a non-JPMS application which is not
supported by JavaFX.
