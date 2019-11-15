package ru.alfastrah.vhi.kafkatest.kafka.behavior;

import org.springframework.stereotype.Component;
import ru.alfastrah.vhi.kafkatest.kafka.behavior.en.ErrorCases;
import ru.alfastrah.vhi.kafkatest.kafka.behavior.en.MethodCases;
import ru.alfastrah.vhi.kafkatest.kafka.behavior.processor.error.DirectInvocationErrorBehaviorProcessor;

import java.util.HashMap;
import java.util.Map;

@Component
@SuppressWarnings("unchecked")
public class BehaviourProvider {

    private Map<String,Map<MethodCases,Object>> behaviorMap = new HashMap<>();
    private Map<MethodCases,Object> defaultParamsMap = new HashMap<>();

    public BehaviourProvider() {
        defaultParamsMap.put(MethodCases.USUAL_CALL,false);
        defaultParamsMap.put(MethodCases.ON_KAFKA_ERROR,true);
    }


    public <T> T getMethodBehaviour(String methodId, MethodCases param, Class<T> type){
        return (T) getParamsMap(methodId).get(methodId);
    }

    private Map<MethodCases, Object> initParamsMapByDefaultValues() {
        Map<MethodCases,Object> result = new HashMap<>(defaultParamsMap);
        return result;
    }

    private Map<MethodCases, Object> getParamsMap(String methodId){
        Map<MethodCases,Object> paramsMap = behaviorMap.get(methodId);
        if(paramsMap == null) {
            paramsMap = initParamsMapByDefaultValues();
            behaviorMap.put(methodId, paramsMap);
        }
        return paramsMap;
    }

    public void setMethodBehavior(String methodId, MethodCases param, Object value ){
        getParamsMap(methodId).put(param,value);
    }

    public DirectInvocationErrorBehaviorProcessor getFailBehaviour(String topicName, ErrorCases kafkaUnavailable) {
        return new DirectInvocationErrorBehaviorProcessor();
    }
}
