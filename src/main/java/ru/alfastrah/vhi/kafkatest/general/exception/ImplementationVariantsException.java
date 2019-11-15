package ru.alfastrah.vhi.kafkatest.general.exception;

import ru.alfastrah.vhi.kafkatest.general.annotation.RuntimeImplementation;

import java.lang.reflect.Field;

public class ImplementationVariantsException extends RuntimeException {
    public ImplementationVariantsException() {
    }

    public ImplementationVariantsException(String message) {
        super(message);
    }

    public ImplementationVariantsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImplementationVariantsException(Throwable cause) {
        super(cause);
    }

    public ImplementationVariantsException(int annotationsValue, Field field, String beanName) {
        super(" there are "
                +annotationsValue
                +" implementation variants for field "
                +field.getName()
                +" of bean "
                +beanName);
    }

    public ImplementationVariantsException(Field field, String beanName) {
        super(" there is no implementation variants for field "
                +field.getName()
                +" of bean "
                +beanName);
    }
}
