package ru.alfastrah.vhi.kafkatest.kafka;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import ru.alfastrah.vhi.kafkatest.kafka.annotation.SuccessCheck;
import ru.alfastrah.vhi.kafkatest.kafka.behavior.BehaviourProvider;
import ru.alfastrah.vhi.kafkatest.kafka.behavior.checker.SuccessInvokationChecker;
import ru.alfastrah.vhi.kafkatest.kafka.behavior.checker.SuccessInvokationCheckerFactory;
import ru.alfastrah.vhi.kafkatest.kafka.behavior.en.ErrorCases;
import ru.alfastrah.vhi.kafkatest.kafka.behavior.processor.error.DirectInvocationErrorBehaviorProcessor;
import ru.alfastrah.vhi.kafkatest.kafka.utils.KafkaHelper;
import ru.alfastrah.vhi.kafkatest.kafka.utils.SerializationUtil;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component("kafkaProcessor")
@SuppressWarnings("unchecked")
public class KafkaMethodProcessor extends KafkaListenerAnnotationBeanPostProcessor {

    private final String KAFKA_BROKERS = "localhost:9092";
    private final String CLIENT_ID="client1";

    @Autowired
    private KafkaTemplate<Long, LinkedHashMap> kafkaDtoTemplate;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private SerializationUtil serializationUtil;

    @Autowired
    private BehaviourProvider behaviourProvider;

    private Set<String> topicNames = new HashSet<>();
    private Set<String> topicNamesForReturn = new HashSet<>();
    private Map<String, Object> beanMap = new HashMap<>();
    private Map<Integer, Handler> methodHandlerMap = new ConcurrentHashMap<>();
    private Map<String, SuccessInvokationChecker> methodCheckerMap = new ConcurrentHashMap<>();

    public abstract static class Handler implements MethodInterceptor{
        volatile public AtomicReference realInvocationResult = new AtomicReference();
        volatile public AtomicReference exception = new AtomicReference();
    }
    public interface HasProxySource {
        Object getSourceOfProxy();
    }


    public Object produceMessageWhileInvocation(Object bean, String beanName, String topicName, Method method, Object[] args){

        ObjectWrapper resultWrapper = new ObjectWrapper(null);

        LinkedHashMap<String, Object> arguments = makeArgumentsMap(beanName, method, args);

        class HandlerImpl extends Handler {

            private HandlerImpl(Throwable exception) {
                this.exception.set(exception);
            }
            private HandlerImpl() {
            }

            @Override
            public synchronized Object invoke(MethodInvocation methodInvocation) throws Throwable {

                if((realInvocationResult.get() == null)&&(exception.get() == null)){
                    wait();
                }
                if("getSourceOfProxy".equals(methodInvocation.getMethod().getName())){
                    return realInvocationResult;
                }
                if(realInvocationResult.get()!= null){
                    Method method = methodInvocation.getMethod();
                    Object target = realInvocationResult.get();
                    return method.invoke(target,methodInvocation.getArguments());
                }
                if(exception.get() != null){
                    throw (Throwable) exception.get();
                }
                return null; // TODO: 11.11.2019 кинуть эксепшн
            }
        }

        //проксируем результат
        HandlerImpl handler =null;
        if(!"void".equalsIgnoreCase(method.getReturnType().getSimpleName())&&(!Modifier.isFinal(method.getReturnType().getModifiers()))){
            try {
                Object invocationResult;
                invocationResult = instantiateMethodResult(method);
                handler = new HandlerImpl();
                ProxyFactory proxyFactory = new ProxyFactory();
                proxyFactory.setTarget(invocationResult);
                proxyFactory.addAdvice(handler);
                for(Class in: method.getReturnType().getInterfaces()){
                    proxyFactory.addInterface(in);
                }
                if(method.getReturnType().isInterface()){
                    proxyFactory.addInterface(method.getReturnType());
                }
                proxyFactory.addInterface(HasProxySource.class);
                resultWrapper.object = proxyFactory.getProxy();
            } catch (Exception e) {
                resultWrapper.object = new HandlerImpl(e);
            }

            arguments.put("resultHash",getMethodResultIdentifier(handler));   //размер результатов в очереди ограничен размерностью инта
        }



        ListenableFuture<SendResult<Long, LinkedHashMap>> kafkaSendResult = kafkaDtoTemplate.send(topicName, arguments);

        kafkaSendResult.addCallback(new ListenableFutureCallback<SendResult<Long, LinkedHashMap>>() {
            @Override
            public void onFailure(Throwable throwable) {
                DirectInvocationErrorBehaviorProcessor failBehaviour = behaviourProvider.getFailBehaviour(topicName, ErrorCases.KAFKA_UNAVAILABLE);
                failBehaviour.process((Handler) resultWrapper.object, bean, beanName, topicName, method, args, throwable);
            }

            @Override
            public void onSuccess(SendResult<Long, LinkedHashMap> longLinkedHashMapSendResult) {
            }
        });

        return resultWrapper.object;
    }

    private Object instantiateMethodResult(Method method) throws InstantiationException, IllegalAccessException {
        return KafkaHelper.instantiateMethodResult(method);
    }

    static class ObjectWrapper{
        ObjectWrapper(Object object) {
            this.object = object;
        }
        Object object;
    }

    private AtomicInteger atomicCounter = new AtomicInteger(0);
    private Integer getMethodResultIdentifier(Handler result) {
        Integer key = atomicCounter.incrementAndGet();
        methodHandlerMap.put(key, result);
        return key;
    }

    private LinkedHashMap<String, Object> makeArgumentsMap(String beanName, Method method, Object[] args) {
        LinkedHashMap result = new LinkedHashMap();

        result.put("beanName", beanName);
        result.put("methodName",method.getName());

        if(args!=null){
            Object[] argsWithoutProxies = new Object[args.length];
            for(int i=0;i<args.length;i++){
                if(args[i] instanceof HasProxySource){
                    argsWithoutProxies[i]=((HasProxySource)args[i]).getSourceOfProxy();
                }
                else{
                    argsWithoutProxies[i] = args[i];
                }
            }
            result.put("args", serializationUtil.serialize(argsWithoutProxies));
        }
        else{
            result.put("args", serializationUtil.serialize(args));
        }

        return result;
    }

    public void addTopic(String topicName, String beanName, Object originBean) {
        topicNames.add(topicName);
        beanMap.put(beanName, originBean);  //храним только непроксированные бины
    }

    public void addTopicForReturn(String mainTopicName,Method method) {
        if(!"void".equalsIgnoreCase(method.getReturnType().getSimpleName())){
            topicNamesForReturn.add(mainTopicName);
        }
    }

    public void initTopicsSubscription(){
        initTopicsSubscription(topicNames,"consumeMessageAndInvoke");
        initTopicsSubscription(topicNamesForReturn, "consumeMessageAndProcessReturnedProxy");
    }

    private void initTopicsSubscription(Collection<String> topicNames, String methodName){
        if(topicNames.size() == 0) return;

        KafkaListener listener = new KafkaListener(){

            @Override
            public Class<? extends Annotation> annotationType() {
                return KafkaListener.class;
            }

            @Override
            public String id() {
                return "";
            }

            @Override
            public String containerFactory() {
                return "singleFactory";
            }

            @Override
            public String[] topics() {
                return topicNames.toArray(new String[0]);
            }

            @Override
            public String topicPattern() {
                return "";
            }

            @Override
            public TopicPartition[] topicPartitions() {
                return  new TopicPartition[0];
            }

            @Override
            public String containerGroup() {
                return "";
            }

            @Override
            public String errorHandler() {
                return "";
            }

            @Override
            public String groupId() {
                return "";
            }

            @Override
            public boolean idIsGroup() {
                return false;
            }

            @Override
            public String clientIdPrefix() {
                return "";
            }

            @Override
            public String beanRef() {
                return "__listener";
            }

            @Override
            public String concurrency() {
                return "";
            }

            @Override
            public String autoStartup() {
                return "";
            }

            @Override
            public String[] properties() {
                return new String[0];
            }
        };
        try {
            // TODO: 08.11.2019 попробовать failOnMissing = true
            Producer<Long, LinkedHashMap> producer = createProducer();
            topicNames.forEach(topicName -> {
                ProducerRecord<Long, LinkedHashMap> record = new ProducerRecord(topicName, new LinkedHashMap());
                try {
                    Future<RecordMetadata> future = producer.send(record);
                    RecordMetadata recordMetadata = future.get();
                    // TODO: 14.11.2019 проверять доступность кафки
                }
                catch (Exception ignored) {
                }
            });
            Method method = getClass().getDeclaredMethod(methodName, LinkedHashMap.class , Acknowledgment.class);
            this.processKafkaListener(listener, method, this, "kafkaProcessor");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    //вызывается на вызываемой стороне
    private void consumeMessageAndInvoke(LinkedHashMap<String, Object> arguments, Acknowledgment acknowledgment){
        try {
            String beanName = (String) arguments.get("beanName");
            Object bean = getBean(beanName);
            String methodName = (String) arguments.get("methodName");
            Object[] args = serializationUtil.deserialize((String) arguments.get("args"));
            Method method = getMethod(bean, methodName,args);
            Object invocationResult = null;
            Throwable exception = null;
            SuccessInvokationChecker successInvokationChecker = getSuccessChecker(bean, beanName,method);

            if((bean!=null)&&(method!=null)){
                try{
                    invocationResult = args == null? method.invoke(bean) : method.invoke(bean, args);
                    if(successInvokationChecker.canCommitOffset(invocationResult,null)){
                        acknowledgment.acknowledge();
                    }
                    else{
                        acknowledgment.nack(500);
                    }
                }
                catch (Exception e){
                    exception = e;
                    if(successInvokationChecker.canCommitOffset(null,e)){
                        acknowledgment.acknowledge();
                    }
                    else{
                        acknowledgment.nack(500);
                    }
                }
            }
            else return;

            if(!"void".equalsIgnoreCase(method.getReturnType().getSimpleName())){

                //если вызов был внутри модуля
                Integer resultHash = (Integer) arguments.get("resultHash");
                if(resultHash != null){
                    Handler handler = methodHandlerMap.get(resultHash);

                    if(handler != null){
                        handler.exception.set(exception);
                        handler.realInvocationResult.set(invocationResult);
                        synchronized (handler){
                            handler.notifyAll();
                        }
                        methodHandlerMap.remove(resultHash);
                    }
                    else{
                        //если снаружи, то отправляем сообщение
                        if(successInvokationChecker.canReturnResult(invocationResult,exception)){
                            arguments.put("invocationResult",serializationUtil.serialize(Collections.singletonList(invocationResult).toArray()));    //джексон хочет тип для десериализации, скармливаю ему Object[]
                            arguments.put("exception",exception == null? null : exception.getCause() == null? exception.getMessage(): exception.getCause().getMessage());
                            ListenableFuture<SendResult<Long, LinkedHashMap>> kafkaSendResult = kafkaDtoTemplate.send((String) arguments.get("beanName")+"."+methodName+".return", arguments);
                        }
                    }
                }
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private SuccessInvokationChecker getSuccessChecker(Object bean, String beanName, Method method) {
        StringBuilder builder = new StringBuilder();
        builder.append(beanName).append(".").append(method.getName());
        for(Class param:method.getParameterTypes()){
            builder.append(param.getName());
        }
        String key = builder.toString();
        SuccessInvokationChecker checker = methodCheckerMap.get(key);
        if(checker == null){
            SuccessCheck successCheck = KafkaHelper.getMethodAnnotation(method, SuccessCheck.class);
            if(successCheck != null){
                checker = SuccessInvokationCheckerFactory.createChecker(successCheck);
            }
            else{
                checker = SuccessInvokationCheckerFactory.getDefaultChecker();
            }
            methodCheckerMap.putIfAbsent(key, checker);
        }
        return checker;
    }


    //вызывается на вызывающей стороне для возврата результата
    private void consumeMessageAndProcessReturnedProxy(LinkedHashMap<String, Object> arguments, Acknowledgment acknowledgment){
        try {

            Object invocationResult = arguments.get("invocationResult");
            Object exception = arguments.get("exception");

            //если вызов был внутри модуля
            Integer resultHash = (Integer) arguments.get("resultHash");
            if(resultHash != null){
                Handler handler = methodHandlerMap.get(resultHash);
                if(handler == null) return;
                handler.exception.set(StringUtils.isEmpty(exception)? null:new RuntimeException((String)exception));
                Object deserializedResult = serializationUtil.deserializeSingleObject((String) invocationResult);
                handler.realInvocationResult.set(deserializedResult);
                synchronized (handler){
                    handler.notifyAll();
                }
                methodHandlerMap.remove(resultHash);

                acknowledgment.acknowledge(); // TODO: 14.11.2019 коммит оффсета при получении результата метода. всегда ли?
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private Producer createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, CLIENT_ID);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<Long, String>(props);
    }

    private Object[] makeArgumentsForInvocation(LinkedHashMap<String, Object> arguments) {
        return ((List) arguments.get("args")).toArray();
    }

    private Method getMethod(Object bean, String methodName, Object[] arguments) throws NoSuchMethodException {

        Method result;
        if(arguments!=null){
            Class[] classes = new Class[arguments.length];
            for(int i=0;i<arguments.length;i++){
                classes[i]=arguments[i].getClass();
            }
            result = bean.getClass().getMethod(methodName,classes);
        }
        else{
            result = bean.getClass().getMethod(methodName);
        }
        return result;

    }

    private Object getBean(String beanName) {
        Object result = beanMap.get(beanName);
        if(result == null) result = beanMap.get(beanName+"Impl");
        return result;
    }




}
