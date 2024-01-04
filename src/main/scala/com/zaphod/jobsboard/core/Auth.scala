package com.zaphod.jobsboard.core

import cats.effect.{Async, Ref}
import cats.*
import cats.data.OptionT
import cats.implicits.*
import com.zaphod.jobsboard.config.SecurityConfig
import com.zaphod.jobsboard.domain.auth.NewPasswordInfo
import com.zaphod.jobsboard.domain.security.{Authenticator, JwtToken}
import com.zaphod.jobsboard.domain.user.{NewUserInfo, Role, User}
import org.typelevel.log4cats.Logger
import tsec.authentication.{BackingStore, IdentityStore, JWTAuthenticator}
import tsec.common.SecureRandomId
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.*

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(newUser: NewUserInfo): F[Option[User]]
  def delete(email: String): F[Boolean]
  def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]]

  def authenticator: Authenticator[F]
}

class LiveAuth[F[_]: Async: Logger] private (
    users: Users[F],
    override val authenticator: Authenticator[F]
) extends Auth[F] {
  override def login(email: String, password: String): F[Option[JwtToken]] =
    for {
      // find user in DB
      user <- users.find(email)
      // check password
      validUser <- user.filterA(user =>
        BCrypt.checkpwBool[F](
          password,
          PasswordHash[BCrypt](user.hashedPassword)
        )
      )
      // return new token
      jwtToken <- validUser.traverse(user => authenticator.create(user.email))
    } yield jwtToken

  override def signUp(newUser: NewUserInfo): F[Option[User]] =
    // find user in DB
    users.find(newUser.email).flatMap {
      case Some(_) => None.pure[F]
      case None =>
        for {
          // hash the new password
          hashedPW <- BCrypt.hashpw[F](newUser.password)
          nUser = User(
            newUser.email,
            hashedPW,
            newUser.firstName,
            newUser.lastName,
            newUser.company,
            Role.RECRUITER
          )
          // create new user in DB
          _ <- users.create(nUser)
        } yield Some(nUser)
    }

  override def delete(email: String): F[Boolean] = users.delete(email)

  override def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]] = {
    def updateUser(user: User, newPassword: String): F[Option[User]] =
      for {
        hashedPW    <- BCrypt.hashpw[F](newPasswordInfo.newPassword)
        updatedUser <- users.update(user.copy(hashedPassword = hashedPW))
      } yield updatedUser

    def updateIfPassCheck(
        passCheck: Boolean,
        user: User,
        newPassword: String
    ): F[Either[String, Option[User]]] =
      if (passCheck) updateUser(user, newPassword).map(Right(_))
      else Left("Invalid password").pure[F]

    def checkAndUpdate(
        user: User,
        newPasswordInfo: NewPasswordInfo
    ): F[Either[String, Option[User]]] =
      for {
        passCheck <- BCrypt.checkpwBool[F](
          newPasswordInfo.oldPassword,
          PasswordHash[BCrypt](user.hashedPassword)
        )
        updateResult <- updateIfPassCheck(passCheck, user, newPasswordInfo.newPassword)
//          if (passCheck) updateUser(user, newPasswordInfo.newPassword).map(Right(_))
//          else Left("Invalid password")
      } yield updateResult

    // find user in DB
    users.find(email).flatMap {
      case None       => Right(None).pure[F]
      case Some(user) => checkAndUpdate(user, newPasswordInfo)
    }
  }
}

object LiveAuth {
  def apply[F[_]: Async: Logger](users: Users[F])(securityConfig: SecurityConfig): F[LiveAuth[F]] = {

    // 1. identity store
    val idStore: IdentityStore[F, String, User] = (email: String) =>
      OptionT[F, User](users.find(email))

    // 2. backing store for JWT tokens: BackingStore[F, id, JwtToken]
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] =
          OptionT(ref.get.map(_.get(id)))

        override def put(elem: JwtToken): F[JwtToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))

        override def update(v: JwtToken): F[JwtToken] =
          put(v)

        override def delete(id: SecureRandomId): F[Unit] =
          ref.modify(store => (store - id, ()))
      }
    }

    // 3. key for hashing
    val keyF = HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8"))

    // 4. authenticator
    for {
      key <- keyF
      tokenStore <- tokenStoreF
      authenticator = JWTAuthenticator.backed.inBearerToken(
        expiryDuration = securityConfig.jwtExpiryDuration, // expiration of tokens
        maxIdle = None, // max idle time (optional)
        tokenStore = tokenStore,
        identityStore = idStore, // identity store
        signingKey = key // hash key
      )
    } yield new LiveAuth[F](users, authenticator)
  }
}
