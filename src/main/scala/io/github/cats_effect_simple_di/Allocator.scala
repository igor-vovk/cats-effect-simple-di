package io.github.cats_effect_simple_di

import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Ref, Resource}

import scala.reflect.ClassTag

object Allocator {

  def create(
    runtime: IORuntime,
    listener: AllocationLifecycleListener = NoOpListener,
  ): Resource[IO, Allocator] = {
    val acquire = IO(unsafeCreate(runtime, listener))
    val release = (a: Allocator) => a.shutdownAll
    Resource.make(acquire)(release)
  }

  /**
   * When using this method you must call [[shutdownAll]] manually after you finished working with dependencies.
   */
  def unsafeCreate(
    runtime: IORuntime,
    listener: AllocationLifecycleListener = NoOpListener,
  ): Allocator =
    new Allocator(runtime, listener)

}

class Allocator private(
  runtime: IORuntime,
  listener: AllocationLifecycleListener,
) {

  private val shutdown: Ref[IO, IO[Unit]] = Ref.unsafe(IO.unit)

  def allocate[A: ClassTag](resource: Resource[IO, A]): A =
    resource
      .allocated
      .flatMap { case (a, release) =>
        listener.onInit(a) *>
          shutdown.update(listener.onShutdown(a) *> release *> _) *> // Shutdown this resource, and after shutdown all previous
          IO.pure(a)
      }.unsafeRunSync()(runtime)

  def allocate[A: ClassTag](io: IO[A]): A = allocate(io.toResource)

  def shutdownAll: IO[Unit] = {
    shutdown.getAndSet(IO.unit).flatten
  }

}
