package booksontweet

//import cats.effect.{Effect, IO}
import cats.effect._
import cats.implicits.{catsSyntaxFlatMapOps => _, _}
import fs2.StreamApp.ExitCode
import fs2.{Chunk, Pipe, Pull, Scheduler, Stream, StreamApp, io, text}
import org.http4s.server.blaze.BlazeBuilder
import java.nio.file.Paths

import cats.{Applicative, Eq}

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

sealed trait BookPart extends Any {
  def length(): Int
}

object BookPart {
  implicit val catsEq: Eq[BookPart] = _ == _
}

case object Paragraph extends BookPart {
  override val toString: String = " ¶ "
  override val length: Int = 3
}

case class Chapter(id: String) extends AnyVal with BookPart {
  override def toString(): String = s"*Ch $id*: "
  override def length(): Int = toString.length
}

case class Text(copy: String) extends AnyVal with BookPart {
  override def toString(): String = copy.toString + " "
  override def length(): Int = toString.length()
}

case class BookTweet(bps: IndexedSeq[BookPart]) {
  def length(): Int = bps.map(_.length).sum
  lazy val toTweet: String = bps.mkString
  override lazy val toString: String = s"[${length} chars] $toTweet"

  def :+(bp: BookPart): BookTweet = BookTweet(bps :+ bp)
}

class CliRunner[F[_]: Effect](implicit F: Applicative[F]) extends StreamApp[F] {
  import ExecutionContext.Implicits.global

  def untilChunkSize[F[_]](size: Int): Pipe[F, BookPart, BookTweet] = {

    def go(buffer: Vector[BookPart],
           s: Stream[F, BookPart]): Pull[F, BookTweet, Option[Unit]] = {
      s.pull.unconsChunk.flatMap {
        case Some((chunk, s)) =>
          val parts = chunk.toVector.flatMap {
            case Text(c) => c.split(" ").toVector.map(Text(_))
            case p       => Vector(p)
          }
          val (bt, carry) =
            parts.foldLeft(BookTweet(buffer), Vector.empty[BookPart]) {
              case ((bt, c), bp) =>
                if (bt.length >= size) bt -> (c :+ bp)
                else {
                  val newBt = bt :+ bp
                  if (newBt.length >= size) bt -> (c :+ bp)
                  else newBt -> c
                }
            }

          if (!carry.isEmpty) Pull.output1(bt) >> go(carry, s)
          else go(buffer ++ parts, s)
        case None if buffer.nonEmpty =>
          Pull.output1(BookTweet(buffer)) >> Pull.pure(None)
        case None => Pull.pure(None)
      }
    }

    s =>
      go(Vector.empty, s).stream
  }

  override def stream(args: List[String],
                      requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    val path = args.head
    val offset = if (args.length > 1) args(1).toInt else 0
    val chunkSize = if (args.length > 2) args(2).toInt else 140
    val max = if (args.length > 3) args(3).toInt else 25

    println(s"Running for path '$path' @ +$offset by $chunkSize ...")

    val book: Stream[F, ExitCode] = io.file
      .readAll(Paths.get(path), 2048)
      .drop(offset)
      .through(text.utf8Decode)
      .through(text.lines)
      .map[BookPart](l =>
        l match {
          case p if p.isEmpty                 => Paragraph
          case c if c.matches("^[IVXLDCM]+$") => Chapter(c)
          case _                              => Text(l)
      })
      .unchunk
      .changes
      .through[BookTweet](untilChunkSize(chunkSize))
      .mapAccumulate(0)((i, v) => (i + 1) -> v)
      .map { case (i, s) => s"Tweet #$i: $s\n" }
      .take(max)
      .through(text.utf8Encode)
      .to(io.stdout)
      .drain
//      .repartition(
//        v =>
//          Chunk.indexedSeq(
//            v.replaceAll("[\\r\\n]+", " \\ ")
//              .replaceAll("\\s+", " ")
//              .grouped(chunkSize)
//              .filterNot(_.isEmpty)
//              .toVector))
//      .mapAccumulate(0)((i, v) => (i + 1) -> v)
//      .map { case (i, s) => s"Tweet #$i: $s\n" }
//      .take(max)
//      .through(text.utf8Encode)
//      .to(io.stdout)
//      .drain

    val result: Stream[F, ExitCode] =
      Stream.eval(F.pure((ExitCode.Success: ExitCode)))

    book ++ result
  }
}
