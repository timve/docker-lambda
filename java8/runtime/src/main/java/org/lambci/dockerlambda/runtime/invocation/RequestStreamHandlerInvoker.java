package org.lambci.dockerlambda.runtime.invocation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class RequestStreamHandlerInvoker implements HandlerInvoker {

    private final RequestStreamHandler lambdaInstance;

    public RequestStreamHandlerInvoker(Class<?> lambdaClass) throws IllegalAccessException, InstantiationException {
        this.lambdaInstance = (RequestStreamHandler)lambdaClass.newInstance();
    }

    @Override
    public String invoke(String input, Context context) throws InvocationException {
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            lambdaInstance.handleRequest(inputStream, outputStream, context);
            byte[] result = outputStream.toByteArray();
            return new String(result);
        } catch (Exception e) {
            throw new InvocationException(e);
        }
    }
}
