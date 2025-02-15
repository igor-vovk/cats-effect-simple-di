package io.github.cats_effect_simple_di

import cats.effect.implicits.*
import cats.effect.std.Dispatcher
import cats.effect.unsafe.IORuntime
import cats.effect.*
import cats.implicits.*

import scala.reflect.ClassTag

object Allocator {

  def create[F[_]: Async](): Resource[F, Allocator[F]] =
    for
      dispatcher  <- Dispatcher.parallel[F]
      shutdownRef <- Ref.of(Async[F].unit).toResource
      allocator <- {
        val acquire = Async[F].delay(Allocator(dispatcher, shutdownRef, NoOpListener[F]))
        val release = (a: Allocator[F]) => a.shutdownAll

        Resource.make(acquire)(release)
      }
    yield allocator

  /**
   * When using this method you must call [[shutdownAll]] manually after you finished working with
   * dependencies.
   */
  def unsafeCreate(runtime: IORuntime): Allocator[IO] =
    create[IO]().allocated.unsafeRunSync()(runtime)._1

}

class Allocator[F[_]: Sync] private (
  dispatcher: Dispatcher[F],
  shutdown: Ref[F, F[Unit]],
  listener: AllocationLifecycleListener[F],
) {

  def withListener(listener: AllocationLifecycleListener[F]): Allocator[F] =
    new Allocator(dispatcher, shutdown, listener)

  def allocate[A: ClassTag](resource: Resource[F, A]): A =
    dispatcher.unsafeRunSync {
      resource
        .allocated
        .flatMap { (acquired, release) =>
          listener.onInit(acquired) *>
            shutdown.update(listener.onShutdown(acquired) *> release *> _) *>
            // Shutdown this resource, and after shutdown all previous
            Sync[F].pure(acquired)
        }
    }

  def allocate[A: ClassTag](fa: F[A]): A = allocate(fa.toResource)

  def shutdownAll: F[Unit] =
    shutdown.getAndSet(Sync[F].unit).flatten

}
