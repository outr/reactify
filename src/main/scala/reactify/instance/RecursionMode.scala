package reactify.instance

sealed trait RecursionMode

object RecursionMode {
  case object Static extends RecursionMode
  case object None extends RecursionMode
  case object RetainPreviousValue extends RecursionMode
  case object Full extends RecursionMode
}