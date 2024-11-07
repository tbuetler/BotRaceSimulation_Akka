/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */

/**
 * Module for the interface between bot-race actors.
 */
module ch.bfh.akka.botrace.common {
	requires akka.actor.typed;
	requires com.fasterxml.jackson.annotation;
	requires akka.serialization.jackson;

	requires scala.library;

	// Automatic module
	// See:
	// https://stackoverflow.com/questions/46501388/unable-to-derive-module-descriptor-for-auto-generated-module-names-in-java-9

	// Exporting all (relevant) packages doesn't prevent the
	// generation of Javadoc for these packages.
	exports ch.bfh.akka.botrace.common;
	opens ch.bfh.akka.botrace.common;
    exports ch.bfh.akka.botrace.common.botmessage;
    opens ch.bfh.akka.botrace.common.botmessage;
	exports ch.bfh.akka.botrace.common.boardmessage;
	opens ch.bfh.akka.botrace.common.boardmessage;
}
