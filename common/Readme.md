# The Interface (Common) for the Bot-Race Actor System

## Description

This module defines the messages
and the protocol between the board
and bot(s) of the Akka Bot-Race actor system.

## Interaction Among Board and Bot Actors

This section describes the exchange of messages
between Akka actors of the Bot-Race actor system.
See also the state diagrams of the board
and bots actor systems in the top-level
[README](../README.md) file.

### At Any Time

- Board &rightarrow; Bot: `PingMessage`  
  At any time, the board can send a `PingMessage` to
  a bot. The bot MUST respond with a `PingResponseMessage`.
  If not, the board assumes that the bot is dead and
  removes it.

- Bot &rightarrow; Board: `DeregisterMessage`  
  If sent, this message tells the board that
  this bot is no longer part of the Bot-Race
  actor system.

- Board &rightarrow; Bot: `UnexpectedMessage`  
  Optional: The board sends this message to a bot if
  it previously got an unexpected message from that bot.

### At Registration

- Bot &rightarrow; Board: `RegisterMessage`  
  A bot sends `RegisterMessage` to the board.
  Provided that the board has terminated its
  setup phase then the board responds with
  a `SetupMessage`.

### Playing

- Board &rightarrow; Bot: `StartMessage`  
  Indicates the start of the race.
- Bot &rightarrow; Board: `AvailableDirectionsRequestMessage`  
  A bot sends this message to get informed about possible
  directions.
- Board &rightarrow; Bot: `AvailableDirectionsReplyMessage`  
  The board responds with `AvailableDirectionsReplyMessage`
  indicating the available directions as well as the
  remaining distance. See Javadoc for the definition
  of the distance.
- Bot &rightarrow; Board: `ChosenDirectionMessage`  
  The bot tell the board the direction for the next step.
- Board &rightarrow; Bot: `ChosenDirectionIgnoredMessage`  
  The board replies with this message if the chosen direction
  of the bot is not available, for example, if there is
  an obstacle or the bot reached the border of the playground.
- Board &rightarrow; Bot: `PauseMessage`  
  Tells the (remaining) bots that they shall pause the race.
- Board &rightarrow; Bot: `TargetReachedMessage`  
  Tells a bot that it has reached the target. The
  board pauses the race.
- Board &rightarrow; Bot: `UnregisteredMessage`  
  Tells all bots that the race is terminated normally
  or abnormally. In any case, a bot MUST register
  for a new race.

### Pausing

- Board &rightarrow; Bot: `ResumeMessage`  
  Tells the (remaining) bots that they shall resume the race.

## Dependency

Among others, the Board and Bot modules
must have the following dependency:

```console
<dependency>
	<groupId>ch.bfh.akka.botrace</groupId>
	<artifactId>common</artifactId>
	<version>1.1</version>
</dependency>
```

## Configuration of Common

> **Note**: No change needed for the configuration of Common.

The configuration of Common is as follows:

```code
#
# Specification of the serialization format of messages for the Bot-Race actor system.
#
akka {
  actor {
    serialization-bindings {
      "ch.bfh.akka.actorrace.common.CborSerializable" = jackson-cbor
    }
  }
}
```

#### Notes:

- No changes needed here unless you use a different kind of serialization.
- `ch.bfh.akka.botrace.common.CborSerializable` and `jackson-cbor` are
  responsible for the use of
  [CBOR](https://en.wikipedia.org/wiki/CBOR)
  serialization.
- Akka provides also other serialization/deserialization methods.

## Maven Installation

You must install this Maven modul into your local Maven repository. (When using IDE's only,
some IDE's to not require this.) To compile and install this module, type:

```console
mvn clean install
-- a lot of output --
[INFO] --- install:3.1.1:install (default-install) @ common ---
[INFO] Installing /.../common/pom.xml to /.../repository/ch/bfh/akka/botrace/common/1.0/common-1.0.pom
[INFO] Installing /.../common/target/common-1.0.jar to /.../repository/ch/bfh/akka/botrace/common/1.0/common-1.0.jar
[[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  3.791 s
[INFO] Finished at: 2023-10-05T15:11:13+02:00
[INFO] ------------------------------------------------------------------------
```

## Javadoc

Assuming you have the Maven command available in your terminal,
to generate Javadoc, type:

```console
mvn clean compile javadoc:javadoc
-- a lot of output --
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  3.791 s
[INFO] Finished at: 2023-10-05T15:11:13+02:00
[INFO] ------------------------------------------------------------------------
```

You find the generated Javadoc files (with `index.html`) in the
subdirectory `target/site/apidocs`.
