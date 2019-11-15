package ru.alfastrah.vhi.kafkatest.kafka.exception;

public class KafkaException extends RuntimeException {
    public KafkaException() {
    }

    public KafkaException(String message) {
        super(message);
    }

    public KafkaException(String message, Throwable cause) {
        super(message, cause);
    }
}
