package docspell.swissqr

import cats.{Functor, Semigroupal}
import cats.syntax.all.*

import scala.annotation.tailrec
import scala.util.Try

private[swissqr] trait LineReader:
  type Result[A] = Either[String, (A, String)]

  case class P[A](f: String => Result[A]):
    def apply(str: String) = f(str)
    def emap[B](g: A => Either[String, B]): P[B] =
      P(str => f(str).flatMap((a, rem) => g(a).map(b => (b, rem))))

    def ~[B, C](next: P[B])(merge: (B, A) => C): P[C] =
      P(str =>
        f(str) match {
          case Right((a, rem)) => next.map(b => merge(b, a)).f(rem)
          case Left(errMsg)    => Left(errMsg)
        }
      )

    def withError(msg: String => String): P[A] =
      P(str => f(str).left.map(msg))

  object P:
    def pure[A](a: A): P[A] = P(str => Right(a, str))
    def failed[A](error: String): P[A] = P(_ => Left(error))

    given functorP: Functor[P] =
      new Functor[P] {
        def map[A, B](fa: P[A])(f: A => B) =
          P(str => fa(str).map((a, rem) => (f(a), rem)))
      }

    given semigroupalP: Semigroupal[P] =
      new Semigroupal[P] {
        def product[A, B](fa: P[A], fb: P[B]) =
          (fa ~ fb)((b, a) => a -> b)
      }

    extension (p: P[String])
      def nonEmpty: P[Option[String]] =
        p.emap(str => if (str.trim.isEmpty) Right(None) else Right(Some(str)))

      def fold[A, B](empty: P[B], nonEmpty: P[A]): P[Either[B, A]] =
        P(str =>
          p(str) match {
            case Right((s, _)) =>
              if (s.trim.isEmpty)
                empty(str).map((b, rem) => (Left(b), rem))
              else
                nonEmpty(str).map((a, rem) => (Right(a), rem))
            case Left(err) => Left(err)
          }
        )

  val line: P[String] =
    P(str =>
      str.indexOf('\n') match {
        case n if n >= 0 =>
          Right((str.substring(0, n).trim, str.substring(n + 1)))
        case _ if str.nonEmpty =>
          Right(str.trim -> "")
        case _ =>
          Left(s"Input exhausted")
      }
    )

  def lines(n: Int): P[List[String]] =
    val ps = List.fill(n)(line)
    ps.foldLeft(P.pure(List.empty[String]))((res, p) => (res ~ p)(_ :: _))

  def skip(lines: Int): P[Unit] =
    @tailrec
    def go(left: Int, index: Int, input: String): String =
      if (left == 0) input.substring(index)
      else
        input.indexOf('\n', index) match {
          case idx if idx >= 0 => go(left - 1, idx + 1, input)
          case _               => ""
        }

    if (lines <= 0) P.pure(())
    else P(str => Right(() -> go(lines, 0, str)))

  val restAsLines: P[List[String]] =
    P(str => Right(scala.io.Source.fromString(str).getLines().toList, ""))

  val bigDecimal: P[BigDecimal] =
    line.emap(s => Try(BigDecimal(s)).toEither.leftMap(_.getMessage))

  val int: P[Int] =
    line.emap(s => s.toIntOption.toRight(s"Invalid int: $s"))
