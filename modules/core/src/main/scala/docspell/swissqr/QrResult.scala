package docspell.swissqr

import cats.*
import cats.syntax.all.*

final case class QrResult[F[_]: Monad](value: F[Option[List[Either[String, SwissQR]]]]):
  def orElse(other: QrResult[F]): QrResult[F] =
    QrResult(value.flatMap {
      case None      => other.value
      case Some(Nil) => other.value
      case res       => res.pure[F]
    })

object QrResult:
  def none[F[_]: Monad]: QrResult[F] = QrResult[F](None.pure[F])

  def of[F[_]: Monad](res: List[Either[String, SwissQR]]): QrResult[F] =
    QrResult(Some(res).pure[F])
