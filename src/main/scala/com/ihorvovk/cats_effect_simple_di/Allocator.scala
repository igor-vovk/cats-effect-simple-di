package com.ihorvovk.cats_effect_simple_di

import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Ref, Resource}

object Allocator {

  def apply(runtime: IORuntime): Resource[IO, Allocator] =
    Resource.make {
      IO(unsafeCreate(runtime))
    } {
      _.shutdownAll
    }

  def unsafeCreate(runtime: IORuntime): Allocator =
    new Allocator()(runtime)

}

class Allocator(implicit runtime: IORuntime) {

  private val shutdown: Ref[IO, IO[Unit]] = Ref.unsafe(IO.unit)

  def allocate[A](resource: Resource[IO, A]): A =
    resource.allocated.flatMap { case (a, release) =>
      // Shutdown this resource, and after shutdown all previous
      shutdown.update(release *> _) *> IO.pure(a)
    }.unsafeRunSync()

  def allocate[A](io: IO[A]): A = allocate(io.toResource)

  def shutdownAll: IO[Unit] = {
    shutdown.getAndSet(IO.unit).flatten
  }

}
