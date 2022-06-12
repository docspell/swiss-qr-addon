package docspell.swissqr.addon

import cats.effect._
import munit._

class ConfigParseTest extends CatsEffectSuite:

  test("parse with missing pieces") {
    val yamlString =
      """field-currency:
        |  CHF: chf
        |  EUR: eur
        |
        |add-tags:
        |  - Invoice
        |  - QR-Code""".stripMargin

    for {
      cfg <- Config.fromYaml[IO](yamlString)
      _ = assertEquals(
        cfg,
        Config(
          Some(Map("CHF" -> "chf", "EUR" -> "eur")),
          Some(List("Invoice", "QR-Code")),
          None
        )
      )
    } yield ()
  }
