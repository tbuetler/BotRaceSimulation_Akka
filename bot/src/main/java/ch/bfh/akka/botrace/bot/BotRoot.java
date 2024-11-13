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
import ch.bfh.akka.botrace.common.Direction;
import ch.bfh.akka.botrace.common.Message;
import ch.bfh.akka.botrace.common.boardmessage.*;
import ch.bfh.akka.botrace.common.boardmessage.PingMessage;
import ch.bfh.akka.botrace.common.botmessage.*;

import java.util.List;
import java.util.Random;

/**
 * The root actor of the Bot actor system.
 */
public class BotRoot extends AbstractOnMessageBehavior<Message> { // guardian actor
    Random random = new Random();

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

    private ActorRef<Message> boardRef;
    private final String actorName = getContext().getSelf().path().name();
    private final ActorRef<Message> botRef = getContext().getSelf();

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
            case ListingResponse listingResponse                                       -> onListingResponse(listingResponse);
            case UnregisteredMessage unregisteredMessage                               -> onUnregister(unregisteredMessage);

            default -> throw new IllegalStateException("Unexpected value: " + message);
        };
    }

    private Behavior<Message> onAvailableDirectionsReply(AvailableDirectionsReplyMessage message){
        List<Direction> directionList = message.directions();
        int random = new Random().nextInt(directionList.size());
        boardRef.tell(new ChosenDirectionMessage(directionList.get(random), this.botRef));

        return this;
    }

    private Behavior<Message> onSetup(SetupMessage setupMessage){

        return this;
    }

    private Behavior<Message> onStart(StartMessage startMessage){
        boardRef.tell(new AvailableDirectionsRequestMessage(botRef));
        return this;
    }

    private Behavior<Message> onChosenDirectionIgnored(ChosenDirectionIgnoredMessage chosenDirectionIgnoredMessage){
        boardRef.tell(new AvailableDirectionsRequestMessage(botRef));
        return this;
    }

    private Behavior<Message> onTargetReached(TargetReachedMessage targetReachedMessage){

        return this;
    }

    private Behavior<Message> onPause(PauseMessage pauseMessage){

        return this;
    }

    private Behavior<Message> onResume(ResumeMessage resumeMessage){
        boardRef.tell(new AvailableDirectionsRequestMessage(botRef));
        return this;
    }

    private Behavior<Message> onUnregister(UnregisteredMessage unregisteredMessage){
        getContext().getLog().info("Bot is deregistered ");
        return Behaviors.stopped();
    }

    private Behavior<Message> onListingResponse(ListingResponse listingResponse) {
        getContext().getLog().info("Received listing from receptionist");
        for (ActorRef<Message> boardRef : listingResponse.listing.getServiceInstances(serviceKeyForBoard)) {
            this.boardRef = boardRef;
            boardRef.tell(new RegisterMessage(actorName, botRef));
            getContext().getLog().info("Stored board reference from receptionist");
        }
        return this;
    }

    private Behavior<Message> onPing() {
        getContext().getLog().info("Bot {} got pinged", actorName);
        if(boardRef != null){
            boardRef.tell(new PingResponseMessage(actorName, botRef));
            getContext().getLog().info("Bot {} responded to the ping", actorName);
        }
        else{
            getContext().getLog().info("No board reference found");
        }
        return this;
    }
}