package docspell.swissqr.addon

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import io.circe.{Decoder, Encoder}
import io.circe.syntax.*

case class Config(
    fieldCurrency: Option[Map[String, String]],
    addTags: Option[List[String]],
    checkTags: Option[List[String]],
    additionalAsName: Option[Boolean]
):
  def getFieldName(currencyCode: String): Option[String] =
    fieldCurrency.flatMap(m => m.get(currencyCode).orElse(m.get("*")))

  def checkTagsMatch(givenTags: List[String]): Boolean =
    checkTags.toList.flatten.toSet.intersect(givenTags.toSet).nonEmpty

object Config:
  def fromYamlFile[F[_]: Sync](file: Path): F[Config] =
    FileUtil[F]
      .readString(file)
      .flatMap(str =>
        if (str.trim.isEmpty) default.pure[F]
        else fromYaml(str)
      )

  def fromYaml[F[_]: Sync](str: String): F[Config] =
    Sync[F]
      .delay(io.circe.yaml.parser.parse(str))
      .rethrow
      .map(_.as[Config])
      .rethrow

  val default: Config =
    Config(None, None, None, None)

  given jsonDecoder: Decoder[Config] =
    Decoder.forProduct4("field-currency", "add-tags", "check-tags", "additional-as-name")(
      Config.apply
    )

  given jsonEncoder: Encoder[Config] =
    Encoder.forProduct4("field-currency", "add-tags", "check-tags", "additional-as-name")(
      cfg => (cfg.fieldCurrency, cfg.addTags, cfg.checkTags, cfg.additionalAsName)
    )
