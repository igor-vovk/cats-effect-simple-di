package me.ivovk.cedi

import cats.Applicative
import cats.effect.Sync

import scala.reflect.ClassTag

trait AllocationLifecycleListener[F[_]] {
  def onInit[A: ClassTag](a: A): F[Unit]

  def onShutdown[A: ClassTag](a: A): F[Unit]
}

class NoOpListener[F[_]: Applicative] extends AllocationLifecycleListener[F] {
  override def onInit[A: ClassTag](a: A): F[Unit] = Applicative[F].unit

  override def onShutdown[A: ClassTag](a: A): F[Unit] = Applicative[F].unit
}

class LoggingAllocationListener[F[_]: Sync] extends AllocationLifecycleListener[F] {

  import org.typelevel.log4cats.Logger
  import org.typelevel.log4cats.slf4j.Slf4jLogger

  private val logger: Logger[F] = Slf4jLogger.getLogger[F]

  override def onInit[A: ClassTag](a: A): F[Unit] =
    logger.info(s"Allocated $a")

  override def onShutdown[A: ClassTag](a: A): F[Unit] =
    logger.info(s"Shutting down $a")
}
