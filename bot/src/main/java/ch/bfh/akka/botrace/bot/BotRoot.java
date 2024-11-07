/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.bot;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractOnMessageBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.Receptionist.Listing;
import akka.actor.typed.receptionist.ServiceKey;
import ch.bfh.akka.botrace.common.BoardService;
import ch.bfh.akka.botrace.common.Message;

/**
 * The root actor of the Bot actor system.
 */
public class BotRoot extends AbstractOnMessageBehavior<Message> { // guardian actor

    /**
     * The service key instance to lookup for the service name {@link BoardService#SERVICE_NAME} of the board.
     */
    final static ServiceKey<Message> serviceKeyForBoard = ServiceKey
            .create(Message.class, BoardService.SERVICE_NAME);

    /**
     * The response message of the {@link Receptionist}.
     */
    record ListingResponse(Listing listing) implements Message {}

    /**
     * Factory method to create the bot root actor.
     *
     * @return a behavior.
     */
    public static Behavior<Message> create(String botName) {
        return Behaviors.setup(c -> new BotRoot(c, botName));
    }

    /**
     * Upon creation of the Bot root actor, it tells the {@link Receptionist} its
     * interest in receiving a list of services.
     *
     * @param context the context of this actor system
     */
    private BotRoot(ActorContext<Message> context, String botName) {
        super(context);
        // TODO initialize the root actor...

        ActorRef<Listing> listingResponseAdapter = context
                .messageAdapter(Listing.class, ListingResponse::new);
        context.getSystem().receptionist().tell(Receptionist.subscribe(serviceKeyForBoard, listingResponseAdapter));
        // TODO continue initialization, if necessary...
    }

    /**
     * Handle incoming messages.
     * @param message a message
     * @return the same behavior
     */
    @Override
    public Behavior<Message> onMessage(Message message) {
        // TODO handle incoming messages
        return Behaviors.same();
    }

    // TODO add handlers for messages
    // TODO add mechanism for phases, e.g., finite state machine
}
