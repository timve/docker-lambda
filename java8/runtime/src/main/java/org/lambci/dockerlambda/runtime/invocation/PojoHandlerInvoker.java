package org.lambci.dockerlambda.runtime.invocation;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

public class PojoHandlerInvoker implements HandlerInvoker {

    private final Object lambdaInstance;
    private final Method method;

    public PojoHandlerInvoker(Class<?> lambdaClass, String methodName) throws IllegalAccessException, InstantiationException {
        this.lambdaInstance = lambdaClass.newInstance();
        this.method = determineMethod(lambdaClass, methodName);
    }

    private static Method determineMethod(Class<?> lambdaClass, String methodName) {
        Method[] allMethods = lambdaClass.getDeclaredMethods();
        Comparator<Method> numberOfArgumentsDesc = Comparator.comparingInt(Method::getParameterCount).reversed();
        Comparator<Method> lastParameterIsContext = Comparator.comparing(PojoHandlerInvoker::lastParameterIsContext);

        return Arrays.stream(allMethods)
                .filter(method -> method.getName().equals(methodName))
                .sorted(numberOfArgumentsDesc.thenComparing(lastParameterIsContext))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Could not find lambda handler method '" + methodName + "'"));
    }

    private static boolean lastParameterIsContext(Method method) {
        return method.getParameterCount() > 0 && Context.class.equals(method.getParameterTypes()[method.getParameterTypes().length - 1]);
    }

    public Object invoke(String input, Context context) throws InvocationException {
        Class<?> type = method.getParameters()[0].getType();

        try {
            return lastParameterIsContext(method) ? method.invoke(lambdaInstance, new ObjectMapper().readValue(input, type), context) : method.invoke(lambdaInstance, new ObjectMapper().convertValue(input, type));
        } catch (Exception e) {
            throw new InvocationException(e);
        }
    }

}
