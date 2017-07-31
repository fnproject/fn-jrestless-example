package com.oracle.faas.jrestlessexample;

import com.oracle.faas.api.InputEvent;
import com.oracle.faas.api.OutputEvent;

public class OracleFunctionsTestObjectHandler extends OracleFunctionsRequestHandler{

    protected OracleFunctionsTestObjectHandler(){

    }

    public OutputResponse handleRequest(InputEvent inputEvent){
        return inputEvent.consumeBody((inputStream) -> {
            OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, inputStream);
            return this.delegateRequest(wrappedInput);
        });
    }
}
