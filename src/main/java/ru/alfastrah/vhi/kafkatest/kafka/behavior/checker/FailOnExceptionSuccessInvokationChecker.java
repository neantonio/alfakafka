package ru.alfastrah.vhi.kafkatest.kafka.behavior.checker;

public class FailOnExceptionSuccessInvokationChecker implements SuccessInvokationChecker {
    @Override
    public boolean canCommitOffset(Object invokationResult, Throwable exception) {
        return exception==null;
    }
}
