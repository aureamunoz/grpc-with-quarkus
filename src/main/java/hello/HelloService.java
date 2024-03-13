package hello;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static hello.HelloOuterClass.*;

@GrpcService
public class HelloService implements Hello {
    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item(() -> HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
    }

    @Override
    public Multi<HelloReply> streamHello(HelloOuterClass.Void request) {
        Random random = new Random();
        int i = random.nextInt(22);
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1)).onItem()
                .transform(s -> HelloReply.newBuilder().setMessage(randomString(i)).build());
    }

    /**
     * Generates a random string of a specified length.
     *
     * @param longitud
     *            The length of the random string to be generated.
     *
     * @return A randomly generated string of the specified length.
     */
    public static String randomString(int longitud) {

        String lettersCollection = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        String result = "";
        for (int x = 0; x < longitud; x++) {
            int randomIndex = randomNumberBetween(0, lettersCollection.length() - 1);
            char randomChar = lettersCollection.charAt(randomIndex);
            result += randomChar;
        }
        return result;
    }

    /**
     * Generates a random integer within the specified range [min, max].
     *
     * @param min
     *            The minimum value (inclusive) of the range.
     * @param max
     *            The maximum value (inclusive) of the range.
     *
     * @return A random integer within the specified range [min, max].
     *
     * @throws IllegalArgumentException
     *             If min is greater than max.
     */
    public static int randomNumberBetween(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

}
