# grpc-training-with-quarkus

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Project generation and running the application in dev mode

1. Generate project

Go to the [code.quarkus.io](https://code.quarkus.io) site.
Select the gRPC extension.
Select `No` in Starter Code field. We will write the code ourselves.
Modify the default group and artifact names if needed.
Generate the application and download .zip file.

Alternatively, you can also generate the project by running the following command:

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.8.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=getting-started-on-quarkus-demo \
    -Dextensions="grpc"

```

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Adding gRPC services

### Create the service and message definitions. 

Quarkus expects the service and message definitions in the src/main/proto directory. This way, quarkus-maven-plugin will generate Java files from your proto files.
Create the src/main/proto/hello.proto file with the following content:

````protobuf
syntax="proto3";

package hello;

service Hello {
  rpc SayHello (HelloRequest) returns (HelloReply);
  rpc StreamHello (Void) returns (stream HelloReply);
}

message  HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}

message Void {

}
````

Before coding, we need to generate the classes used to implement and consume gRPC services. In a terminal, run:

```bash
mvn compile
```

Once generated, you can look at the target/generated-sources/grpc directory

### Implementing the service

Create the src/main/java/org/acme/HelloService.java file with the following content:

```java
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
        return Uni.createFrom().item(()->HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
    }

}


```

This is the class that implements the service itself. In quarkus, we need to implement an interface based on mutiny, which is the reactive programming api.
The grpc extension is reactive by default, so instead of having HelloReply as the response type, we have Uni<HelloReply>.
I also need to expose it as a CDI bean, so we annotate it with `@GrpcService`. This way Quarkus will detect it, deploy it to the grpc server and expose it as a bean so we can take advantage of all the bean benefits.
Protocolbuffer for Java follows a
builder pattern, so instead of instantiating directly, we use the builder to create the object and set whatever we want.


### The gRPC server

One cool thing is the grpc server. 
The services are served by a server. 
Available services (CDI beans) are automatically registered and exposed.

By default, the server is exposed on localhost:9000, and uses plain-text (so no TLS) when running normally, and localhost:9001 for tests.

You could use the `grpcurl` tool to send a request to our Hello grpc service in a terminal as follows:

```bash
grpcurl --plaintext -d '{"name":"Quarkus"}' localhost:9000 hello.Hello.SayHello
```

If everything went well you should see something like:

```bash
{
  "message": "Hello Quarkus"
}
```

### Handling Streams

gRPC allows receiving and returning streams. Add a new method in the .proto file that receives a stream from the server. 
Replace the content of the .proto file by the following:

```protobuf
syntax="proto3";

package hello;

service Hello {
  rpc SayHello (HelloRequest) returns (HelloReply);
  rpc StreamHello (Void) returns (stream HelloReply);
}

message  HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}

message Void {

}
```

The StreamHello operations returns a stream. 
Using Mutiny, you can implement it as follows:

```java
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
        return Uni.createFrom().item(()->HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
    }

    @Override
    public Multi<HelloReply> streamHello(HelloOuterClass.Void request) {
        Random random = new Random();
        int i = random.nextInt(22);
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1)).onItem().transform(s-> HelloReply.newBuilder().setMessage(randomString(i)).build());
    }
    
    
    /**
     * Generates a random string of a specified length.
     *
     * @param longitud The length of the random string to be generated.
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
     * @param min The minimum value (inclusive) of the range.
     * @param max The maximum value (inclusive) of the range.
     * @return A random integer within the specified range [min, max].
     * @throws IllegalArgumentException If min is greater than max.
     */
    public static int randomNumberBetween(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

}


```
## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/grpc-training-with-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

https://quarkus.io/guides/grpc-getting-started
https://quarkus.io/guides/grpc-service-implementation


