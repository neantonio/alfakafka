package ru.alfastrah.vhi.kafkatest.kafka.utils;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Base64;

@Component
public class DefaultSerializationUtil {

    public Object serialize(Object source)  {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(source);
            oos.flush();
            oos.close();
            byte[] byteArr = bos.toByteArray();
            return Base64.getEncoder().encodeToString(byteArr);
        }
        catch (Exception e){
            return null;
        }
    }

    public Object[] deserialize(String source)  {
        final byte[] bytes = Base64.getDecoder().decode(source);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return (Object[]) in.readObject();
        }
        catch (Exception e){
            return null;
        }
    }
}
