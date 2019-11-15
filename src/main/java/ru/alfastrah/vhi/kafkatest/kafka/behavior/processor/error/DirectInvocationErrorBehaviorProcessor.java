package ru.alfastrah.vhi.kafkatest.kafka.behavior.processor.error;

import ru.alfastrah.vhi.kafkatest.kafka.KafkaMethodProcessor;

import java.lang.reflect.Method;

public class DirectInvocationErrorBehaviorProcessor extends AbstractErrorBehaviorProcessor {

    protected void processInternal(KafkaMethodProcessor.Handler proxy, Object bean, String beanName, String topicName, Method method, Object[] args) {
        try {
            proxy.realInvocationResult.set(method.invoke(bean,args));
        } catch (Throwable e){
            proxy.exception.set(e);
        }
    }
}
