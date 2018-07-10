package reactify.bind

sealed trait BindSet

object BindSet {
  case object LeftToRight extends BindSet
  case object RightToLeft extends BindSet
  case object None extends BindSet
}