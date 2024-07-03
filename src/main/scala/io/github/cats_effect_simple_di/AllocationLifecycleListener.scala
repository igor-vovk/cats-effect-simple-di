package io.github.cats_effect_simple_di

import cats.effect.IO

import scala.reflect.ClassTag

trait AllocationLifecycleListener {
  def onInit[A: ClassTag](a: A): IO[Unit]

  def onShutdown[A: ClassTag](a: A): IO[Unit]
}

object NoOpListener extends AllocationLifecycleListener {
  override def onInit[A: ClassTag](a: A): IO[Unit] = IO.unit

  override def onShutdown[A: ClassTag](a: A): IO[Unit] = IO.unit
}

object LogbackAllocationListener extends AllocationLifecycleListener {

  import org.typelevel.log4cats.Logger
  import org.typelevel.log4cats.slf4j.Slf4jLogger

  private val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def onInit[A: ClassTag](a: A): IO[Unit] =
    logger.info(s"Allocated $a")

  override def onShutdown[A: ClassTag](a: A): IO[Unit] =
    logger.info(s"Shutting down $a")
}