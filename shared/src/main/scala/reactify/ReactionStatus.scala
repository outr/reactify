package reactify

trait ReactionStatus

object ReactionStatus {
  case object Continue extends ReactionStatus
  case object Stop extends ReactionStatus
}