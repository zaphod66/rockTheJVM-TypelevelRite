package com.zaphod.jobsboard.domain

import cats.{Monad, MonadThrow, Semigroup}
import cats.implicits.*
import com.zaphod.jobsboard.domain.user.*
import org.http4s.{Response, Status}
import tsec.authentication.{AugmentedJWT, JWTAuthenticator, SecuredRequestHandler, SecuredRequest, TSecAuthService}
import tsec.authorization.{AuthorizationInfo, BasicRBAC}
import tsec.mac.jca.HMACSHA256

import scala.language.implicitConversions

object security {
  type Crypto = HMACSHA256
  type JwtToken = AugmentedJWT[Crypto, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]
  
  type AuthRoute[F[_]] = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
  type AuthRBAC[F[_]] = BasicRBAC[F, Role, User, JwtToken]

  type SecuredHandler[F[_]] = SecuredRequestHandler[F, String, User, JwtToken]
  
  // RBAC

  given authRole[F[_]: MonadThrow]: AuthorizationInfo[F, Role, User] with {
    override def fetchInfo(u: User): F[Role] = u.role.pure[F]
  }
  def allRoles[F[_]: MonadThrow]: AuthRBAC[F] = BasicRBAC.all[F, Role, User, JwtToken]
  def recruiterOnly[F[_]: MonadThrow]: AuthRBAC[F] = BasicRBAC(Role.RECRUITER)
  def adminOnly[F[_]: MonadThrow]: AuthRBAC[F] = BasicRBAC(Role.ADMIN)

  case class Authorizations[F[_]](rbacRoutes: Map[AuthRBAC[F], List[AuthRoute[F]]])
  private object Authorizations {
    given semigroup[F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance { (authA, authB) =>
      Authorizations(authA.rbacRoutes |+| authB.rbacRoutes)
    }
  }

  // AuthRoute -> Authorizations -> TSecAuthService -> HttpRoute

  // AuthRoute -> Authorizations = .restrictedTo extension method
  extension [F[_]] (authRoute: AuthRoute[F])
    def restrictedTo(rbac: AuthRBAC[F]): Authorizations[F] =
      Authorizations(Map(rbac -> List(authRoute)))

  // Authorizations -> TSecAuthService (implicit conversion)
  given auth2tsec[F[_]: Monad]: Conversion[Authorizations[F], TSecAuthService[User, JwtToken, F]] =
    authz => {
      // this returns 401 always
      val unauthorizedService: TSecAuthService[User, JwtToken, F] = TSecAuthService[User, JwtToken, F] { _ =>
        Response[F](Status.Unauthorized).pure[F]
      }

      authz.rbacRoutes  // map[RBAC -> List[AuthRoute[F]]
        .toSeq
        .foldLeft(unauthorizedService) {
          case (acc, (rbac, routes)) =>
            // merge routes into one
            val bigRoute = routes.reduce(_.orElse(_))
            // build a new service, fallback to the acc if rbac/route fails
            TSecAuthService.withAuthorizationHandler(rbac)(bigRoute, acc.run)
        }
  }
}
