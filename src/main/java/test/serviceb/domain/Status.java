package test.serviceb.domain;

/**
 * Enum representing the various statuses an order can have.
 * The {@code Status} enum defines the possible states of an order and is
 * used to indicate the current state of an order in the system.
 * Possible values:
 * - {@code CONFIRMED}: The order has been confirmed and is ready for processing.
 * - {@code SHIPPED}: The order has been shipped to the customer.
 * - {@code CANCELLED}: The order has been cancelled.
 */
public enum Status {
  CONFIRMED, SHIPPED, CANCELLED
}
