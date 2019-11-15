package ru.alfastrah.vhi.kafkatest.kafka.behavior.processor.error;

import ru.alfastrah.vhi.kafkatest.kafka.KafkaMethodProcessor;

import java.lang.reflect.Method;

public interface ErrorBehaviorProcessor {
    void process(KafkaMethodProcessor.Handler proxy, Object bean, String beanName, String topicName, Method method, Object[] args, Throwable e);
}
