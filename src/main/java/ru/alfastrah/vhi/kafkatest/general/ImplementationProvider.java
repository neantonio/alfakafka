package ru.alfastrah.vhi.kafkatest.general;

import java.lang.reflect.Method;

//все реализации должны соответствовать конвенции имен "ИмяАннотации + ImplementationProvider"
public interface ImplementationProvider {
    Object getImplementation(Object bean, String beanName, String fieldName, Class<?> type);
}
