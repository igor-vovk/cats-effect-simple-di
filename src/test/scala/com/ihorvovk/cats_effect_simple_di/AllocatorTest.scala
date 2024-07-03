package com.ihorvovk.cats_effect_simple_di

import cats.effect.unsafe.IORuntime
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref, Resource}
import org.scalatest.flatspec.AnyFlatSpec

//noinspection TypeAnnotation
class AllocatorTest extends AnyFlatSpec {

  trait ctx {
    object TestDependencies {
      def apply(runtime: IORuntime): Resource[IO, TestDependencies] =
        Allocator(runtime).map(new TestDependencies(_))

      val shutdownOrderCapturer: Ref[IO, Seq[String]] = Ref.unsafe(Seq.empty)
    }

    class TestDependencies(allocator: Allocator) {

      import TestDependencies.*

      lazy val testResourceA: String = allocator.allocate {
        Resource.make(IO("resourceA")) { _ =>
          shutdownOrderCapturer.update(_ :+ "A").void
        }
      }

      lazy val testResourceB: String = allocator.allocate {
        Resource.make(IO(s"resourceB, but depends on $testResourceA")) { _ =>
          shutdownOrderCapturer.update(_ :+ "B").void
        }
      }

    }
  }

  "Allocator" should "allocate a resource" in new ctx {
    val testDependencies = TestDependencies(global)
    val testResource     = testDependencies.use(deps => IO.pure(deps.testResourceA)).unsafeRunSync()
    assert(testResource == "resourceA")
  }

  it should "allocate a resource with dependencies" in new ctx {
    val testDependencies = TestDependencies(global)
    testDependencies.use { deps =>
      IO.pure(deps.testResourceB)
    }.unsafeRunSync()

    val resourceShutdownOrder = TestDependencies.shutdownOrderCapturer.get.unsafeRunSync()
    assert(resourceShutdownOrder == Seq("B", "A"))
  }

}
