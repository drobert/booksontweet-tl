package booksontweet

//import cats.effect.{Effect, IO}
import cats.effect._
import fs2.StreamApp.ExitCode
import fs2.{Chunk, Scheduler, Stream, StreamApp, io, text}
import org.http4s.server.blaze.BlazeBuilder
import java.nio.file.Paths

import cats.Applicative

import scala.concurrent.ExecutionContext

// The only place where the Effect is defined. You could change it for `monix.eval.Task` for example.
object Server extends HttpServer[IO]

class HttpServer[F[_]: Effect] extends StreamApp[F] {

  private val ctx = new Module[F]

  override def stream(args: List[String],
                      requestShutdown: F[Unit]): Stream[F, ExitCode] =
    Scheduler(corePoolSize = 2) flatMap { implicit scheduler =>
      BlazeBuilder[F]
        .bindHttp() // Default address `localhost:8080`
        .mountService(ctx.userHttpEndpoint, "/users") // You can mount as many services as you want
        .serve
    }

}

object Runner extends CliRunner[IO]

class CliRunner[F[_]: Effect](implicit F: Applicative[F]) extends StreamApp[F] {
  import ExecutionContext.Implicits.global

  override def stream(args: List[String],
                      requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    val path = args.head
    val offset = if (args.length > 1) args(1).toInt else 0
    val chunkSize = if (args.length > 2) args(2).toInt else 140
    val max = if (args.length > 3) args(3).toInt else 5

    println(s"Running for path '$path' @ +$offset by $chunkSize ...")

    val book: Stream[F, ExitCode] = io.file
      .readAll(Paths.get(path), 2048)
      .drop(offset)
      .through(text.utf8Decode)
      .segmentN(chunkSize)
      .map(s => s.mapConcat(Chunk.singleton(_)).force.toVector.reduce(_ + _))
      .map(l => s"Tweet: $l\n")
      .take(max)
      .through(text.utf8Encode)
      .to(io.stdout)
      .drain

    val result: Stream[F, ExitCode] =
      Stream.eval(F.pure((ExitCode.Success: ExitCode)))

//    val combined: Stream[F, ExitCode] = book.append(result)
//
//    combined

    book ++ result
  }
}
