package booksontweet

import cats.effect.Effect
import doobie.util.transactor.Transactor
import org.http4s.HttpService
import booksontweet.http.{HttpErrorHandler, UserHttpEndpoint}
import booksontweet.repository.PostgresUserRepository
import booksontweet.repository.algebra.UserRepository
import booksontweet.service.UserService

// Custom DI module
class Module[F[_] : Effect] {

  private val xa: Transactor[F] =
    Transactor.fromDriverManager[F](
      "org.postgresql.Driver", "jdbc:postgresql:users", "postgres", "postgres"
    )

  private val userRepository: UserRepository[F] =
    new PostgresUserRepository[F](xa)

  private val userService: UserService[F] =
    new UserService[F](userRepository)

  private val httpErrorHandler: HttpErrorHandler[F] =
    new HttpErrorHandler[F]

  val userHttpEndpoint: HttpService[F] =
    new UserHttpEndpoint[F](userService, httpErrorHandler).service

}
