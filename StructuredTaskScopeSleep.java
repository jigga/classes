import jdk.incubator.concurrent.StructuredTaskScope;

import java.util.concurrent.*;

public class StructuredTaskScopeSleep {
    public static void main(String[] args) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<Object>()) {
            scope.fork(() -> {
                System.out.println("Returning from the forked task...");
                return "DONE";
            });
            TimeUnit.SECONDS.sleep(3);
            System.out.println("Joining...");
            scope.join();
        }
        System.out.println("DONE");
    }
}
