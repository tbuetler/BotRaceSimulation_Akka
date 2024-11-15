# Documentation (yet a Template)

> **Disclaimer:** This documentation might been enhanced and grammer checked with chatGPT and deepl.

> This is a template for the project documentation. Template comments are placed
> into Markdown notes. Replace them with your text, code fragments, and images.

> **Note:** The documentation is in progress. The notes are going to be delete when finished //Tim

> **Note an Gil, Martin, Pablo:** I de notes steit jewils was im jewilige Abschnitt gschribe söt werde. Bitte prüeft dr text.

## Introduction

> Give a short summary of the Bot-Race Actor system. Describe the role of the board, the bots.
> Give an example of a board layout, i.e., obstacles, starting point and target, and that bots must navigate to the
> target having
> limited information. Summarize briefly the phases of the board when playing the game.
> When referring to phases use the terms as given in the stated diagrams.

> Spend a few words on the basic principle of Akka.

The Bot-Race Actor system is a distributed system that simulates a race between bots on a board. The role of the board
is to provide the game environment, including obstacles and the target that the bots must navigate to. The bots have
limited information about the board and communicate with the board to get information about available directions and the
chosen direction.

An example of a board layout could be a rectangular grid with obstacles represented by walls or barriers. The starting
point for the bots could be at one end of the grid, and the target could be at the other end. The bots must navigate
through the grid, avoiding obstacles and reaching the target.

\
The phases of the board when playing the game are:

**Setup Phase:**\
The board is set up with the initial configuration, including the layout of obstacles and the starting
and target positions.

**Playing Phase:**\
The bots start the race, and they communicate with the board to get information about available
directions and the chosen direction. They move one step at a time, and the board updates their positions accordingly.

**Pause Phase:**\
The board can pause the race at any time, for example, if a bot reaches a specific position or if a
certain condition is met.

**Resume Phase:**\
The race can be resumed after being paused, allowing the bots to continue their movement.

**Termination Phase:** \
The race is terminated when all bots have reached the target or when a specific condition is met,
such as a certain time limit or a specific number of steps taken.

**Akka**\
The basic principle of Akka is to build distributed systems using actors, which are lightweight, isolated, and
independent units of execution. Akka provides a message-passing model where actors communicate with each other by
sending messages. Actors are resilient to failures and can be scaled horizontally by adding more actors. The Bot-Race
Actor system utilizes Akka to implement the distributed behavior of the bots and the board.

## Document the Bot / Board Interaction

> Document the interaction between a bot with the board. Use the UML sequence diagram
> you made for day 2. Discuss it (happy case). Discuss also possible error cases.

> You can reuse / include the drawings made for the first session with your coach.

> **Note:** The title of the sequence diagrams ar wrong

### Interaction between a Bot and the Board

In the happy case, the bot registers with the board, receives the setup information, and then requests available
directions to move. The board responds with the available directions, and the bot moves to a new position. This process
continues until the bot reaches the target, at which point the board sends a termination message to the bot.

#### Error Cases

**Invalid Registration:** If the bot sends an invalid registration message, the board will respond with an error
message,
and the bot will not be able to participate in the game.

**Unknown Message:** If the bot sends an unknown message to the board, the board will respond with an error message, and
the
bot will not be able to continue playing.

**Timeout:** If the bot does not respond to the board's messages within a certain time limit, the board will assume that
the
bot has timed out and will terminate the game.

### Sequencediagram without interrupts

![Sequencediagram](../img/Sequencediagram_without.svg)

### Sequencediagram wih interrupts

![Sequencediagram](../img/Sequencediagram_with.svg)

## The Design of the Board Actor System

> Document the board actor system. Use UML class diagrams and / or any other UML diagram notation
> you are familiar with. Classes in class diagrams should have minimal information such as class
> names (mandatory), major attributes, and perhaps major operations. But do not list every detail
> of classes in class diagrams; for details, the code is the documentation.

> Describe how you implemented the states (a.k.a. the phases) of the board.

> Describe the coupling of the board actor system with you UI (CLI or GUI or both). Describe
> the roles of interfaces, if any. Describe how the UI can "talk" to the actor system.
> Describe how the actor system can "talk" to the UI, and how UI-updates are performed.
> Describe, too, how and when the actor system is instantiated, and how the UI
> gets a reference to it. And also describe, how the actor system (i.e., the root actor
> of the board actor system) obtains a reference to the UI (its controller or its model).

> Describe you UI architecture. Assuming that it is architected according to the
> [MVC architectural pattern](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) or
> [any variant thereof](https://martinfowler.com/eaaDev/uiArchs.html),
> describe which of your classes has which role of the [MVC] pattern.

> You can reuse / include the drawings made for the second session with your coach.

### Classdiagram *(Generated by IntelliJ IDEA)*

![Classdiagram](src/classdiagram.png)

## The Design of the Bot Actor System

> Document the bot actor system in the same spirit as the board actor system documentation, less
> the UI aspect.

> Describe how you implemented the states (a.k.a. the phases) of the board. Especially
> describe how the bot actor system terminates in the case that all its bots have
> terminated.

The bot actor system is designed to handle the behavior of the bots in the Bot-Race actor system. It consists of several actors, each representing a bot.

### Classes in the Bot Actor System

- `BotRoot`: Represents the root actor of the bot actor system.
- `ClusterListener`: Listens to cluster events and handles the registration and deregistration of bots.
- `Bot`: Represents a single bot in the system.

### States of the Bot Actor System

The bot actor system has the following states:

> **Note:** This has to be done correctly

- `Idle`: The initial state of the bot actor system. Bots are not yet registered.
- `Running`: The state when the bot actors are running.
- `Terminated`: The state when all the bot actors have terminated.

### Termination of the Bot Actor System

The bot actor system terminates when all the bot actors have terminated. This can happen for several reasons:

- All the bots have reached the target position.
- All the bots have been deregistered.
- The board actor system has terminated.

When the bot actor system terminates, it sends a message to the board actor system to inform it of the termination.


## The Algorithms of the Bots

> Describe the algorithm or the algorithms of your bot. Describe the reasoning of your
> design decisions. Illustrate the algorithm(s) with drawings, if suitable.

> Pseudocode of the algorithm(s)

The bot uses a randomized path selection algorithm to navigate a grid and reach a target position. At each move, it
queries the possible directions it can go from the current position and randomly picks one direction from this set. The
bot’s path selection logic is straightforward but not optimal, as it lacks memory of past positions or awareness of
dead-ends, which makes it susceptible to revisiting the same points repeatedly or getting stuck in loops.

Here's an example of a pseudocode for the bot's algorithm:

```pseudocode
function playGame() {
    initializeBot();
    while (true) {
        switch (currentPhase) {
            case REGISTERING:
                registerWithBoard();
                waitForGameToStart();
                break;

            case READY:
                waitForAvailableDirections();
                break;

            case PLAYING:
                chooseDirection();
                sendChosenDirectionToBoard();
                checkIfMoveIgnored();
                checkIfTargetReached();
                break;

            case PAUSED:
                waitForResumeMessageFromBoard();
                break;

            case TARGET_REACHED:
                deregisterWithBoard();
                return;  // Spiel endet, wenn das Ziel erreicht ist
        }
    }
}

// Phase: REGISTERING
function registerWithBoard() {
    // Send registration message to the board
    print("Registering bot with the board");
}

function waitForGameToStart() {
    // Wait until the start message is received
    print("Waiting for game to start...");
}

// Phase: READY
function waitForAvailableDirections() {
    // Request available directions from the board and wait for response
    print("Requesting available directions from the board");
}

// Phase: PLAYING
function chooseDirection() {
    availableDirections = getAvailableDirections();  // Retrieve from board message
    if (availableDirections.isEmpty()) {
        print("No directions available");
        return;
    }
    
    // **Simple Strategy for Pathfinding**:
    // 1. Evaluate each direction:
    //    - If moving closer to the target, prioritize it.
    //    - If not closer, deprioritize.
    // 2. Choose the first best direction; fallback to random if blocked.
    
    bestDirection = null;
    minDistanceToTarget = calculateDistanceToTarget(currentPosition);
    
    // Evaluate all directions to find the best one
    for each direction in availableDirections {
        newPosition = calculateNewPosition(currentPosition, direction);
        distanceToTarget = calculateDistanceToTarget(newPosition);
        
        if (distanceToTarget < minDistanceToTarget) {
            minDistanceToTarget = distanceToTarget;
            bestDirection = direction;
        }
    }

    // If a closer direction is found, choose it; otherwise, pick a random direction
    chosenDirection = bestDirection != null ? bestDirection : chooseRandomDirection(availableDirections);
}

function sendChosenDirectionToBoard() {
    // Send the chosen direction to the board
    print("Sending chosen direction to board:", chosenDirection);
}

function checkIfMoveIgnored() {
    // If the move is ignored, request available directions again
    if (moveWasIgnored()) {
        print("Chosen direction was ignored. Re-requesting directions");
        waitForAvailableDirections();
    }
}

function checkIfTargetReached() {
    // Check if the bot has reached the target position
    if (currentPosition == targetPosition) {
        currentPhase = TARGET_REACHED;
        print("Target reached!");
    }
}

// Phase: PAUSED
function waitForResumeMessageFromBoard() {
    // Wait for a resume message to continue playing
    print("Game paused. Waiting to resume...");
}

// Phase: TARGET_REACHED
function deregisterWithBoard() {
    // Send deregistration message to the board
    print("Deregistering bot from the board");
}

// **Helper Functions**:
function calculateNewPosition(currentPosition, direction) {
    // Calculate new position based on direction
    return newPosition;
}

function calculateDistanceToTarget(position) {
    // Calculate distance from the given position to the target
    return distance;
}

function chooseRandomDirection(availableDirections) {
    // Choose a random direction from available options
    return randomDirection;
}

function moveWasIgnored() {
    // Check if the move was ignored based on board response
    return trueOrFalse;
}

function initializeBot() {
    // Set initial values for bot
    currentPhase = REGISTERING;
    currentPosition = (0, 0);
    moveCount = 0;
    recentDirections = [];
}
```

This is a simplified example, and the actual implementation may vary based on the specific requirements and design
decisions of your bot.

## Administrative Issues

> For every team member: his/her experience  
> Overall experience  
> Git commit analytics

**Gil**\
This project allowed me to enhance my understanding of distributed systems, specifically with Akka's actor-based
approach. I was involved in designing the board actor system, focusing on message handling between the bots and the
board. This project highlighted the importance of effective communication between team members, as well as a deep
understanding of concurrent programming concepts. I found it challenging yet rewarding to design the message protocols
and ensure consistent state transitions in the system.

**Martin**\
Working on the bot race game gave me the opportunity to develop my skills in pathfinding and algorithm design. I worked
closely on the bot’s movement logic, implementing strategies to detect and avoid dead-ends, which involved understanding
how to track previously visited positions and apply memory-based movement to optimize navigation. This experience
improved my problem-solving abilities, especially in debugging movement paths and refining the bot’s efficiency within
the grid.

**Pablo**\
In this project, I was able to apply and expand my knowledge of distributed systems using the Akka framework. My role
primarily focused on error handling and optimizing the board’s response times to bot requests. Working on exception
management and error-case scenarios taught me a lot about resilience in distributed applications, which are critical
when multiple actors interact. Additionally, I took part in debugging the actor interactions, which strengthened my
understanding of concurrency and synchronization in Java.

**Tim**\
Using Git and GitHub for version control was a new and valuable experience for me. I learned to manage branches, submit
pull requests, and resolve merge conflicts, which improved my workflow. I also took on part of the GUI design for this
project, which presented a unique challenge. With no predefined layout, I had to be creative and develop a user
interface that not only presented information clearly but also interacted effectively with the backend. This experience
taught me a lot about UI design principles and integration with backend systems.
