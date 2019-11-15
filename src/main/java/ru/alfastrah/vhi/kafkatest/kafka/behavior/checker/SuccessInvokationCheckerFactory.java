package ru.alfastrah.vhi.kafkatest.kafka.behavior.checker;

import ru.alfastrah.vhi.kafkatest.kafka.annotation.SuccessCheck;


public class SuccessInvokationCheckerFactory {

    static SuccessInvokationChecker defaultChecker = new ChainOfSuccessInvokationChecker();

    public static SuccessInvokationChecker createChecker(SuccessCheck successCheck){
        ChainOfSuccessInvokationChecker checkersChain = new ChainOfSuccessInvokationChecker();
        if(successCheck.repeatOnException()){
            checkersChain.addChecker(new FailOnExceptionSuccessInvokationChecker());
        }
        if(successCheck.repeatOnNullResult()){
            checkersChain.addChecker(new FailOnNullResultSuccessInvokationChecker());
        }
        if(!successCheck.returnResultOnException()){
            checkersChain.addChecker(new NotReturnResultOnExceptionInvokationChecker());
        }

        return checkersChain;
    }

    public static SuccessInvokationChecker getDefaultChecker(){
        return defaultChecker;
    }
}
