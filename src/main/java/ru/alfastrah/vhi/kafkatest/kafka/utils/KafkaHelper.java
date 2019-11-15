package ru.alfastrah.vhi.kafkatest.kafka.utils;

import org.aopalliance.intercept.MethodInterceptor;
import ru.alfastrah.vhi.kafkatest.general.ImplementationDispatcher;
import ru.alfastrah.vhi.kafkatest.kafka.annotation.InvokeUsingKafka;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@SuppressWarnings("unchecked")
public class KafkaHelper {
    public static <T> T getClassAnnotation(Class clazz, Class<T> annotationClass){
        T result = (T) clazz.getAnnotation(annotationClass);
        if(result == null){
            for(Class in:clazz.getInterfaces()){
                result = (T) in.getAnnotation(annotationClass);
                if(result != null) break;
            }
        }
        return result;
    }

    public static <T> T getInterfaceMethodAnnotation(Method method, Class<T> annotationClass){
        for(Class in:method.getDeclaringClass().getInterfaces()){
            try {
                Method methodOfInterface = in.getMethod(method.getName(),method.getParameterTypes());
                T result = getMethodAnnotation(methodOfInterface, annotationClass);
                if (result != null) return result;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    public static <T> T getMethodAnnotation(Method method, Class<T> annotationClass){
        T result = (T) method.getAnnotation((Class)annotationClass);
        if(result == null){
            result = getInterfaceMethodAnnotation(method, annotationClass);
        }
        return result == null ? getClassAnnotation(method.getDeclaringClass(),annotationClass) : result;
    }

    public static String getTopicNameToInvoke(String beanName, Method method){
        if(beanName.contains("Impl")){
            beanName = beanName.replace("Impl","");
        }
        InvokeUsingKafka invokeUsingKafka = getMethodAnnotation(method, InvokeUsingKafka.class);
        if(invokeUsingKafka != null){
            return "notSet".equals(invokeUsingKafka.topicName()) ? beanName+"."+method.getName() : invokeUsingKafka.topicName();
        }
        throw new RuntimeException("check InvokeUsingKafka before call getTopicName");
    }

    public static String getTopicNameToGetResult(String beanName, Method method){
        return getTopicNameToInvoke(beanName,method)+".return";
    }

    public static Object instantiateMethodResult(Method method) throws IllegalAccessException, InstantiationException {
        if(method.getReturnType().isInterface()){
            class Handler implements InvocationHandler {
                public Object invoke(Object proxy, Method method, Object[] args){
                    return null;
                }
            }
            return Proxy.newProxyInstance(method.getClass().getClassLoader(),
                    new Class[] {method.getReturnType() },
                    new Handler());
        }
        else {
            return method.getReturnType().newInstance();
        }
    }
}
