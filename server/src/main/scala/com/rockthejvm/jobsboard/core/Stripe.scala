package com.rockthejvm.jobsboard.core

import cats.*
import cats.implicits.*
import com.stripe.{Stripe => TheStripe}
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook
import com.stripe.param.checkout.SessionCreateParams
import org.typelevel.log4cats.Logger

import com.rockthejvm.jobsboard.config.StripeConfig
import com.rockthejvm.jobsboard.logging.syntax.*

import scala.jdk.OptionConverters.*
import scala.util.Try

trait Stripe[F[_]] {
  /*
   * 1. Someone calls an endpoint on our server
   *    (send a JobInfo to us) - persisted to the DB - Jobs[F].create(...)
   * 2. Return a checkout page URL
   * 3. The frontend will redirect user to that URL
   * 4. The user pays (fills in CC details...)
   * 5. The backend will be notified by Stripe (webhook)
   *    - test mode: use Stripe CLI to redirect the events to localhost:4041/api/jobs/webhook...
   * 6. Perform the final operation on the job advert - set the active flag to false for that Job ID
   *
   * app -> http -> stripe -> redirect user
   *                                        <- user pays stripe
   * activate job <- webhook <- stripe
   */
  def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]]
  def handleWebhookEvent[A](payload: String, signature: String, action: String => F[A]): F[Option[A]]
}

class LiveStripe[F[_]: MonadThrow: Logger](stripeConfig: StripeConfig) extends Stripe[F] {
  override def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]] = {
    // Globally set constant
    TheStripe.apiKey = stripeConfig.key

    SessionCreateParams
      .builder()
      .setMode(SessionCreateParams.Mode.PAYMENT)
      // automatic receipt/invoice
      .setInvoiceCreation(
        SessionCreateParams.InvoiceCreation.builder().setEnabled(true).build()
      )
      // save the user email
      .setPaymentIntentData(
        SessionCreateParams.PaymentIntentData.builder().setReceiptEmail(userEmail).build()
      )
      .setSuccessUrl(s"${stripeConfig.successUrl}/$jobId")
      .setCancelUrl(stripeConfig.cancelUrl)
      .setCustomerEmail(userEmail)
      .setClientReferenceId(jobId) // will be sent back to us by the webhook
      .addLineItem(
        SessionCreateParams.LineItem
          .builder()
          .setQuantity(1L)
          .setPrice(stripeConfig.price)
          .build()
      )
      .build()
      .pure[F]
      .map(params => Session.create(params))
      .map(_.some)
      .logError(error => s"Creating checkout session failed: $error")
      .recover { case _ => None }
  }

  override def handleWebhookEvent[A](payload: String, signature: String, action: String => F[A]): F[Option[A]] =
    MonadThrow[F]
      .fromTry(
        Try(
          Webhook.constructEvent(
            payload,
            signature,
            stripeConfig.webhookSecret
          )
        )
      )
      .logError(error => "Stripe security verification failed - possibly fake attempt") // TODO: pass from config
      .flatMap { event =>
        event.getType() match {
          case "checkout.session.completed" => // happy path
            event
              .getDataObjectDeserializer()
              // Make sure API matches between build.sbt and API version for API key, or else silent failure (returns None)
              .getObject()                   // Optional[Deserializer]
              .toScala                       // Option[...]
              .map(_.asInstanceOf[Session])  // Option[Session]
              .map(_.getClientReferenceId()) // Option[String] <-- stores my job ID
              .map(action)                   // Option[F[A]] performing the effect
              .sequence                      // F[Option[A]]
              .log(
                {
                  case None    => s"Event ${event.getId()} not producing any effect - check dashboard"
                  case Some(_) => s"Event ${event.getId()} fully paid - OK"
                },
                error => s"Webhook action failed $error"
              )

          case _ =>
            // discard the effect
            None.pure[F]
        }
      }
      .logError(error => s"Something else went wrong: $error") // TODO: pass from config
      .recover { case _ => None }
}

object LiveStripe {
  def apply[F[_]: MonadThrow: Logger](stripeConfig: StripeConfig): F[LiveStripe[F]] =
    new LiveStripe[F](stripeConfig).pure[F]
}
