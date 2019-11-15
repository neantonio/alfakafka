package ru.alfastrah.vhi.kafkatest.kafka.utils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Primary
public class JacksonSerializationUtil implements SerializationUtil {
    ObjectMapper om = new ObjectMapper();

    @PostConstruct
    public void init(){
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_OBJECT);
    }

    public  Object serialize(Object source)  {
        try {
            return om.writeValueAsString(source);
        }
        catch (Exception e){
            return null;
        }
    }

    public Object[] deserialize(String source)  {

        try {
            return om.readValue(source, Object[].class);
        }
        catch (Exception e){
            return null;
        }
    }

    @Override
    public Object deserializeSingleObject(String source)  {

        try {
            Object result =  om.readValue(source, Object[].class);
            try{
                Object[] arr = (Object[]) result;
                if(arr.length==1) {
                    return arr[0];
                }
                else {
                    return result;
                }
            }
            catch (Exception e){
                return result;
            }
        }
        catch (Exception e){
            return null;
        }
    }
}
