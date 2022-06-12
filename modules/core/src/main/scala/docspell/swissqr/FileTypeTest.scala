package docspell.swissqr

import cats.Applicative
import cats.syntax.applicative.*
import docspell.swissqr.FileTypeTest.FileType
import fs2.io.file.Path

import java.nio.file.Files

trait FileTypeTest[F[_]]:
  def getFileType(file: Path): F[Option[FileType]]

object FileTypeTest:
  enum FileType:
    case Pdf, Image, Text

  def none[F[_]: Applicative]: FileTypeTest[F] =
    new FileTypeTest[F]:
      def getFileType(file: Path) = Applicative[F].pure(None)

  def fromExt[F[_]: Applicative]: FileTypeTest[F] =
    new FileTypeTest[F]:
      def getFileType(file: Path) =
        file.extName.toLowerCase match {
          case ".pdf"                    => Some(FileType.Pdf).pure[F]
          case ".jpg" | ".png" | ".jpeg" => Some(FileType.Image).pure[F]
          case ".txt"                    => Some(FileType.Text).pure[F]
          case _                         => None.pure[F]
        }
