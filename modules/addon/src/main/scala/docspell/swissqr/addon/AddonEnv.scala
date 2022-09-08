package docspell.swissqr.addon

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.{Files, Path}

import java.nio.file.{Files => NioFiles}

case class AddonEnv(
    itemDataJson: Path,
    itemPdfJson: Path,
    itemOriginalJson: Path,
    tempDir: Path,
    itemPdfDir: Path,
    itemOriginalDir: Path
):

  def itemMeta[F[_]: Sync]: F[ItemMetadata] =
    FileUtil[F].readJson[ItemMetadata](itemDataJson)

  def pdfProperties[F[_]: Files: Sync]: F[List[FileProperties]] =
    FileUtil[F].readJson[List[FileProperties]](itemPdfJson)

  def originalProperites[F[_]: Files: Sync]: F[List[FileProperties]] =
    FileUtil[F].readJson[List[FileProperties]](itemOriginalJson)

  def attachmentPdfs[F[_]: Files: Sync]: F[List[Path]] =
    pdfProperties[F].map(as => as.map(a => itemPdfDir / a.id))

  def attachmentOriginals[F[_]: Files: Sync]: F[List[Path]] =
    originalProperites[F].map(fp => fp.map(f => itemOriginalDir / f.id))

object AddonEnv:
  def fromEnv[F[_]: Sync]: F[AddonEnv] =
    Sync[F].delay {
      AddonEnv(
        Path(sys.env("ITEM_DATA_JSON")),
        Path(sys.env("ITEM_PDF_JSON")),
        Path(sys.env("ITEM_ORIGINAL_JSON")),
        Path(sys.env("TMP_DIR")),
        Path(sys.env("ITEM_PDF_DIR")),
        Path(sys.env("ITEM_ORIGINAL_DIR"))
      )
    }
