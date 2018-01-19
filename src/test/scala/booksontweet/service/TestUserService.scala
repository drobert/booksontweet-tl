package booksontweet.service

import cats.effect.IO
import booksontweet.TestUsers.users
import booksontweet.model.UserName
import booksontweet.repository.algebra.UserRepository

object TestUserService {

  private val testUserRepo: UserRepository[IO] =
    (username: UserName) => IO {
      users.find(_.username.value == username.value)
    }

  val service: UserService[IO] = new UserService[IO](testUserRepo)

}
