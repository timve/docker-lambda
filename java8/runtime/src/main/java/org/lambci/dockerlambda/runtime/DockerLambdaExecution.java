package org.lambci.dockerlambda.runtime;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lambci.dockerlambda.runtime.invocation.HandlerInvoker;

import java.io.PrintStream;
import java.util.Date;

class DockerLambdaExecution {

    private Date start;
    private boolean started;

    private final Context context;
    private final HandlerInvoker invoker;

    private final PrintStream outputStream;
    private final PrintStream errorStream;

    private boolean hasErrors;
    private String timeout;

    DockerLambdaExecution(HandlerInvoker invoker, Context context, String timeout, PrintStream outputStream, PrintStream errorStream) {
        this.timeout = timeout;
        this.invoker = invoker;
        this.context = context;
        this.outputStream = outputStream;
        this.errorStream = errorStream;
    }

    void run(String body) {
        reportStart();
        started = true;
        start = new Date();


        Object result = null;
        try {
            result = invoker.invoke(body, context);
        } catch (Exception e) {
            hasErrors = true;
            reportFault(e);
        }

        reportDone(result);
    }

    private void reportStart() {
        outputStream.print(String.format("START RequestId: %s Version: %s", context.getAwsRequestId(), context.getFunctionVersion()));
    }

    private void reportFault(Throwable throwable) {
        errorStream.println(throwable.getMessage());
        throwable.printStackTrace(errorStream);
    }

    private void reportDone(Object result) {
        if (!started) {
            outputStream.println(String.format("END RequestId: %s", context.getAwsRequestId()));
        }

        Date end = new Date();
        long duration = (end.getTime() - start.getTime());
        long billed = Math.min(100 * ((duration / 100) + 1), Integer.parseInt(timeout) * 1000L);
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

        outputStream.print(String.format("REPORT RequestId: %s Duration: %s ms Billed Duration: %s ms Memory Size: %s MB Max Memory Used: %s MB", context.getAwsRequestId(), duration, billed, context.getMemoryLimitInMB(), totalMemory));

        if (result != null) {
            try {
                new ObjectMapper().writer().writeValue(outputStream, result);
            } catch (Exception e) {
                errorStream.println("Could not write lambda return value");
                e.printStackTrace(errorStream);
            }

        }
    }

    boolean hasErrors() {
        return hasErrors;
    }
}