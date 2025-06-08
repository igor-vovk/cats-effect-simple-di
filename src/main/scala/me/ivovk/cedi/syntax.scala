package me.ivovk.cedi

import cats.effect.{IO, Resource}

import scala.reflect.ClassTag

object syntax {

  type Allocator[F[_]] = me.ivovk.cedi.Allocator[F]
  val Allocator: me.ivovk.cedi.Allocator.type = me.ivovk.cedi.Allocator
  type AllocatorIO = me.ivovk.cedi.Allocator[IO]

  def allocate[F[_]: Allocator, A: ClassTag](fa: F[A]): A =
    Allocator[F].allocate(fa)

  def allocate[F[_]: Allocator, A: ClassTag](resource: Resource[F, A]): A =
    Allocator[F].allocate(resource)

}
