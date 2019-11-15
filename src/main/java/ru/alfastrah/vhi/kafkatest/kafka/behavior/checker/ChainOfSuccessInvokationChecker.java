package ru.alfastrah.vhi.kafkatest.kafka.behavior.checker;

import java.util.ArrayList;
import java.util.List;

public class ChainOfSuccessInvokationChecker implements SuccessInvokationChecker {

    List<SuccessInvokationChecker> checkers = new ArrayList<>();

    @Override
    public boolean canCommitOffset(Object invokationResult, Throwable exception) {
        for(SuccessInvokationChecker checker:checkers){
            if(!checker.canCommitOffset(invokationResult, exception)) return false;
        }
        return true;
    }

    public void addChecker(SuccessInvokationChecker checker){
        checkers.add(checker);
    }
}
