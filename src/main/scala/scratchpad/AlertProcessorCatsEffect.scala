package scratchpad

import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxTuple2Parallel
import com.typesafe.config.ConfigFactory

import java.io.{BufferedWriter, FileWriter}
import scala.util.Try

object AlertProcessorCatsEffect {

  val config = ConfigFactory.load()

  import io.circe.*
  import io.circe.generic.semiauto.*
  import io.circe.parser.*

  implicit val ContentDecoder: Decoder[Content] = deriveDecoder[Content]
  implicit val AlertDecoder: Decoder[Alert] = deriveDecoder[Alert]
  implicit val QueryTermDecoder: Decoder[QueryTerm] = deriveDecoder[QueryTerm]

  val queryServicesIO: IO[ServicesResponse] = {
    import sttp.client3.*

    val key: String = Try(config.getString("key")).getOrElse(throw new Exception(
      "Please add a file 'application.conf' to resources folder with a single line 'key=\"XXXXX\"'"
    ))
    val queryTermsUrl = uri"https://services.prewave.ai/adminInterface/api/testQueryTerm?key=${key}"
    val alertsUrl = uri"https://services.prewave.ai/adminInterface/api/testAlerts?key=${key}"

    Resource.make(
      IO.blocking(HttpClientSyncBackend())
    )(backend => IO.blocking(backend.close())).use(backend => {

      // Fetch query terms and alerts from server in parallel:
      (
        timeIO("Fetch query terms from server")(IO.blocking(basicRequest.get(queryTermsUrl).send(backend)).flatMap(_._1 match {
          case Left(error) => IO.raiseError(new Exception(s"Failed to fetch with error: $error"))
          case Right(value) => IO.pure(value)
        })),
        timeIO("Fetch alerts from server")(IO.blocking(basicRequest.get(alertsUrl).send(backend)).flatMap(_._1 match {
          case Left(error) => IO.raiseError(new Exception(s"Failed to fetch with error: $error"))
          case Right(value) => IO.pure(value)
        }))
      ).parTupled.map({
        case (queryTermsJson, alertsJson) => ServicesResponse(queryTermsJson, alertsJson)
      })
    })
  }

  val decodeJsonIO: ServicesResponse => IO[(Vector[QueryTerm], Vector[Alert])] = {
    case ServicesResponse(queryTermsJson, alertsJson) =>

      // Decode json in parallel:
      (
        timeIO("Decoding QueryTerms JSON")(IO(decode[Vector[QueryTerm]](queryTermsJson))).flatMap({
          case Left(error) => IO.raiseError(error)
          case Right(value) => IO.pure(value)
        }),
        timeIO("Decoding Alerts JSON")(IO(decode[Vector[Alert]](alertsJson))).flatMap({
          case Left(error) => IO.raiseError(error)
          case Right(value) => IO.pure(value)
        }) // .flatMap(alerts => IO.pure(Stream.emits(alerts).repeat.take(100000).toVector)) // debug with 1M alerts
      ).parTupled
  }

  def main(args: Array[String]): Unit = {
    import cats.effect.unsafe.implicits.global

    val io =
      queryServicesIO.flatMap(decodeJsonIO).flatMap({
        case (queryTerms, alerts) =>

          timeIO("Matching alerts to search terms")(IO.parSequenceN(8)(
            alerts.map(alert => {
              IO.delay(queryTerms.collect({
                case queryTerm if queryTerm.matchesAlert(alert) => Match(alertId = alert.id, queryTermId = queryTerm.id)
              }))
            })
          ))
            // Use a set to guarantee uniqueness:
            .map(_.flatten.toSet)
            .flatMap(matches => {
              timeIO("Writing to file")(
                Resource.make(
                  IO.blocking(new BufferedWriter(new FileWriter("output.txt")))
                )(bw => {
                  IO.blocking(bw.close())
                }).use(bw => {
                  IO.blocking(matches.foreach(m => bw.write(m.outputFormatted + System.lineSeparator())))
                })
              )
            })
      })

    timeIO("Run full IO")(io).unsafeRunSync()
  }
}
