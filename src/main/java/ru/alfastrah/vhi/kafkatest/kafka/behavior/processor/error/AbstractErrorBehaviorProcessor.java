package ru.alfastrah.vhi.kafkatest.kafka.behavior.processor.error;

import ru.alfastrah.vhi.kafkatest.kafka.KafkaMethodProcessor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractErrorBehaviorProcessor implements ErrorBehaviorProcessor {

    List<ErrorBehaviorProcessor> errorBehaviorProcessors = new ArrayList<>();

    public void process(KafkaMethodProcessor.Handler proxy, Object bean, String beanName, String topicName, Method method, Object[] args, Throwable e) {

        processInternal(proxy, bean, beanName, topicName, method, args);
        errorBehaviorProcessors.forEach(processor->{
            processor.process( proxy, bean, beanName, topicName, method, args, e);
        });

    }

    //хук для потомков
    protected abstract void processInternal(KafkaMethodProcessor.Handler proxy, Object bean, String beanName, String topicName, Method method, Object[] args);
}
