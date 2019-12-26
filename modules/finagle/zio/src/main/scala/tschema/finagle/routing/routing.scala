package tschema.finagle.routing
import com.twitter
import com.twitter.finagle.{Service, http}
import com.twitter.finagle.http.Request
import com.twitter.util.Promise
import tschema.finagle.ConvertService
import tschema.finagle.routing.IoRouting.IOHttp
import zio.{Fiber, Runtime, UIO, ZIO}

trait ZioRoutingCommon {
  def request: Request
  def path: CharSequence
  def matched: Int
}

private class ZIOConvertService[R <: ZioRoutingCommon, E](f: Throwable => E) extends ConvertService[ZIO[R, E, *]] {
  def convertService[A](svc: Service[Request, A]): ZIO[R, E, A] =
    ZIO.accessM { r =>
      ZIO.effectAsyncInterrupt[R, E, A] { cb =>
        val fut = svc(r.request).respond {
          case twitter.util.Return(a) => cb(ZIO.succeed(a))
          case twitter.util.Throw(ex) => cb(ZIO.fail(f(ex)))
        }

        Left(UIO(fut.raise(new InterruptedException)))
      }
    }
}
