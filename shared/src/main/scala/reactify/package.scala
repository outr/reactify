import scala.language.implicitConversions

package object reactify {
  implicit def val2Value[T](v: Val[T]): T = v()
}