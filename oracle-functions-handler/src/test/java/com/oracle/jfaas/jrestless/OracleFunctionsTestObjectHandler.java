package com.oracle.jfaas.jrestless;

import com.oracle.faas.api.InputEvent;

public class OracleFunctionsTestObjectHandler extends OracleFunctionsRequestHandler {

    protected OracleFunctionsTestObjectHandler(){

    }

    public WrappedOutput handleRequest(InputEvent inputEvent){
        return inputEvent.consumeBody((inputStream) -> {
            OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, inputStream);
            return this.delegateRequest(wrappedInput);
        });
    }
}
