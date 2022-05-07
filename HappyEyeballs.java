import jdk.incubator.concurrent.StructuredTaskScope;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HappyEyeballs {

    private final String hostname;
    private final InetAddress[] addresses;
    private final List<Future<Socket>> results;

    public HappyEyeballs(String hostname) throws UnknownHostException {
        this.hostname = hostname;
        this.addresses = InetAddress.getAllByName(hostname);
        if (this.addresses.length == 0) {
            throw new IllegalArgumentException("Could not resolve " + hostname + " to any IP address.");
        }
		System.out.println("Addresses:");
		System.out.println(Stream.of(addresses).map(InetAddress::toString).collect(Collectors.joining("\n\t", "\t", "")));
        this.results = new ArrayList<>(this.addresses.length);
    }

    public Socket connect() throws InterruptedException {
        try(var scope = new StructuredTaskScope.ShutdownOnSuccess<Socket>()) {
            // submit the first task instantly
            results.add(scope.fork(new Task(addresses[0], 443)));
            for (int i = 1; i < addresses.length; i++) {
                var prev = results.get(i-1);
                try {
                    prev.get(250, TimeUnit.MILLISECONDS);
					break;
                } catch (ExecutionException | TimeoutException e) {
					System.out.println("Got " + e + " when connecting to " + addresses[i-1]);
                    results.add(scope.fork(new Task(addresses[i], 443)));
                } catch (CancellationException e) { // subsequent task may be cancelled when the previous one finishes after the timeout
                    System.out.println("Got " + e + " when connecting to " + addresses[i-1]);
                    break;
                }
            }
			System.out.println("Joining...");
            scope.join();
            return scope.result(e -> new RuntimeException("Could not connect to " + hostname));
        }
    }

    class Task implements Callable<Socket> {

        private final InetAddress address;
        private final int port;

        public Task(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        public Socket call() throws Exception {
			var addressString = address.toString();
            System.out.println(Thread.currentThread() + " - connecting to: " + addressString);
            if (addressString.matches(".*[24]$")) {
                TimeUnit.MILLISECONDS.sleep(300);
            } else if (addressString.matches(".*[13579]$")) {
                TimeUnit.MILLISECONDS.sleep(100);
                throw new RuntimeException("You can't connect to this address: " + addressString);
            }
            var socket = new Socket(address, port);
            System.out.println("Successfully connected to: " + addressString);
            return socket;
        }

    }

    public static void main(String[] args) throws Exception {
		var host = args[0];
        var happyEyeballs = new HappyEyeballs(host);
        System.out.println(happyEyeballs.connect());
    }

}
