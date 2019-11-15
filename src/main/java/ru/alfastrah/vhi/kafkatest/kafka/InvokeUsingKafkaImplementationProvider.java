package ru.alfastrah.vhi.kafkatest.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.alfastrah.vhi.kafkatest.general.ImplementationProvider;
import ru.alfastrah.vhi.kafkatest.kafka.utils.KafkaHelper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component
public class InvokeUsingKafkaImplementationProvider implements ImplementationProvider {

    @Autowired
    private KafkaMethodProcessor kafkaMethodProcessor;

    @Override
    public Object getImplementation(Object bean, String beanName, String fieldName, Class<?> type) {
        class Handler implements InvocationHandler {

            private Handler() {
            }
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws IllegalArgumentException {

                // TODO: 13.11.2019 выбор имени бина
                return kafkaMethodProcessor.produceMessageWhileInvocation(bean,
                        fieldName,
                        KafkaHelper.getTopicNameToInvoke(fieldName,method),
                        method,
                        args);
            }
        }

        return Proxy.newProxyInstance(bean.getClass().getClassLoader(),
                new Class[] { type },
                new Handler());
    }
}
