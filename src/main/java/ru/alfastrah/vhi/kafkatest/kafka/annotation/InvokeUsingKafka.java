package ru.alfastrah.vhi.kafkatest.kafka.annotation;

import ru.alfastrah.vhi.kafkatest.general.annotation.RuntimeImplementation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.METHOD})
@RuntimeImplementation
public @interface InvokeUsingKafka {
    String topicName() default "notSet";
}
