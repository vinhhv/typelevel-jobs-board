package com.rockthejvm.jobsboard.fixtures

import cats.data.OptionT
import cats.effect.IO
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import org.http4s.*
import org.http4s.headers.Authorization
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration.*

trait SecuredRouteFixture extends UserFixture {
  val mockedAuthenticator: Authenticator[IO] = {
    // key for hashing
    val key = HMACSHA256.unsafeGenerateKey
    // identity store for retrieve users
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == vinhEmail) OptionT.pure(Vinh)
      else if (email == joeEmail) OptionT.pure(Joe)
      else OptionT.none[IO, User]
    // jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      1.day,   // expiration of tokens
      None,    // max idle time (optional)
      idStore, // identity store
      key      // hash key
    )
  }

  extension (r: Request[IO])
    def withBearerToken(jwtToken: JwtToken): Request[IO] =
      r.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](jwtToken.jwt)
        // Authorization: Bearer {jwt}
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }
}
