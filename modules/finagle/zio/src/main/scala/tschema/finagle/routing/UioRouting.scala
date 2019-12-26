package tschema.finagle.routing

import cats.Monad
import cats.syntax.semigroup._
import com.twitter
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Service, http}
import com.twitter.util.{Future, Promise}
import tschema.finagle.routing.UioRouting.UIOHttp
import tschema.finagle.{ConvertService, LiftHttp, Rejection, Routed, RoutedPlus, RunHttp}
import tschema.utils.SubString
import zio.{Exit, Fiber, UIO, ZIO}

final case class UioRouting(
    request: http.Request,
    path: CharSequence,
    matched: Int
) extends ZioRoutingCommon

object UioRouting extends UioRoutedImpl {

  type UIOHttp[+A] = ZIO[UioRouting, Rejection, A]

  implicit val uioRouted: RoutedPlus[UIOHttp] with LiftHttp[UIOHttp, UIO] = new UioRoutedInstance

  def uioConvertService(f: Throwable => Rejection): ConvertService[UIOHttp] =
    new ZIOConvertService[UioRouting, Rejection](f)

  implicit def uioRunnable(
      implicit
      rejectionHandler: Rejection.Handler = Rejection.defaultHandler
  ): RunHttp[UIOHttp, UIO] =
    zioResponse => ZIO.runtime[Any].flatMap(runtime => ZIO.effectTotal(execResponse(runtime, zioResponse, _)))

  private[this] def execResponse(runtime: zio.Runtime[Any], zioResponse: UIOHttp[Response], request: Request)(
      implicit handler: Rejection.Handler
  ): Future[Response] =
    execWithRuntime(runtime, request)(
      zioResponse
        .provide(UioRouting(request, SubString(request.path), 0))
        .catchAll(rejection => ZIO.succeed(handler(rejection)))
    )
}
private[finagle] class UioRoutedImpl {

  protected class UioRoutedInstance extends RoutedPlus[UIOHttp] with LiftHttp[UIOHttp, UIO] {
    private type F[a] = UIOHttp[a]
    implicit private[this] val self: RoutedPlus[F] = this
    implicit private[this] val monad: Monad[F]     = zio.interop.catz.monadErrorInstance

    def matched: F[Int] = ZIO.access(_.matched)

    def withMatched[A](m: Int, fa: F[A]): F[A] = fa.provideSome(_.copy(matched = m))

    def path: F[CharSequence]    = ZIO.access(_.path)
    def request: F[http.Request] = ZIO.access(_.request)
    def reject[A](rejection: Rejection): F[A] =
      Routed.unmatchedPath[F].flatMap(path => throwRej(rejection withPath path.toString))

    def combineK[A](x: F[A], y: F[A]): F[A] =
      catchRej(x)(xrs => catchRej(y)(yrs => throwRej(xrs |+| yrs)))

    def apply[A](fa: UIO[A]): F[A] = fa

    @inline private[this] def catchRej[A](z: F[A])(f: Rejection => F[A]): F[A] = z.catchAll(rej => f(rej))

    @inline private[this] def throwRej[A](map: Rejection): F[A] = ZIO.fail(map)
  }

}
