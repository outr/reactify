package reactify.bind

sealed trait BindSet

/**
  * BindSet defines how a binding should be applied when first defined
  */
object BindSet {
  /**
    * The left value is assigned to the right
    */
  case object LeftToRight extends BindSet

  /**
    * The right value is assigned to the left
    */
  case object RightToLeft extends BindSet

  /**
    * Values are not modified at bind-time
    */
  case object None extends BindSet
}