package dashing.server

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import cats.effect.{ContextShift, IO}
import io.chrisdavenport.mules._
import org.http4s._
import org.http4s.client.JavaNetClientBuilder
import org.http4s.dsl.io._
import org.http4s.syntax.kleisli._
import org.http4s.testing.IOMatchers
import org.specs2.mutable.Specification

import model.{PageInfo, PRDashboardsConfig}

class PullRequestsRoutesSpec extends Specification with IOMatchers {
  args(skipAll = sys.env.get("GITHUB_ACCESS_TOKEN").isEmpty)

  implicit val timer = IO.timer(global)
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val c: YearMonthClock[IO] = YearMonthClock.create[IO]

  def serve(req: Request[IO]): Response[IO] = (for {
    cache <- MemoryCache.createMemoryCache[IO, String, PageInfo](TimeSpec.fromDuration(12.hours))
    client = JavaNetClientBuilder[IO](global).create
    graphQL = new GraphQL(client, sys.env.getOrElse("GITHUB_ACCESS_TOKEN", ""))
    service <- new PullRequestsRoutes[IO]()
      .routes(cache, graphQL, PRDashboardsConfig(List("igwp"), List.empty, 365.days))
      .orNotFound(req)
  } yield service).unsafeRunSync

  "PullRequestsRoutes" should {
    "respond to /prs-quarterly" in {
      val response = serve(Request(GET, Uri(path = "/prs-quarterly")))
      response.status must_== (Ok)
    }
    "respond to /prs-monthly" in {
      val response = serve(Request(GET, Uri(path = "/prs-monthly")))
      response.status must_== (Ok)
    }
  }
}
