package com.rockthejvm.jobsboard.core

import cats.*
import cats.implicits.*
import com.stripe.{Stripe => TheStripe}
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import org.typelevel.log4cats.Logger

import com.rockthejvm.jobsboard.logging.syntax.*

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
}

class LiveStripe[F[_]: MonadThrow: Logger] extends Stripe[F] {
  override def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]] = {
    // Globally set constant
    TheStripe.apiKey =
      "sk_test_51OaNapGTqL4gRKRDosL4RQuU9Dimou2D5ReXUaz04jE0xaB03g3dsGwwDCvDeMQSr0CXt0k5gIY9ULnDwm8Bkzk100UzO38s1f"

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
      .setSuccessUrl(s"localhost:1234/job/$jobId") // need config
      .setCancelUrl(s"localhost:1234")             // need config
      .setCustomerEmail(userEmail)
      .setClientReferenceId(jobId) // will be sent back to us by the webhook
      .addLineItem(
        SessionCreateParams.LineItem
          .builder()
          .setQuantity(1L)
          .setPrice("price_1OaNejGTqL4gRKRDrqurcIft") // need config
          .build()
      )
      .build()
      .pure[F]
      .map(params => Session.create(params))
      .map(_.some)
      .logError(error => s"Creating checkout session failed: $error")
      .recover { case _ => None }
  }
}

object LiveStripe {
  def apply[F[_]: MonadThrow: Logger](): F[LiveStripe[F]] =
    new LiveStripe[F].pure[F]
}
