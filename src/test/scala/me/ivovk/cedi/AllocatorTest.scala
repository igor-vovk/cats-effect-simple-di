package me.ivovk.cedi

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref, Resource}
import me.ivovk.cedi.syntax.*
import org.scalatest.flatspec.AnyFlatSpec

//noinspection TypeAnnotation
class AllocatorTest extends AnyFlatSpec {

  trait ctx {
    object TestDependencies {
      def create(): Resource[IO, TestDependencies] =
        Allocator.create[IO]()
          .map(_.withListener(new LoggingAllocationListener[IO]))
          .map(TestDependencies(using _))

      val shutdownOrderCapturer: Ref[IO, Seq[String]] = Ref.unsafe(Seq.empty)
    }

    class TestDependencies(using AllocatorIO) {

      import TestDependencies.*

      // Allocate resources using the Allocator syntax
      lazy val testResourceA: String = allocate {
        Resource.make(IO("resourceA")) { _ =>
          shutdownOrderCapturer.update(_ :+ "A").void
        }
      }

      // Allocate resources that depend on other resources using direct method
      lazy val testResourceB: String = allocate {
        Resource.make(IO(s"resourceB, but depends on $testResourceA")) { _ =>
          shutdownOrderCapturer.update(_ :+ "B").void
        }
      }

    }
  }

  "Allocator" should "allocate a resource" in new ctx {
    val testDependencies = TestDependencies.create()
    val testResource     = testDependencies.use { deps =>
      IO.pure(deps.testResourceA)
    }.unsafeRunSync()

    assert(testResource == "resourceA")
  }

  it should "allocate resources in the correct order" in new ctx {
    val testDependencies = TestDependencies.create()
    testDependencies.use { deps =>
      IO.pure(deps.testResourceB)
    }.unsafeRunSync()

    val resourceShutdownOrder = TestDependencies.shutdownOrderCapturer.get.unsafeRunSync()
    assert(resourceShutdownOrder == Seq("B", "A"))
  }

}
