package booksontweet

import java.io.File

import cats.effect._
import fs2.StreamApp.ExitCode
import fs2.{Chunk, Pipe, Pull, Scheduler, Segment, Stream, StreamApp, io, text}
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

// TODO: move this to a 'model' package

sealed trait BookPart extends Any {
  def length(): Int
}

case object Paragraph extends BookPart {
  override val toString: String = " ¶"
  override val length: Int = 3
}

case class Chapter(id: String) extends AnyVal with BookPart {
  override def toString(): String = s" **Ch $id**"
  override def length(): Int = toString.length
}

case class Text(copy: String) extends AnyVal with BookPart {
  override def toString(): String = " " + copy.toString
  override def length(): Int = toString.length()
}

case class BookTweet(bps: IndexedSeq[BookPart]) {
  def length(): Int = toTweet.length
  lazy val toTweet: String = bps.mkString.trim
  override lazy val toString: String = s"[${length} chars] $toTweet"

  def :+(bp: BookPart): BookTweet = BookTweet(bps :+ bp)
}

// TODO: move internal book parsing logic outside of CLI/Runner/Server
object StreamUtils {
  import scala.language.implicitConversions

  implicit def InvariantOps2[F[_], O](s: Stream[F, O]): InvariantOps2[F, O] =
    new InvariantOps2(s)

  final class InvariantOps2[F[_], O](stream: Stream[F, O]) {
    def filterPrevious(f: (O, O) => Boolean): Stream[F, O] = {
      def go(last: O, s: Stream[F, O]): Pull[F, O, Option[Unit]] =
        s.pull.uncons1.flatMap {
          case None =>
            Pull.output1(last) >> Pull.pure[F, Option[Unit]](Option.empty[Unit])
          case Some((hd, tl)) =>
            if (f(last, hd)) Pull.output1(last) >> go(hd, tl) else go(hd, tl)
        }

      stream.pull.uncons1.flatMap {
        case None           => Pull.pure(None)
        case Some((hd, tl)) => go(hd, tl)
      }.stream
    }
  }

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
                if (!c.isEmpty) bt -> (c :+ bp)
                else {
                  val newBt = bt :+ bp
                  if (newBt.length >= size) bt -> Vector(bp)
                  else newBt -> c
                }
            }

          if (!carry.isEmpty) Pull.output1(bt) >> go(carry, s)
          else go(bt.bps.toVector, s)
        case None if buffer.nonEmpty =>
          Pull.output1(BookTweet(buffer)) >> Pull.pure(None)
        case None => Pull.pure(None)
      }
    }

    s =>
      go(Vector.empty, s).stream
  }
}

object Runner extends CliRunner[IO]

class CliRunner[F[_]: Effect](implicit F: Applicative[F]) extends StreamApp[F] {
  import StreamUtils._

  override def stream(args: List[String],
                      requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    // TODO: build a 'BookConfig' class, encapsulating all of the following
    // per book, plus distinct chapter parsers, etc.
    // ...need a larger sample size

    val path =
      if (!args.isEmpty) new File(args(0)).toURI
      else ClassLoader.getSystemResource("kafka-metamorphosis.txt").toURI()
    val offset = if (args.length > 1) args(1).toInt else 873
    val chunkSize = if (args.length > 2) args(2).toInt else 140
    val max = if (args.length > 3) args(3).toInt else Integer.MAX_VALUE
    val totalBytes = if (args.length > 4) args(4).toInt else 121988 - 873

    println(s"Running for path '$path' @ +$offset by $chunkSize ...")

    val book: Stream[F, ExitCode] = io.file
      .readAll(Paths.get(path), 2048) // TODO: think through '2048'
      .drop(offset) // TODO: per book config
      .take(totalBytes) // TODO: per book config
      .through(text.utf8Decode)
      .through(text.lines)
      .map[BookPart] {
        case p if p.isEmpty                 => Paragraph
        case c if c.matches("^[IVXLDCM]+$") => Chapter(c)
        case l                              => Text(l.trim)
      }
      // NOTE: won't help for Paragraph -> Chapter
      .filterWithPrevious {
        case (Chapter(_), Paragraph) => false
        case (Paragraph, Paragraph)  => false
        case _                       => true
      }
      // NOTE: will totally help for Paragraph -> Chapter
      .filterPrevious {
        case (Paragraph, Paragraph)  => false
        case (Paragraph, Chapter(_)) => false
        case _                       => true
      }
      .through[BookTweet](untilChunkSize(chunkSize))
      .mapAccumulate(0)((i, v) => (i + 1) -> v)
      .map { case (i, s) => s"Tweet #$i: $s\n" }
      .take(max)
      .through(text.utf8Encode)
      .to(io.stdout)
      .drain

    val result: Stream[F, ExitCode] =
      Stream.eval(F.pure((ExitCode.Success: ExitCode)))

    book ++ result
  }
}
