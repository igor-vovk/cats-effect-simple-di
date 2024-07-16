# Simple Dependency Injection for Cats-Effect

A tiny library that makes dependency injection with [cats-effect](https://github.com/typelevel/cats-effect) simple.
This is a follow-up of
the [article](https://medium.com/@ivovk/dependency-injection-with-cats-effect-resource-monad-ad7cd47b977) I wrote about
the topic.

Traditional approach to dependency injection with cats-effect is to build a single for-comprehension that wires all the
dependencies together. This approach is not very scalable and can become quite messy as the number of dependencies
grows.

The suggested approach with this library would be:

```scala
import io.github.cats_effect_simple_di.Allocator

// create a Dependencies object and class that holds all the dependencies:
object Dependencies {
  def create(runtime: IORuntime): Resource[IO, Dependencies] =
    Allocator.create(runtime).map(new Dependencies(_))
}

class Dependencies private(allocator: Allocator) {
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

// Use your dependencies in the main app class
object Main extends IOApp.Simple {
  override def run: IO[Unit] =
    Dependencies.create(runtime).use { dependencies =>
      // use your exit dependency here
      dependencies.myServer.useForever
    }
}
```

## What happens under the hood?

* `lazy val` solves the problem that dependencies are instantiated only when they are accessed and only one instance is
  created.
* `Allocator` is a wrapper around `Resource` that keeps track of the order of resource allocation and finalization. So
  when application is shut down, resources are shut down in the reverse order they were initialized.
* `Dependencies` initialization is wrapped in a `Resource` so that resources are shut down when the application
  finishes.

## Installation

Supported Scala versions: `3.x`

To install add the following to your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % Versions.catsEffect,
  "io.github.igor-vovk" %% "cats-effect-simple-di" % Versions.simpleDi,
)
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
