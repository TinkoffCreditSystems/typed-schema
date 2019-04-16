package ru.tinkoff.tschema
package finagle

import Routed.implicits._
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.functor._
import com.twitter.finagle.http.Request
import ru.tinkoff.tschema.finagle.Rejection.{MalformedParam, MissingParam}
import ru.tinkoff.tschema.param._

trait ParamDirectives[S <: ParamSource] {
  def source: S
  def getByName[F[_]: Routed](name: String): F[Option[String]]

  def notFound(name: String): Rejection                 = Rejection.missingParam(name, source)
  def malformed(name: String, error: String): Rejection = Rejection.malformedParam(name, error, source)

  def singleRejection(name: String, error: SingleParamError): Rejection =
    error match {
      case MissingParamError    => notFound(name)
      case ParseParamError(err) => malformed(name, err)
    }

  def errorReject[F[_], A](name: String, error: ParamError)(implicit routed: Routed[F]): F[A] =
    error match {
      case single: SingleParamError => routed.reject(singleRejection(name, single))
      case MultiParamError(vals) =>
        routed.rejectMany(vals.map { case (field, err) => singleRejection(field, err) }.toSeq: _*)
    }

  def provideOrReject[F[_]: Routed, A](name: String, result: Param.Result[A]): F[A] =
    result.fold(errorReject[F, A](name, _), _.pure[F])
}

abstract class ParamDirectivesSimple[S <: ParamSource](val source: S) extends ParamDirectives[S] {
  def getFromRequest(name: String)(req: Request): Option[String]

  def getByName[F[_]](name: String)(implicit F: Routed[F]): F[Option[String]] = {
    F.FMonad.map(Routed.request[F])(getFromRequest(name))
  }
}

object ParamDirectives {
  def apply[S <: ParamSource](implicit dir: ParamDirectives[S]): ParamDirectives[S] = dir

  type TC[A <: ParamSource]  = ParamDirectives[A]
  type TCS[A <: ParamSource] = ParamDirectivesSimple[A]
  import ParamSource._

  implicit val queryParamDirective: TC[Query] = new TCS[Query](Query) {
    def getFromRequest(name: String)(req: Request): Option[String] = req.params.get(name)
  }

  implicit val cookieParamDirectives: TC[Cookie] = new TCS[Cookie](Cookie) {
    def getFromRequest(name: String)(req: Request): Option[String] = req.cookies.get(name).map(_.value)
  }

  implicit val pathParamDirectives: TC[ParamSource.Path] = new TC[ParamSource.Path] {
    def getByName[F[_]: Routed](name: String): F[Option[String]] = Routed.segment.map(_.map(_.toString))
    def source                                                   = ParamSource.Path
  }

  implicit val formDataParamDirectives: TC[Form] = new TCS[Form](Form) {
    def getFromRequest(name: String)(req: Request): Option[String] = req.params.get(name)
  }

  implicit val headerParamDirectives: TC[Header] = new TCS[Header](Header) {
    def getFromRequest(name: String)(req: Request): Option[String] = req.headerMap.get(name)
  }

}