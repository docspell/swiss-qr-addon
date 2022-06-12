package docspell.swissqr.addon

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class ItemMetadata(
    id: String,
    name: String,
    tags: List[String],
    assumedTags: List[String]
)

object ItemMetadata:
  given jsonDecoder: Decoder[ItemMetadata] = deriveDecoder
