package izumi.fundamentals.bio.test

import izumi.functional.bio.{BIO, BIOFunctor, BIOMonad, BIOMonadError, BIOPrimitives, BIOTemporal, F}
import izumi.fundamentals.bio.test.masking._
import org.scalatest.WordSpec

import scala.concurrent.duration._

object masking {
  import izumi.functional.bio.{BIOFork, BIOFork3, BIOPrimitives3}

  type Primitives[F[+_, +_]] = BIOPrimitives[F]
  type Fork[F[+_, +_]] = BIOFork[F]
  type Fork3[F[-_, +_, +_]] = BIOFork3[F]
  type BIOMonad3[F[-_, +_, +_]] = BIOMonad[F[Any, +?, +?]]
  type Primitives3[F[-_, +_, +_]] = BIOPrimitives3[F]
}

class BIOSyntaxTest extends WordSpec {

  "BIO.apply is callable" in {
    class X[F[+_, +_]: BIO] {
      def hello = BIO(println("hello world!"))
    }

    assert(new X[zio.IO].hello != null)
  }

  "BIO.widen/widenError is callable" in {
    def x[F[+_, +_]: BIO]: F[Throwable, AnyVal] = {
      identity[F[Throwable, AnyVal]] {
        BIO[F].pure(None: Option[Int]).flatMap {
          _.fold(
            BIO[F].unit.widenError[Throwable].widen[AnyVal]
          )(
            _ => BIO[F].fail(new RuntimeException)
          )
        }
      }
    }

    x[zio.IO]
  }

  "BIO.when/unless/ifThenElse have nice inference" in {
    def x[F[+_, +_]: BIOMonad] = {
      F.ifThenElse(F.pure(false): F[RuntimeException, Boolean])(F.pure(()), F.pure(()): F[Throwable, Any]) *>
      F.when(F.pure(false): F[RuntimeException, Boolean])(F.pure(()): F[Throwable, Unit])
    }

    x[zio.IO]
  }

  "withFilter test" in {
    def x[F[+_, +_]: BIOMonadError]: F[NoSuchElementException, Unit] = {
      assertDoesNotCompile(
        """
        for {
          (1, 2) <- F.pure(Option((2, 1)))
        } yield ()
        """
      )
      for {
        (1, 2) <- F.pure((2, 1))
      } yield ()
    }
    x[zio.IO]
  }

  "F summoner examples" in {
    def x[F[+_, +_]: BIOMonad] = {
      F.when(false)(F.unit)
    }
    def y[F[+_, +_]: BIOTemporal] = {
      F.timeout(F.forever(F.unit))(5.seconds)
    }
    def z[F[+_, +_]: BIOFunctor]: F[Nothing, Unit] = {
      F.map(z[F])(_ => ())
    }
    def `attach BIOPrimitives & BIOFork methods even when they aren't imported`[F[+_, +_]: BIOMonad: Primitives: Fork]: F[Nothing, Int] = {
      F.fork[Any, Nothing, Int] {
        F.mkRef(4).flatMap(r => r.update(_ + 5) *> r.get.map(_ - 1))
      }.flatMap(_.join)
    }
    def `attach BIOPrimitives & BIOFork3 methods to a trifunctor BIO even when not imported`[F[-_, +_, +_]: BIOMonad3: Primitives3: Fork3]: F[Any, Nothing, Int] = {
      F.fork(F.mkRef(4).flatMap(r => r.update(_ + 5) *> r.get.map(_ - 1))).flatMap(_.join)
    }
    lazy val _ = (
      x,
      y[zio.IO](_: BIOTemporal[zio.IO]),
      z,
      `attach BIOPrimitives & BIOFork methods even when they aren't imported`[zio.IO],
      `attach BIOPrimitives & BIOFork3 methods to a trifunctor BIO even when not imported`[zio.ZIO],
    )
  }
}
