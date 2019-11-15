package ru.alfastrah.vhi.kafkatest.general;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.alfastrah.vhi.kafkatest.general.annotation.RuntimeImplementation;
import ru.alfastrah.vhi.kafkatest.general.exception.ImplementationVariantsException;
import ru.alfastrah.vhi.kafkatest.kafka.KafkaMethodProcessor;
import ru.alfastrah.vhi.kafkatest.kafka.annotation.InvokeUsingKafka;
import ru.alfastrah.vhi.kafkatest.kafka.utils.KafkaHelper;


import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

@Component()
public class ExtAutowiredAnnotationBeanPostProcessor extends AutowiredAnnotationBeanPostProcessor implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeanCreationException {
        try{
            return super.postProcessPropertyValues(pvs, pds, bean, beanName);
        }
        catch (Exception e){
            return postProcessProperties(pvs, bean, beanName);
        }
    }

    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {

        Map<String, Object> fieldToImplement = new HashMap<>();
        Map<String, Field> fieldToImplementTypes = new HashMap<>();

        for(Field field:bean.getClass().getDeclaredFields()){
            List<Class> runtimeImplementationAnnotations = new ArrayList<>();

            if(field.getType().isInterface()){
                for(Annotation annotation: field.getType().getAnnotations()){
                    if(annotation.annotationType().isAnnotationPresent(RuntimeImplementation.class)){
                        runtimeImplementationAnnotations.add(annotation.annotationType());

                        for(Method method:field.getType().getDeclaredMethods()){
                            KafkaMethodProcessor kafkaMethodProcessor = applicationContext
                                    .getAutowireCapableBeanFactory()
                                    .getBean(KafkaMethodProcessor.class);
                            kafkaMethodProcessor.addTopicForReturn(KafkaHelper.getTopicNameToGetResult(field.getName(),method),method);
                        }
                    }
                }
            }
            if(runtimeImplementationAnnotations.size()==1){
                fieldToImplement.put(field.getName(), getImplementation(runtimeImplementationAnnotations.get(0), bean, beanName, field.getName(), field.getType()));
                fieldToImplementTypes.put(field.getName(),field);
            }
            else if(runtimeImplementationAnnotations.size()>1){
                throw new ImplementationVariantsException(runtimeImplementationAnnotations.size(), field, beanName);
            }

        }

        InjectionMetadata metadata;
        try {
            Method findAutowiringMetadataMethod = getClass().getSuperclass().getDeclaredMethod("findAutowiringMetadata",new Class[]{String.class, Class.class,PropertyValues.class});
            findAutowiringMetadataMethod.setAccessible(true);
            metadata = (InjectionMetadata) findAutowiringMetadataMethod.invoke(this,beanName,bean.getClass(),pvs);

            //удаляем из метадаты чтобы не было ошибок при автосвязывании
            for(String fieldName: fieldToImplement.keySet()){
                try {
                    Field injectedElementsField = metadata.getClass().getDeclaredField("injectedElements");
                    injectedElementsField.setAccessible(true);
                    Field checkedElementsField = metadata.getClass().getDeclaredField("checkedElements");
                    checkedElementsField.setAccessible(true);

                    List injectedElementsList = (List) injectedElementsField.get(metadata);
                    Set checkedElementsList = (Set) checkedElementsField.get(metadata);

                    Field beanField = fieldToImplementTypes.get(fieldName);
                    filterElementsCollection(injectedElementsList,beanField.getType());
                    filterElementsCollection(checkedElementsList,beanField.getType());

                    beanField.setAccessible(true);
                    beanField.set(bean,fieldToImplement.get(fieldName));

                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }

        } catch (NoSuchMethodException e) {
            return super.postProcessPropertyValues(pvs,null,bean,beanName);
        } catch (IllegalAccessException e) {
            return super.postProcessPropertyValues(pvs,null,bean,beanName);
        } catch (InvocationTargetException e) {
            return super.postProcessPropertyValues(pvs,null,bean,beanName);
        }

        try {
            metadata.inject(bean, beanName, pvs);
            return pvs;
        } catch (BeanCreationException var6) {
            throw var6;
        } catch (Throwable var7) {
            throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", var7);
        }
    }

    private void filterElementsCollection(Collection list, Class aClass) {
        List itemsToRemove = new ArrayList();
        list.forEach(item ->{
            if(item.toString().contains(aClass.getName())){
                itemsToRemove.add(item);
            }
        });
        list.removeAll(itemsToRemove);
    }

    private Object getImplementation(Class annotation, Object bean, String beanName, String fieldName, Class<?> type) {
        class Handler implements InvocationHandler {

            private Object implementation;

            public Handler() {

            }
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException {
                return args == null? method.invoke(getImplementation()) : method.invoke(getImplementation(),args);
            }

            public Object getImplementation() {
                if(implementation == null) {
                    ImplementationDispatcher dispatcher = (ImplementationDispatcher) (implementation = applicationContext
                                                .getAutowireCapableBeanFactory()
                                                .getBean(ImplementationDispatcher.class));
                            implementation =dispatcher.getImplementation(annotation,bean,beanName,fieldName,type);
                }
                return implementation;
            }
        }

        return Proxy.newProxyInstance(bean.getClass().getClassLoader(),
                new Class[] { type },
                new Handler());

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
