package org.lambci.dockerlambda.runtime.invocation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;

public class RequestHandlerInvoker implements HandlerInvoker {

    private final RequestHandler lambdaInstance;

    public RequestHandlerInvoker(Class<?> lambdaClass) throws IllegalAccessException, InstantiationException {
        this.lambdaInstance = (RequestHandler)lambdaClass.newInstance();
    }

    @Override
    public Object invoke(String input, Context context) throws InvocationException {
        Class<?> type = Arrays.stream(lambdaInstance.getClass().getDeclaredMethods())
                .filter(method -> method.getName().equals(RequestHandler.class.getDeclaredMethods()[0].getName()))
                .map(method -> method.getParameters()[0].getType())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find body parameter type of RequestHandler implementation method"));

        try {
            return lambdaInstance.handleRequest(new ObjectMapper().readValue(input, type), context);
        } catch (Exception e) {
            throw new InvocationException(e);
        }
    }
}
