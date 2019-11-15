package ru.alfastrah.vhi.kafkatest.kafka.behavior.checker;

public interface SuccessInvokationChecker {
    default boolean canCommitOffset(Object invokationResult, Throwable exception) {
        return true;
    }

    default boolean canReturnResult(Object invocationResult, Throwable exception){
        return true;
    }
}
