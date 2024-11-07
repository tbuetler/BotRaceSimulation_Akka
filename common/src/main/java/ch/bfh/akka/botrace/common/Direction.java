/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.common;

/**
 * The 8 directions from which an actor can move plus the "no-move" direction.
 */
public enum Direction {
    /** North */ N,
    /** North East */ NE,
    /** East */ E,
    /** South East */ SE,
    /** South */ S,
    /** South West */ SW,
    /** West */ W,
    /** North West */ NW,
    /** Center means no move */ X
}
