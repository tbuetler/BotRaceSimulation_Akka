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
import ch.bfh.akka.botrace.common.boardmessage.*;
import ch.bfh.akka.botrace.common.boardmessage.PingMessage;
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

    private ActorRef<Message> boardRef;
    private final String actorName = getContext().getSelf().path().name();

    /**
     * Handle incoming messages.
     * @param message a message
     * @return the same behavior
     */
    @Override
    public Behavior<Message> onMessage(Message message) {

        return switch(message){
            case PingMessage ignored                                                   -> onPing();
            case SetupMessage setupMessage                                             -> onSetup(setupMessage);
            case StartMessage startMessage                                             -> onStart(startMessage);
            case AvailableDirectionsReplyMessage availableDirectionsReplyMessage       -> onAvailableDirectionsReply(availableDirectionsReplyMessage);
            case ChosenDirectionIgnoredMessage chosenDirectionIgnoredMessage           -> onChosenDirectionIgnored(chosenDirectionIgnoredMessage);
            case TargetReachedMessage targetReachedMessage                             -> onTargetReached(targetReachedMessage);
            case PauseMessage pauseMessage                                             -> onPause(pauseMessage);
            case ResumeMessage resumeMessage                                           -> onResume(resumeMessage);
            case DeregisterMessage deregisterMessage                                   -> onDeregister(deregisterMessage);
            case ChosenDirectionMessage chosenDirectionMessage                         -> onChosenDirection(chosenDirectionMessage);
            case AvailableDirectionsRequestMessage availableDirectionsRequestMessage   -> onAvailableDirectionsRequest(availableDirectionsRequestMessage);
            case ListingResponse listingResponse                                       -> onListingResponse(listingResponse);
            case RegisterMessage registerMessage                                       -> onRegister(registerMessage);
            case UnregisteredMessage unregisteredMessage                               -> onUnregister(unregisteredMessage);

            default -> throw new IllegalStateException("Unexpected value: " + message);
        };
    }

    private Behavior<Message> onAvailableDirectionsReply(AvailableDirectionsReplyMessage availableDirectionsReplyMessage){

        return this;
    }

    private Behavior<Message> onSetup(SetupMessage setupMessage){

        return this;
    }

    private Behavior<Message> onStart(StartMessage startMessage){

        return this;
    }

    private Behavior<Message> onChosenDirectionIgnored(ChosenDirectionIgnoredMessage chosenDirectionIgnoredMessage){

        return this;
    }

    private Behavior<Message> onTargetReached(TargetReachedMessage targetReachedMessage){

        return this;
    }

    private Behavior<Message> onPause(PauseMessage pauseMessage){

        return this;
    }

    private Behavior<Message> onResume(ResumeMessage resumeMessage){

        return this;
    }

    private Behavior<Message> onUnregister(UnregisteredMessage unregisteredMessage){

        return this;
    }

    private Behavior<Message> onListingResponse(ListingResponse listingResponse) {
        getContext().getLog().info("Received listing from receptionist");
        for (ActorRef<Message> boardRef : listingResponse.listing.getServiceInstances(serviceKeyForBoard)) {
            this.boardRef = boardRef;
            getContext().getLog().info("Stored board reference from receptionist");
        }
        return this;
    }

    private Behavior<Message> onPing() {
        getContext().getLog().info("Bot {} got pinged", actorName);
        if(boardRef != null){
            boardRef.tell(new PingResponseMessage(actorName, getContext().getSelf()));
            getContext().getLog().info("Bot {} responded to the ping", actorName);
        }
        else{
            getContext().getLog().info("No board reference found");
        }
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

    private Behavior<Message> onRegister(RegisterMessage message) {
        getContext().getLog().info("Bot registered: {}", message.name());

        // registration logic..


        return this;
    }
}
