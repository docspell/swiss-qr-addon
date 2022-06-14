package docspell.swissqr.addon

import docspell.swissqr.addon.Output.*
import io.circe.Encoder
import io.circe.syntax.*
import io.circe.generic.semiauto.deriveEncoder

case class Output(commands: List[Command])

object Output:
  val empty: Output = Output(Nil)

  def apply(itemId: String, actions: List[Action]): Output =
    Output(List(Command(itemId, actions)))

  trait Action

  case class SetField(field: String, value: String, action: String = "set-field")
      extends Action
  object SetField:
    given jsonEncoder: Encoder[SetField] = deriveEncoder

  case class AddNotes(notes: String, separator: String, action: String = "add-notes")
      extends Action
  object AddNotes:
    given jsonEncoder: Encoder[AddNotes] = deriveEncoder

  case class SetName(name: String, action: String = "set-name") extends Action
  object SetName:
    given jsonEncoder: Encoder[SetName] = deriveEncoder

  case class AddTags(tags: List[String], action: String = "add-tags") extends Action
  object AddTags:
    given jsonEncoder: Encoder[AddTags] = deriveEncoder

  object Action:
    given jsonEncoder: Encoder[Action] =
      Encoder.encodeJson.contramap {
        case c: SetField => c.asJson
        case c: AddNotes => c.asJson
        case c: AddTags  => c.asJson
        case c: SetName  => c.asJson
      }

  case class Command(
      itemId: String,
      actions: List[Action],
      command: String = "item-update"
  )
  object Command:
    given jsonEncoder: Encoder[Command] = deriveEncoder

  given jsonEncoder: Encoder[Output] = deriveEncoder
