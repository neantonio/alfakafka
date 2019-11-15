package ru.alfastrah.vhi.kafkatest.general;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

@Component
public class BeanFactoryPostProcessor implements org.springframework.beans.factory.config.BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        configurableListableBeanFactory
                .getBeanDefinition("org.springframework.context.annotation.internalAutowiredAnnotationProcessor")
                .setBeanClassName("ru.alfastrah.vhi.kafkatest.general.ExtAutowiredAnnotationBeanPostProcessor");

    }

}
