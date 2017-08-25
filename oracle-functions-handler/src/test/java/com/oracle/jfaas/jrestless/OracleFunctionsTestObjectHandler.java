package com.oracle.jfaas.jrestless;


import com.fnproject.fn.api.InputEvent;

public class OracleFunctionsTestObjectHandler extends OracleFunctionsRequestHandler {

    public WrappedOutput handleTestRequest(InputEvent inputEvent){
        return inputEvent.consumeBody((inputStream) -> {
            OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, inputStream);
            return this.delegateRequest(wrappedInput);
        });
    }
}
