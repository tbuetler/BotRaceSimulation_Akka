/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */

/**
 * Module for the bot(s) of the bot-race actor system.
 */
module ch.bfh.akka.botrace.bot {

	// Non-JPMS modules
	requires akka.actor;
	requires akka.actor.typed;
	requires akka.cluster;
	requires akka.cluster.typed;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires typesafe.config;
	requires org.slf4j;
	requires scala.library;

	// See:
	// https://stackoverflow.com/questions/62815536/why-am-i-getting-the-error-noclassdeffounderror-sun-misc-unsafe-when-trying-t
	requires jdk.unsupported;

	requires ch.bfh.akka.botrace.common;

	// Exporting all (relevant) packages doesn't prevent the
	// generation of Javadoc for these packages.
	exports ch.bfh.akka.botrace.bot;
}
