package reactify.reaction

trait ReactionStatus

/**
  * ReactionStatus is utilized during the propagation of a fired value against a Reactive. This value may be returned by
  * a Reaction or set via `status` on the Reactive (on the same thread while the fired value is propagating)
  */
object ReactionStatus {
  /**
    * Continue propagation
    */
  case object Continue extends ReactionStatus

  /**
    * Stop propagation
    */
  case object Stop extends ReactionStatus
}