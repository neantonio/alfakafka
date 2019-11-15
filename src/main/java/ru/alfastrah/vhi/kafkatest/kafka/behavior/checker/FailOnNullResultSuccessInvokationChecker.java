package ru.alfastrah.vhi.kafkatest.kafka.behavior.checker;

public class FailOnNullResultSuccessInvokationChecker implements SuccessInvokationChecker {
    @Override
    public boolean canCommitOffset(Object invokationResult, Throwable exception) {
        return invokationResult==null;
    }
}
