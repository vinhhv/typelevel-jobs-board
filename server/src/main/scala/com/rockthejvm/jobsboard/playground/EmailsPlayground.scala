package com.rockthejvm.jobsboard.playground

import cats.effect.{IO, IOApp}
import com.rockthejvm.jobsboard.config.EmailServiceConfig
import com.rockthejvm.jobsboard.core.LiveEmails

import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}

object EmailsPlayground {
  def main(args: Array[String]): Unit = {
    // configs
    /*
      Host	    smtp.ethereal.email
      Port	    587
      Security	STARTTLS
      Username	marisol.koelpin@ethereal.email
      Password	n2kzQ3sUMUSd6qVyzn
     */

    val host        = "smtp.ethereal.email"
    val port        = 587
    val user        = "marisol.koelpin@ethereal.email"
    val pass        = "n2kzQ3sUMUSd6qVyzn"
    val frontendUrl = "https://google.com"

    val token = "ABCD1234"

    // properties file
    val prop = new Properties()
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", true)
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.ssl.trust", host)

    // authentication
    val auth = new Authenticator {
      override protected def getPasswordAuthentication: PasswordAuthentication =
        new PasswordAuthentication(user, pass)
    }

    // session
    val session = Session.getInstance(prop, auth)

    // email itself
    val subject = "Email from Rock the JVM"
    val content = s"""
      <div style="
        border: 1px solid black;
        padding: 20px;
        font-family: sans-serif;
        line-height: 2;
        font-size: 20px;
      ">
      <h1>Rock the JVM: Password Recovery</h1>
      <p>Your password recovery token is $token</p>
      <p>
        Click <a href="$frontendUrl/login">here</a> to get back to the application
      </p>
      <p>😘 from Rock the JVM</p>
      </div>
    """

    // message = MIME message
    val message = new MimeMessage(session)
    message.setFrom("vinh@rockthejvm.com")
    message.setRecipients(Message.RecipientType.TO, "the.user@gmail.com")
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")

    // send
    Transport.send(message)
  }
}

object EmailsEffectPlayground extends IOApp.Simple {
  override def run: IO[Unit] = for {
    emails <- LiveEmails[IO](
      EmailServiceConfig(
        host = "smtp.ethereal.email",
        port = 587,
        user = "marisol.koelpin@ethereal.email",
        pass = "n2kzQ3sUMUSd6qVyzn",
        frontendUrl = "https://google.com"
      )
    )
    _ <- emails.sendPasswordRecoveryEmail("someone@rockthejvm.com", "ROCKTJVM")
  } yield ()
}
