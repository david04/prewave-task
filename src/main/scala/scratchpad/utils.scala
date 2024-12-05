package scratchpad

import cats.effect.IO
import fs2.Stream

import java.io.{BufferedWriter, FileWriter}

val Start = System.currentTimeMillis()

def timeIO[T](label: String)(io: IO[T]): IO[T] = for {
  start <- IO.delay(System.currentTimeMillis())
  _ <- IO.println(s"[${System.currentTimeMillis() - Start}]:\tRunning $label...")
  rslt <- io
  _ <- IO.println(s"[${System.currentTimeMillis() - Start}]:\tTook ${System.currentTimeMillis() - start}ms to run $label")
} yield rslt

def timeStream[T](label: String)(stream: fs2.Stream[IO, T]): fs2.Stream[IO, T] =
  Stream.bracket(
    IO.delay(System.currentTimeMillis())
  )(start =>
    IO.println(s"[${System.currentTimeMillis() - Start}]:\tTook ${System.currentTimeMillis() - start}ms to run $label")
  ).flatMap(_ => stream)

