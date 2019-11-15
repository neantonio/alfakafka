package ru.alfastrah.vhi.kafkatest.kafka.behavior.processor.error;

import ru.alfastrah.vhi.kafkatest.kafka.KafkaMethodProcessor;
import ru.alfastrah.vhi.kafkatest.kafka.exception.KafkaException;

import java.lang.reflect.Method;

public class ThrowingExceptionErrorBehaviorProcessor extends AbstractErrorBehaviorProcessor {
    @Override
    protected void processInternal(KafkaMethodProcessor.Handler proxy, Object bean, String beanName, String topicName, Method method, Object[] args) {
        proxy.exception.set(new KafkaException());
    }
}
