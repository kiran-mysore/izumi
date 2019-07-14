package com.github.pshirshov.izumi.functional.bio

import cats.Eval
import cats.effect.ExitCase
import com.github.pshirshov.izumi.functional.bio.BIOCatsConversions._

trait BIOCatsConversions extends BIOCatsConversions1 {
  @inline implicit final def BIOToSync[F[+_, +_]](implicit F: BIO[F]): BIOCatsSync[F] = new BIOCatsSync[F](F)
}

trait BIOCatsConversions1 extends BIOCatsConversions2 {
  @inline implicit final def BIOToBracket[F[+_, +_]](implicit F: BIOPanic[F]): BIOCatsBracket[F] = new BIOCatsBracket[F](F)
}

trait BIOCatsConversions2 extends BIOCatsConversions3 {
  @inline implicit final def BIOToMonadError[F[+_, +_], E](implicit F: BIOBracket[F]): BIOCatsMonadError[F, E] = new BIOCatsMonadError[F, E](F)
}

trait BIOCatsConversions3 extends BIOCatsConversions4 {
  @inline implicit final def BIOToMonad[F[+_, +_], E](implicit F: BIOMonad[F]): BIOCatsMonad[F, E] = new BIOCatsMonad[F, E](F)
}
trait BIOCatsConversions4 extends BIOCatsConversions5 {
  @inline implicit final def BIOToApplicativeError[F[+_, +_], E](implicit F0: BIOError[F]): BIOCatsApplicativeError[F, E] = new BIOCatsApplicativeError[F, E] {
    override val F: BIOError[F] = F0
  }
}
trait BIOCatsConversions5 extends BIOCatsConversions6 {
  @inline implicit final def BIOToApplicative[F[+_, +_], E](implicit F0: BIOApplicative[F]): BIOCatsApplicative[F, E] = new BIOCatsApplicative[F, E] {
    override val F: BIOApplicative[F] = F0
  }
}
trait BIOCatsConversions6 extends BIOCatsConversions7 {
  @inline implicit final def BIOToBifunctor[F[+_, +_]](implicit F0: BIOBifunctor[F]): BIOCatsBifunctor[F] = new BIOCatsBifunctor[F] {
    override val F: BIOBifunctor[F] = F0
  }
}
trait BIOCatsConversions7 {
  @inline implicit final def BIOToFunctor[F[_, +_], E](implicit F0: BIOFunctor[F]): BIOCatsFunctor[F, E] = new BIOCatsFunctor[F, E] {
    override val F: BIOFunctor[F] = F0
  }
}

object BIOCatsConversions {

  trait BIOCatsFunctor[F[_, +_], E] extends cats.Functor[F[E, ?]] {
    def F: BIOFunctor[F]

    @inline override final def map[A, B](fa: F[E, A])(f: A => B): F[E, B] = F.map(fa)(f)
    @inline override final def void[A](fa: F[E, A]): F[E, Unit] = F.void(fa)
    @inline override final def widen[A, B >: A](fa: F[E, A]): F[E, B] = fa
  }

  trait BIOCatsBifunctor[F[+_, +_]] extends cats.Bifunctor[F] {
    def F: BIOBifunctor[F]

    @inline override final def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D] = F.bimap(fab)(f, g)
    @inline override final def leftMap[A, B, C](fab: F[A, B])(f: A => C): F[C, B] = F.leftMap(fab)(f)
  }

  trait BIOCatsApplicative[F[+_, +_], E] extends cats.Applicative[F[E, ?]] with BIOCatsFunctor[F, E] with BIOCatsBifunctor[F] {
    override def F: BIOApplicative[F]

    @inline override final def ap[A, B](ff: F[E, A => B])(fa: F[E, A]): F[E, B] = F.map2(ff, fa)(_.apply(_))
    @inline override final def map2[A, B, Z](fa: F[E, A], fb: F[E, B])(f: (A, B) => Z): F[E, Z] = F.map2(fa, fb)(f)
    @inline override final def map2Eval[A, B, Z](fa: F[E, A], fb: Eval[F[E, B]])(f: (A, B) => Z): Eval[F[E, Z]] = Eval.later(F.map2(fa, fb.value)(f))

    @inline override final def pure[A](x: A): F[E, A] = F.pure(x)
    @inline override final def point[A](x: A): F[E, A] = F.pure(x)

    @inline override final def productR[A, B](fa: F[E, A])(fb: F[E, B]): F[E, B] = F.*>(fa, fb)
    @inline override final def productL[A, B](fa: F[E, A])(fb: F[E, B]): F[E, A] = F.<*(fa, fb)
  }

  trait BIOCatsApplicativeError[F[+_, +_], E] extends BIOCatsApplicative[F, E] with cats.ApplicativeError[F[E, ?], E] {
    override def F: BIOError[F]

    @inline override final def raiseError[A](e: E): F[E, A] = F.fail(e)
    @inline override final def handleErrorWith[A](fa: F[E, A])(f: E => F[E, A]): F[E, A] = F.catchAll(fa)(f)
    @inline override final def recoverWith[A](fa: F[E, A])(pf: PartialFunction[E, F[E, A]]): F[E, A] = F.catchSome(fa)(pf)

    @inline override final def attempt[A](fa: F[E, A]): F[E, Either[E, A]] = F.attempt(fa)
    @inline override final def fromEither[A](x: Either[E, A]): F[E, A] = F.fromEither(x)
  }

  class BIOCatsMonad[F[+_, +_], E](override val F: BIOMonad[F]) extends cats.Monad[F[E, ?]] with BIOCatsApplicative[F, E] {
    @inline override final def flatMap[A, B](fa: F[E, A])(f: A => F[E, B]): F[E, B] = F.flatMap(fa)(f)
    @inline override final def flatten[A](ffa: F[E, F[E, A]]): F[E, A] = F.flatten(ffa)
    @inline override final def tailRecM[A, B](a: A)(f: A => F[E, Either[A, B]]): F[E, B] = {
      F.flatMap(f(a)) {
        case Left(next) => tailRecM(next)(f)
        case Right(res) => F.pure(res)
      }
    }
  }

  class BIOCatsMonadError[F[+_, +_], E](override val F: BIOMonad[F] with BIOError[F]) extends BIOCatsMonad[F, E](F) with BIOCatsApplicativeError[F, E] with cats.MonadError[F[E, ?], E]

  class BIOCatsBracket[F[+_, +_]](override val F: BIOPanic[F]) extends BIOCatsMonadError[F, Throwable](F) with cats.effect.Bracket[F[Throwable, ?], Throwable] {

    @inline override final def bracketCase[A, B](acquire: F[Throwable, A])(use: A => F[Throwable, B])(release: (A, ExitCase[Throwable]) => F[Throwable, Unit]): F[Throwable, B] = {
      F.bracketCase[Throwable, A, B](acquire)(
        (a, e) => F.orTerminate {
          release(a, e match {
            case BIOExit.Success(_) => ExitCase.Completed
            case value: BIOExit.Failure[Throwable] => ExitCase.Error(value.toThrowable)
          })
        })(use)
    }

    @inline override final def bracket[A, B](acquire: F[Throwable, A])(use: A => F[Throwable, B])(release: A => F[Throwable, Unit]): F[Throwable, B] = {
      F.bracket(acquire)(e => F.orTerminate(release(e)))(use)
    }
  }

  class BIOCatsSync[F[+_, +_]](override val F: BIO[F]) extends BIOCatsBracket[F](F) with cats.effect.Sync[F[Throwable, ?]] {
    @inline override final def suspend[A](thunk: => F[Throwable, A]): F[Throwable, A] = F.flatten(F.syncThrowable(thunk))
    @inline override final def delay[A](thunk: => A): F[Throwable, A] = F.syncThrowable(thunk)
  }

}