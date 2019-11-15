package ru.alfastrah.vhi.kafkatest.kafka.behavior.checker;

public class NotReturnResultOnExceptionInvokationChecker implements SuccessInvokationChecker{
    public boolean canReturnResult(Object invocationResult, Throwable exception){
        return exception==null;
    }
}
