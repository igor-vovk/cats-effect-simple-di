# Cats-Effect-Simple-DI

A tiny library that allows you to make simple dependency injection with Cats Effect.
This is a follow-up of
the [article](https://medium.com/@ivovk/dependency-injection-with-cats-effect-resource-monad-ad7cd47b977) I wrote about
the topic.

Traditional approach to Dependency Injection with Cats Effect is to build a single for-comprehension that wires all the
dependencies together. This approach is not very scalable and can become quite messy as the number of dependencies
grows.

The suggested approach would be:

```scala
import io.github.cats_effect_simple_di.Allocator

// create a Dependencies object:
class Dependencies(allocator: Allocator) {
  // Suppose you need to instantiate a class that returns a Resource[F, A]
  // Then you can use the allocator to allocate the resource
  lazy val http4sClient: Client[IO] = allocator.allocate {
    EmberClientBuilder.default[IO].build
  }

  // Dependencies that don't need to be shut down can be used directly
  lazy val myClass: MyClass = new MyClass(http4sClient)

  // Dependencies will be shut down in the right order
  lazy val myServer: Server[IO] = allocator.allocate {
    BlazeServerBuilder[IO](runtime.compute)
      .bindHttp(8080, "localhost")
      .withHttpApp(myClass.httpApp)
      .resource
  }

}

// Then you can use the Dependencies object to create a Resource[F, A]
object Dependencies {
  def apply(runtime: IORuntime): Resource[IO, Dependencies] =
    Allocator.create(runtime).map(new Dependencies(_))
}

// Use your dependencies in the main app class
object Main extends IOApp.Simple {
  override def run: IO[Unit] = {
    Dependencies(runtime).use { dependencies =>
      // use your exit dependency here
      dependencies.myServer.useForever
    }
  }
}
```

## Debugging allocation order

If you want to see the order of initialization and finalization of resources, use `LogbackAllocationListener` when
creating an `Allocator` object. This will log the allocation and finalization of resources in the order they happen:

```scala
import io.github.cats_effect_simple_di.AllocationLifecycleListener

Allocator.create(runtime, LogbackAllocationListener)
```

## Modularization

You can have multiple dependencies objects and combine them together. In this case, you can either reuse the same
`Allocator` object or create a new one for each dependency object, but wrap their instantiation
in `allocator.allocate{}` so that they are shut down in the right order:

Example reusing the same `Allocator` object:
```scala
// AWS - specific dependencies
class AwsDependencies(allocator: Allocator) {
  lazy val s3Client: S3Client = allocator.allocate {
    S3ClientBuilder.default.build
  }
}

// Main application dependencies
object Dependencies {
  def create(runtime: IORuntime): Resource[IO, Dependencies] =
    Allocator.create(runtime).map(new Dependencies(_))
}

class Dependencies(allocator: Allocator) {
  val aws = new AwsDependencies(allocator)

  lazy val http4sClient: Client[IO] = allocator.allocate {
    EmberClientBuilder.default[IO].build
  }
}

object App extends IOApp.Simple {
  override def run: IO[Unit] = Dependencies.create(runtime).use { deps =>
    // use aws.s3Client here
    deps.aws.s3Client
  }
}
```

Example creating a new `Allocator` object for each Dependencies object:

```scala
// AWS - specific dependencies
object AwsDependencies {
  def create(runtime: IORuntime): Resource[IO, AwsDependencies] =
    Allocator.create(runtime).map(new AwsDependencies(_))
}

class AwsDependencies(allocator: Allocator) {
  lazy val s3Client: S3Client = allocator.allocate {
    S3ClientBuilder.default.build
  }
}

// Main application dependencies
object Dependencies {
  def create(runtime: IORuntime): Resource[IO, Dependencies] =
    Allocator.create(runtime).map(new Dependencies(_))
}

class Dependencies(allocator: Allocator) {
  lazy val aws = allocator.allocate {
    AwsDependencies.create(IORuntime.global)
  }

  lazy val http4sClient: Client[IO] = http4sAllocator.allocate {
    EmberClientBuilder.default[IO].build
  }
}

object App extends IOApp.Simple {
  override def run: IO[Unit] = Dependencies.create(runtime).use { deps =>
    // use aws.s3Client here
    deps.aws.s3Client
  }
}
```