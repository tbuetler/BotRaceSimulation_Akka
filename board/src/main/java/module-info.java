/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */

/**
 * Module for the board of the bot-race actor system.
 */
module ch.bfh.akka.actorrace.board {

	// Non-JPMS modules
	requires akka.actor;
	requires akka.actor.typed;
	requires akka.cluster;
	requires akka.cluster.typed;
	requires typesafe.config;
	requires org.slf4j;
	requires scala.library;

	// See:
	// https://stackoverflow.com/questions/62815536/why-am-i-getting-the-error-noclassdeffounderror-sun-misc-unsafe-when-trying-t
	requires jdk.unsupported;

	requires ch.bfh.akka.botrace.common;
	requires transitive javafx.controls;
	requires javafx.fxml;
	// requires javafx.graphics; // perhaps necessary

	// Exporting all (relevant) packages doesn't prevent the
	// generation of Javadoc for these packages.
	exports ch.bfh.akka.botrace.board.actor;
	exports ch.bfh.akka.botrace.board.cli;
	exports ch.bfh.akka.botrace.board.model;
	exports ch.bfh.akka.botrace.board.gui; // Required by JavaFX, too

}
