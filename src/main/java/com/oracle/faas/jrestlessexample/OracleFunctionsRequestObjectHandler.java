package com.oracle.faas.jrestlessexample;

import com.oracle.faas.api.InputEvent;
import com.oracle.faas.api.OutputEvent;

public class OracleFunctionsRequestObjectHandler extends OracleFunctionsRequestHandler {

    protected OracleFunctionsRequestObjectHandler(){

    }

    public OutputEvent handleRequest(InputEvent inputEvent){
        return inputEvent.consumeBody((inputStream) -> {
            WrappedInput wrappedInput = new WrappedInput(inputEvent, inputStream);
            WrappedOutput response = this.delegateRequest(wrappedInput);
            return response.outputEvent;
        });
    }
}
