package booksontweet.service

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.functor._
import booksontweet.model._
import booksontweet.repository.algebra.UserRepository

class UserService[F[_] : Async](userRepo: UserRepository[F]) {

  def findUser(username: UserName): F[ApiError Either User] =
    userRepo.findUser(username) map { maybeUser =>
      maybeUser.toRight[ApiError](UserNotFound(username))
    }

  // TODO: To be completed by final user :)
  def findAll: F[ApiError Either List[User]] = EitherT.rightT(List.empty[User]).value
  def addUser(user: User): F[ApiError Either Unit] = EitherT.rightT(()).value
  def updateUser(user: User): F[ApiError Either Unit] = EitherT.rightT(()).value
  def deleteUser(username: UserName): F[ApiError Either Unit] = EitherT.rightT(()).value
}

