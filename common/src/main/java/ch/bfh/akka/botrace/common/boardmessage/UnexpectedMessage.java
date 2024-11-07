/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common.boardmessage;

import ch.bfh.akka.botrace.common.Message;

import java.util.Objects;

/**
 * Signals a bot having sent an unexpected message in a certain phase.
 * @param description a short description why this message was not expected.
 */
public record UnexpectedMessage(String description) implements Message {
    public UnexpectedMessage {
        Objects.requireNonNull(description, "description cannot be null");
    }
}
