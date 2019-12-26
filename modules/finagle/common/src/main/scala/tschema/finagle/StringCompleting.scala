package tschema.finagle
import cats.{Applicative, Functor}
import com.twitter.finagle.http.Status
import tschema.ResponseStatus
import tschema.finagle.util.message
import tschema.swagger.{SwaggerContent, SwaggerTypeable}

class StringCompleting[A](read: A => String, status: Int = 200) {
  implicit val responseStatus: ResponseStatus[A] = ResponseStatus[A](status)

  implicit def fstringComplete[F[_]: Functor, H[_]](implicit lift: LiftHttp[H, F]): Complete[H, A, F[A]] =
    message.fstringComplete[H, F, A](read, Status(status))

  implicit def stringComplete[H[_]: Applicative]: Complete[H, A, A] =
    message.stringComplete[H, A](read, Status(status))

  implicit val swagger: SwaggerTypeable[A] = SwaggerTypeable[String].as[A]
}

class NoneCompleting(status: Int = 200) {
  implicit val responseStatus: ResponseStatus[this.type] = ResponseStatus(status)

  implicit def femptyComplete[H[_]: Applicative, F[_]]: Complete[H, this.type, F[this.type]] =
    message.emptyComplete[H, this.type, F[this.type]](Status(status))

  implicit def emptyComplete[H[_]: Applicative]: Complete[H, this.type, this.type] =
    message.emptyComplete[H, this.type, this.type](Status(status))

  implicit val swagger: SwaggerContent[this.type] = SwaggerContent(List(status -> None))
}
