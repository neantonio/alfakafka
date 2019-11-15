package ru.alfastrah.vhi.kafkatest.kafka.annotation;

public @interface ReturnBehavior {
    Type value() default Type.RETURN_PROXY;

    enum Type{
        WAIT_EXECUTION,
        RETURN_PROXY,
        RETURN_NULL,
        RETURN_NEW_INSTANCE
    }
}


