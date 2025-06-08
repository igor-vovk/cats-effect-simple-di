# cedi [tsedi] – Cats-Effect Dependency Injection library

A tiny library that makes dependency injection with [cats-effect](https://github.com/typelevel/cats-effect) simple.
This is a follow-up of
an [article](https://medium.com/@ivovk/dependency-injection-with-cats-effect-resource-monad-ad7cd47b977) I wrote about
the topic.

Usually, what you want from a dependency injection library is to be able to:

- Define dependencies in a single place
- Instantiate dependencies only when they are needed
- Ensure that dependencies are shut down in the right order when the application finishes
- Instantiate dependencies only once, even if they are accessed multiple times
- Support modularization of dependencies, so that you can have multiple dependency objects and combine them together

The traditional approach to dependency injection with cats-effect is to build a single for-comprehension that wires all
dependencies together. This approach is not very scalable and can become quite messy as the number of dependencies
grows.

The suggested approach with this library would be:

1. Define a `Dependencies` class that holds all the dependencies.
2. Instantiate an `Allocator` and pass it to the `Dependencies` object. The `Allocator` is responsible for
   managing the lifecycle of resources and ensuring that they are shut down in the right order.
3. Use an `allocate` method to instantiate dependencies that return a `Resource[F, A]` or `F[A]`. This method will
   ensure that the resource is properly managed and shut down when the application finishes.
4. Use `lazy val` to ensure that dependencies are instantiated only once (if you need
   to instantiate a dependency multiple times, just use `def` instead of `lazy val`) and only when they are accessed.
5. Wrap the `Dependencies` object in a `Resource` so that resources are shut down automatically when the
   application finishes.
6. Use the `Dependencies` object in your main class extending `IOApp`, ensuring that all dependencies are available and
   properly managed.

Example usage:

```scala
import me.ivovk.cedi.syntax.* // Import necessary packages

// create a Dependencies object and class that holds all the dependencies:
object Dependencies {
  def create(): Resource[IO, Dependencies] =
    Allocator.create[IO]().map(Dependencies(using _))
}

class Dependencies(using AllocatorIO) {
  // Suppose you need to instantiate a class that returns a Resource[F, A]
  // Then you can use the allocator to allocate the resource
  lazy val http4sClient: Client[IO] = allocate {
    // `build` method returns a Resource[IO, Client[IO]]
    EmberClientBuilder.default[IO].build
  }

  // Dependencies that don't need to be shut down can be used directly
  lazy val myClass: MyClass = new MyClass(http4sClient)

  // It also supports dependencies that return an IO
  lazy val myDependency: MyDependency = allocate {
    IO(new MyDependency(http4sClient))
  }

  // Dependencies will be shut down in the right order
  lazy val myServer: Server[IO] = allocate {
    EmberServerBuilder.default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(myDependency.app)
      .build
  }

}

// Use your dependencies in the main app class
object Main extends IOApp.Simple {
  override def run: IO[Unit] =
    Dependencies.create().use { deps =>
      // use your main dependency here
      deps.myServer.useForever
    }
}
```

## Installation

![Maven Central](https://img.shields.io/maven-central/v/me.ivovk/cedi_3?style=flat-square&color=green)

Supported Scala versions: `>= 3.3.x`

To install, add the following to your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "me.ivovk" %% "cedi" % "{version}",
)
```

## Debugging allocation order

If you want to see the order of initialization and finalization of resources, use `LoggingAllocationListener` when
creating an `Allocator` object. This will log the allocation and finalization of resources in the order they happen:

```scala
import me.ivovk.cedi.{Allocator, LoggingAllocationListener}

Allocator.create[IO]().withListener(new LoggingAllocationListener[IO])
```

## Modularization

You can have multiple dependencies objects and combine them together. In this case, you can either reuse the same
`Allocator` object or create a new one for each dependency object, but wrap their instantiation
in `allocate { ... }` so that they are shut down in the right order:

Example reusing the same `Allocator` object:

```scala
import me.ivovk.cedi.syntax.*

// AWS - specific dependencies
class AwsDependencies(using AllocatorIO) {
  lazy val s3Client: S3Client = allocate {
    S3ClientBuilder.default.build
  }
}

// Main application dependencies
object Dependencies {
  def create(): Resource[IO, Dependencies] =
    Allocator.create[IO]().map(Dependencies(using _))
}

class Dependencies(using AllocatorIO) {
  val aws = new AwsDependencies

  lazy val http4sClient: Client[IO] = allocate {
    EmberClientBuilder.default[IO].build
  }
}

object App extends IOApp.Simple {
  override def run: IO[Unit] = Dependencies.create().use { deps =>
    // use aws.s3Client here
    deps.aws.s3Client
  }
}
```

Example creating a new `Allocator` object for each `Dependencies` object:

```scala
import me.ivovk.cedi.syntax.*

// AWS - specific dependencies
object AwsDependencies {
  def create(): Resource[IO, AwsDependencies] =
    Allocator.create[IO]().map(AwsDependencies(using _))
}

class AwsDependencies(using AllocatorIO) {
  lazy val s3Client: S3Client = allocate {
    S3ClientBuilder.default.build
  }
}

// Main application dependencies
object Dependencies {
  def create(): Resource[IO, Dependencies] =
    Allocator.create[IO]().map(new Dependencies(using _))
}

class Dependencies(using AllocatorIO) {
  lazy val aws = allocate {
    AwsDependencies.create()
  }

  lazy val http4sClient: Client[IO] = allocate {
    EmberClientBuilder.default[IO].build
  }
}

object App extends IOApp.Simple {
  override def run: IO[Unit] = Dependencies.create().use { deps =>
    // use aws.s3Client here
    deps.aws.s3Client
  }
}
```
