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
// create a Dependencies object:

import com.ihorvovk.cats_effect_simple_di.Allocator


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
    Allocator(runtime).map(new Dependencies(_))
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
