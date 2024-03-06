package hello;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

@QuarkusTest
public class HelloServiceTest {

    @GrpcClient
    Hello helloService;

    @Test
    public void shouldSayHello(){
        Uni<HelloOuterClass.HelloReply> from_tests = helloService.sayHello(HelloOuterClass.HelloRequest.newBuilder().setName("from Tests").build());

        HelloOuterClass.HelloReply helloReply = from_tests.await().atMost(Duration.ofSeconds(5));
        Assertions.assertEquals(helloReply.getMessage(), "Hello from Tests");
    }
}
