package docspell.swissqr.addon

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.Path
import io.circe.Decoder
import io.circe.{parser => JsonParser}

import java.io.FileNotFoundException
import java.nio.file.Files as NioFiles

trait FileUtil[F[_]]:
  def readString(path: Path): F[String]
  def readJson[A: Decoder](path: Path): F[A]

object FileUtil:
  def apply[F[_]: Sync]: FileUtil[F] =
    new FileUtil[F]:
      def readString(path: Path) =
        Sync[F].blocking {
          if (NioFiles.exists(path.toNioPath))
            NioFiles.readString(path.toNioPath)
          else
            throw new FileNotFoundException(s"File not found: $path")
        }

      def readJson[A: Decoder](path: Path) =
        readString(path).map(str => JsonParser.decode[A](str)).rethrow
