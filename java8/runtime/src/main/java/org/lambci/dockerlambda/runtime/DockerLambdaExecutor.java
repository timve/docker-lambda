package org.lambci.dockerlambda.runtime;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.lambci.dockerlambda.runtime.invocation.HandlerInvoker;
import org.lambci.dockerlambda.runtime.invocation.PojoHandlerInvoker;
import org.lambci.dockerlambda.runtime.invocation.RequestHandlerInvoker;
import org.lambci.dockerlambda.runtime.invocation.RequestStreamHandlerInvoker;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerLambdaExecutor {

    public static void main(String[] args) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        Map<String, String> env = System.getenv();
        String handlerIdentifier = args.length > 0 ? args[0] : env.getOrDefault("AWS_LAMBDA_FUNCTION_HANDLER", "lambda_function.lambda_handler");
        String body = args.length > 1 ? args[1] : env.getOrDefault("AWS_LAMBDA_EVENT_BODY", "{}");
        String timeout = env.getOrDefault("AWS_LAMBDA_FUNCTION_TIMEOUT", String.valueOf(300));

        Context context = new DockerLambdaContext();
        HandlerInvoker invoker = createInvoker(handlerIdentifier);

        DockerLambdaExecution execution = new DockerLambdaExecution(invoker, context, timeout, System.out, System.err);
        execution.run(body);

        System.exit(execution.hasErrors() ? 1 : 0);
    }

    /**
     * Create lambda method invocation adapter to abstract from various options
     * for implementing lambda classes
     * @param   handlerIdentifier The identifier of the method to call (formatted according to AWS specs: 'package.class::method-reference')
     * @return  the <code>HandlerInvoker</code> that can be used to trigger the lambda function
     * @throws  ClassNotFoundException  if the class provided in the <code>handleIdentifier</code> cannot be located
     * @throws  IllegalAccessException  if the class provided in the <code>handleIdentifier</code> or its nullary
     *          constructor is not accessible.
     * @throws  InstantiationException
     *          if the {@code Class} represents an abstract class,
     *          an interface, an array class, a primitive type, or void;
     *          or if the class has no nullary constructor;
     *          or if the instantiation fails for some other reason.
     */
    private static HandlerInvoker createInvoker(String handlerIdentifier) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Matcher matcher = Pattern.compile("(.*)::(.*)").matcher(handlerIdentifier);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("AWS_LAMBDA_FUNCTION_HANDLER should be formatted as: 'package.class::method-reference'");
        }

        Class<?> lambdaClass = Class.forName(matcher.group(1));

        if (RequestHandler.class.isAssignableFrom(lambdaClass)) {
            return new RequestHandlerInvoker(lambdaClass);
        } else if (RequestStreamHandler.class.isAssignableFrom(lambdaClass)) {
            return new RequestStreamHandlerInvoker(lambdaClass);
        } else {
            return new PojoHandlerInvoker(lambdaClass, matcher.group(2));
        }
    }

}
