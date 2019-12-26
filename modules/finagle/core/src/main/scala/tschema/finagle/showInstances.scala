package tschema.finagle
import cats.syntax.show._
import cats.{Applicative, Functor, Show}
import tschema.finagle.util.message

object showInstances {
  implicit def showCompleteInstance[F[_]: Applicative, G[_], A: Show]: Complete[F, A, A] = message.stringComplete(_.show)
  implicit def showCompleteFInstance[F[_], G[_]: Functor, A: Show](implicit lift: LiftHttp[F, G]) =
    message.fstringComplete[F, G, A](_.show)
}
