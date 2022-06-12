package docspell.swissqr.addon

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class FileProperties(id: String, name: String, mimetype: String)

object FileProperties:
  given jsonDecoder: Decoder[FileProperties] = deriveDecoder
