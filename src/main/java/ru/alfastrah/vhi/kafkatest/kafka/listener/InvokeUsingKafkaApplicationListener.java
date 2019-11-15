package ru.alfastrah.vhi.kafkatest.kafka.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import ru.alfastrah.vhi.kafkatest.kafka.KafkaMethodProcessor;

@Component
public class InvokeUsingKafkaApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private KafkaMethodProcessor kafkaMethodProcessor;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
//        KafkaMethodProcessor kafkaMethodProcessor = (KafkaMethodProcessor) AppContext.getAppContext().get("kafkaProcessor");
        kafkaMethodProcessor.initTopicsSubscription();
    }
}
