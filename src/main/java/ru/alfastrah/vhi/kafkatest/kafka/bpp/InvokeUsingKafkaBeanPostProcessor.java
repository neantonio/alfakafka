package ru.alfastrah.vhi.kafkatest.kafka.bpp;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.alfastrah.vhi.kafkatest.kafka.KafkaMethodProcessor;
import ru.alfastrah.vhi.kafkatest.kafka.annotation.InvokeUsingKafka;
import ru.alfastrah.vhi.kafkatest.kafka.utils.KafkaHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * проксирует бины с аннотацией InvokeUsingKafka
 * накапливает названия топиков (имяБина.имяМетода)
 */
@Component
public class InvokeUsingKafkaBeanPostProcessor implements BeanPostProcessor {

    @Autowired
    KafkaMethodProcessor kafkaMethodProcessor;

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        //если есть аннотация, то проксируем и добавляем название очереди
        InvokeUsingKafka invokeUsingKafka = KafkaHelper.getClassAnnotation(bean.getClass(),InvokeUsingKafka.class);
        List<Method> methodsToProxy;

        if(invokeUsingKafka!=null){
            methodsToProxy = Arrays.asList(bean.getClass().getDeclaredMethods());
        }
        else {
            methodsToProxy = new ArrayList<Method>();
            for(Method method : bean.getClass().getDeclaredMethods()){
                if(method.isAnnotationPresent(InvokeUsingKafka.class)){
                    methodsToProxy.add(method);
                }
            }
        }

        if(!CollectionUtils.isEmpty(methodsToProxy)){

            methodsToProxy.forEach(method -> {
                InvokeUsingKafka invokeUsingKafkaForMethod = method.getAnnotation(InvokeUsingKafka.class);
                if(invokeUsingKafkaForMethod == null) invokeUsingKafkaForMethod=invokeUsingKafka;
                assert invokeUsingKafkaForMethod != null;
                String topicName = "notSet".equals(invokeUsingKafkaForMethod.topicName()) ?
                        beanName+"."+method.getName()
                        : invokeUsingKafkaForMethod.topicName();

                kafkaMethodProcessor.addTopic(KafkaHelper.getTopicNameToInvoke(beanName,method),beanName, bean);
            });

            class Handler implements MethodInterceptor {

                @Override
                public Object invoke(MethodInvocation methodInvocation) throws Throwable {
                    Method method = methodInvocation.getMethod();

                    if (methodsToProxy.contains(method)) {
                        return kafkaMethodProcessor.produceMessageWhileInvocation(bean, beanName,KafkaHelper.getTopicNameToInvoke(beanName,method) , method,methodInvocation.getArguments());
                    } else {
                        return method.invoke(bean,methodInvocation.getArguments());
                    }
                }
            }

            ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setTarget(bean);
            proxyFactory.addAdvice(new Handler());

            return proxyFactory.getProxy();
        }

        return bean;
    }
}
