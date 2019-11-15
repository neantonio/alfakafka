package ru.alfastrah.vhi.kafkatest.kafka.utils;

public interface SerializationUtil {
    Object serialize(Object source);
    Object[] deserialize(String source);

    Object deserializeSingleObject(String source);
}
