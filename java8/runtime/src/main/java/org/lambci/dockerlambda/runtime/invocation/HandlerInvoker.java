package org.lambci.dockerlambda.runtime.invocation;

import com.amazonaws.services.lambda.runtime.Context;

public interface HandlerInvoker {

    <O> O invoke(String input, Context context) throws InvocationException;

    class InvocationException extends Exception {
        public InvocationException(Exception e) {
            super(e);
        }
    }
}
