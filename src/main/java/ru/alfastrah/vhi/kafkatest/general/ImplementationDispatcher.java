package ru.alfastrah.vhi.kafkatest.general;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

@Component
public class ImplementationDispatcher {

    @Autowired
    List<ImplementationProvider> implementationProviders;


    public Object getImplementation(Class annotation, Object bean, String beanName, String fieldName, Class<?> type) {
        for(ImplementationProvider provider: implementationProviders){
            if(provider.getClass().getSimpleName().equalsIgnoreCase(annotation.getSimpleName()+"ImplementationProvider")){
                return provider.getImplementation(bean, beanName, fieldName, type);
            }
        }
        throw new IllegalArgumentException("no ImplementationProvider for "+annotation.getSimpleName());
    }
}
