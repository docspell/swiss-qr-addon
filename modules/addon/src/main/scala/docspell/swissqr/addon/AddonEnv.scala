package docspell.swissqr.addon

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.{Files, Path}

import java.nio.file.{Files => NioFiles}

case class AddonEnv(
    itemDataJson: Path,
    itemPdfJson: Path,
    tempDir: Path,
    itemPdfDir: Path
):

  def itemMeta[F[_]: Sync]: F[ItemMetadata] =
    FileUtil[F].readJson[ItemMetadata](itemDataJson)

  def fileProperties[F[_]: Files: Sync]: F[List[FileProperties]] =
    FileUtil[F].readJson[List[FileProperties]](itemPdfJson)

  def attachmentFiles[F[_]: Files: Sync]: F[List[Path]] =
    fileProperties[F].map(as => as.map(a => itemPdfDir / a.id))

object AddonEnv:
  def fromEnv[F[_]: Sync]: F[AddonEnv] =
    Sync[F].delay {
      AddonEnv(
        Path(sys.env("ITEM_DATA_JSON")),
        Path(sys.env("ITEM_PDF_JSON")),
        Path(sys.env("TMP_DIR")),
        Path(sys.env("ITEM_PDF_DIR"))
      )
    }
