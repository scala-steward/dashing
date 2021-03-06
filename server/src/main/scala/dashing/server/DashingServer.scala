package dashing.server

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import cats.Parallel
import cats.effect._
import cats.implicits._
import com.typesafe.config.ConfigFactory
import fs2.Stream
import io.chrisdavenport.mules._
import io.circe.generic.auto._
import io.circe.config.syntax._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.syntax.kleisli._

import model.{DashingConfig, PageInfo}

object DashingServer extends IOApp {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val ymc: YearMonthClock[IO] = YearMonthClock.create[IO]

  override def run(args: List[String]): IO[ExitCode] = ServerStream.stream[IO].compile.lastOrError
}

object ServerStream {

  /** Entry point to the server */
  def stream[F[_]: ConcurrentEffect: ContextShift: Timer: Parallel](
    implicit ec: ExecutionContext, C: YearMonthClock[F]
  ): Stream[F, ExitCode] =
    ConfigFactory.load().as[DashingConfig] match {
      case Right(c) =>
        for {
          cache <- Stream.eval(
            MemoryCache.createMemoryCache[F, String, PageInfo](TimeSpec.fromDuration(c.cacheDuration)))
          client <- BlazeClientBuilder[F](ec).stream
          graphQL = new GraphQL(client, c.ghToken)
          apiService =
            new StarsRoutes[F].routes(cache, graphQL, c.starDashboards) <+>
            new PullRequestsRoutes[F].routes(cache, graphQL, c.prDashboards)
          httpApp = Router(
            "/" -> new RenderingRoutes[F](ec).routes,
            "/api" -> apiService
          ).orNotFound
          server <- BlazeServerBuilder[F]
            .bindHttp(c.port, c.host)
            .withHttpApp(httpApp)
            .withResponseHeaderTimeout(10.minute)
            .withIdleTimeout(10.minute)
            .serve
        } yield server
      case Left(e) => Stream.eval(ConcurrentEffect[F].delay {
        System.err.println(e.getMessage)
        ExitCode.Error
      })
    }
}
