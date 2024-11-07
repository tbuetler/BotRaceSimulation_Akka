/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.boardmessage;

import ch.bfh.akka.botrace.common.Direction;
import ch.bfh.akka.botrace.common.Message;

import java.util.List;
import java.util.Objects;

/**
 * Informs a bot about which directions are possible
 * and how far away (in number of steps) the bot currently is from the target.
 * <br>
 * The number of steps is determined by the
 * <a href="https://en.wikipedia.org/wiki/Euclidean_distance">Euclidean
 * distance</a>,
 * rounded down to the nearest integer.
 * @param directions a list of possible directions a bot may proceed.
 * @param distance the number of steps to reach the target, provided that there are not obstacles.
 */
public record AvailableDirectionsReplyMessage(
        List<Direction> directions,
        int distance) implements Message {
    public AvailableDirectionsReplyMessage {
        Objects.requireNonNull(directions, "directions cannot be null");
    }
}
