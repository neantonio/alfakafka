package ru.alfastrah.vhi.kafkatest.kafka.behavior.processor.error;

import ru.alfastrah.vhi.kafkatest.kafka.KafkaMethodProcessor;

import java.lang.reflect.Method;

public class LoggingErrorBehaviorProcessor extends AbstractErrorBehaviorProcessor {

    @Override
    protected void processInternal(KafkaMethodProcessor.Handler proxy, Object bean, String beanName, String topicName, Method method, Object[] args) {

    }
}
