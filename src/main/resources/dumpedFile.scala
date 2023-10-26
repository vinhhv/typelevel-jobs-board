package com.rockthejvm.foundations

import cats.effect.kernel.Resource
import cats.effect.{IO, IOApp}

import java.io.{File, FileWriter, PrintWriter}
import scala.concurrent.duration.*
import scala.io.{Source, StdIn}
import scala.util.Random

object CatsEffect extends IOApp.Simple {

  /*
    Describing computations as values
   */

  // IO = data structure arbitrary computations (including side effects)
  val firstIO: IO[Int] = IO.pure(42)
  val delayedIO: IO[Int] = IO.apply {
    // complex code
    println("I'm just about to produce the meaning of life")
    42
  }

  def evaluationIO[A](io: IO[A]): Unit = {
    import cats.effect.unsafe.implicits.global // "platform"
    val meaningOfLife = io.unsafeRunSync()
    println(s"the result of the effect is: $meaningOfLife")
  }

  // transformation
  // map + flatMap
  val improvedMeaningOfLife = firstIO.map(_ * 2)
  val printedMeaningOfLife  = firstIO.flatMap(mol => IO(println(mol)))

  // for-comprehension
  def smallProgram(): IO[Unit] = for {
    line1 <- IO(StdIn.readLine())
    line2 <- IO(StdIn.readLine())
    _     <- IO(println(line1 + line2))
  } yield ()

  // old style of standard Scala apps
  // def main(args: Array[String]): Unit = {
  //   evaluationIO(smallProgram())
  // }

  // raise/"catch" errors
  val aFailure: IO[Int] =
    IO.raiseError(new RuntimeException("a proper failure"))
  val dealWithIt = aFailure.handleErrorWith { case _: RuntimeException =>
    IO(println("I'm still here, don't worry"))
  }

  // fibers = "lightweight threads"
  val delayedPrint = IO.sleep(1.second) *> IO(println(Random.nextInt(100)))
  val manyPrints = for {
    fib1 <- delayedPrint.start
    fib2 <- delayedPrint.start
    _    <- fib1.join
    _    <- fib2.join
  } yield ()

  val cancelledFiber = for {
    fib <- delayedPrint.onCancel(IO(println("I'm cancelled!"))).start
    _   <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _   <- fib.join
  } yield ()

  // uncancellation
  val ignoredCancellation = for {
    fib <- IO
      .uncancelable(_ => delayedPrint.onCancel(IO(println("I'm cancelled!"))))
      .start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _ <- fib.join
  } yield ()

  // resources
  val readingResource = Resource.make(
    IO(
      Source.fromFile(
        "src/main/scala/com/rockthejvm/foundations/CatsEffect.scala"
      )
    )
  )(source => IO(println("closing source")) *> IO(source.close()))

  val readingEffect =
    readingResource.use(source => IO(source.getLines().foreach(println)))

  // compose resources
  val copiedFileResource =
    Resource.make(
      IO(
        new PrintWriter(
          new FileWriter(new File("src/main/resources/dumpedFile.scala"))
        )
      )
    )(writer => IO(println("closing duplicated file")) *> IO(writer.close()))

  val compositeResource = for {
    source      <- readingResource
    destination <- copiedFileResource
  } yield (source, destination)

  val copyFileEffect = compositeResource.use { case (source, destination) =>
    IO(source.getLines().foreach(destination.println))
  }

  // CE apps have a "run" method returning an IO, which will
  // internally be evaluated in a main function
  override def run: IO[Unit] = copyFileEffect
}
