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
import ch.bfh.akka.botrace.common.botmessage.*;

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

        context.getSelf().tell(new RegisterMessage(botName, context.getSelf()));
    }

    /**
     * Handle incoming messages.
     * @param message a message
     * @return the same behavior
     */
    @Override
    public Behavior<Message> onMessage(Message message) {
        if (message instanceof PingResponseMessage) { // catch the ping response
            return onPingResponse((PingResponseMessage) message);
        } else if (message instanceof DeregisterMessage) { // catch the deregister message
            return onDeregister((DeregisterMessage) message);
        } else if (message instanceof ChosenDirectionMessage) { // catch the chosen direction message
            return onChosenDirection((ChosenDirectionMessage) message);
        } else if (message instanceof AvailableDirectionsRequestMessage) { // catch the available directions request message
            return onAvailableDirectionsRequest((AvailableDirectionsRequestMessage) message);
        } else {
            // if the message is unknown
            getContext().getLog().info("Received unknown message");
            return this;
        }
    }

    private Behavior<Message> onPingResponse(PingResponseMessage message) {
        getContext().getLog().info("Ping response from: {}", message.name());
        return this;
    }

    private Behavior<Message> onDeregister(DeregisterMessage message) {
        getContext().getLog().info("Deregistering because: {}", message.reason());
        return Behaviors.stopped();
    }

    private Behavior<Message> onChosenDirection(ChosenDirectionMessage message) {
        getContext().getLog().info("Moving in direction: {}", message.chosenDirection());
        return this;
    }

    private Behavior<Message> onAvailableDirectionsRequest(AvailableDirectionsRequestMessage message) {
        getContext().getLog().info("Requesting available directions");
        return this;
    }
}
