package reactify

sealed trait InvocationType

object InvocationType {
  case object Direct extends InvocationType
  case object Derived extends InvocationType
}