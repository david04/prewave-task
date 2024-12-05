package scratchpad

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import fs2.{Pipe, Stream}
import scratchpad.AlertProcessorCatsEffect.decodeJsonIO

import java.io.{BufferedWriter, FileWriter}

object AlertProcessorFS2 {

  val config = ConfigFactory.load()

  import io.circe.*
  import io.circe.generic.semiauto.*

  implicit val ContentDecoder: Decoder[Content] = deriveDecoder[Content]
  implicit val AlertDecoder: Decoder[Alert] = deriveDecoder[Alert]
  implicit val QueryTermDecoder: Decoder[QueryTerm] = deriveDecoder[QueryTerm]

  def alertProcessingPipe(queryTerms: Vector[QueryTerm]): Pipe[IO, Alert, Match] = _.flatMap(alert => {
    Stream.emits(queryTerms.collect({
      case queryTerm if queryTerm.matchesAlert(alert) => Match(alertId = alert.id, queryTermId = queryTerm.id)
    }))
  })

  def alertSavingPipe(bufferedWriter: BufferedWriter): Pipe[IO, Match, Nothing] = s => s.evalTap(`match` => for {
    _ <- IO.blocking(bufferedWriter.write(`match`.outputFormatted + System.lineSeparator()))
    // _ <- IO.blocking(bufferedWriter.flush()) // flush immediately for testing purposes
  } yield ()).drain

  def guaranteeUniquenessPipe: Pipe[IO, Match, Match] = matches => {
    val alreadyMatched = scala.collection.mutable.Set[Match]()
    matches.filter(!alreadyMatched.contains(_)).evalTap(`match` => IO(alreadyMatched += `match`))
  }

  def main(args: Array[String]): Unit = {

    import cats.effect.unsafe.implicits.global

    val stream = Stream.eval(AlertProcessorCatsEffect.queryServicesIO)
      .flatMap(servicesResponse => Stream.eval(decodeJsonIO(servicesResponse)))
      .flatMap({
        case (queryTerms, alerts) =>
          Stream.bracket(
            IO.blocking(new BufferedWriter(new FileWriter("output.txt")))
          )(bw => {
            IO.blocking(bw.close())
          }).flatMap(bufferedWriter => {
            for {
              _ <- Stream.eval(IO.println("#Query terms: " + queryTerms.size))
              _ <- Stream.eval(IO.println("#Alerts: " + alerts.size))
              _ <- Stream.emits(alerts)
                // .evalTap(_ => IO.sleep(1.seconds)) // test going slower
                .through(alertProcessingPipe(queryTerms))
                .through(guaranteeUniquenessPipe)
                .through(alertSavingPipe(bufferedWriter))
            } yield ()
          })
      })

    timeStream("Full stream")(stream).compile.drain.unsafeRunSync()
  }
}
