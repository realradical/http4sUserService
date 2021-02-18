package com.jacobshao

import cats.data.ValidatedNec
import cats.implicits._
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import monix.eval.Task
import org.http4s.EntityDecoder
import org.http4s.circe._

import scala.util.control.NoStackTrace

package object userservice {

  /** ADTs
    */
  case class EmailAddress(value: String) extends AnyVal

  implicit val emailAddressDecoder: Decoder[EmailAddress] = deriveUnwrappedDecoder
  implicit val emailAddressEncoder: Encoder[EmailAddress] = deriveUnwrappedEncoder

  case class UserId(value: Int) extends AnyVal

  implicit val userIdDecoder: Decoder[UserId] = deriveUnwrappedDecoder
  implicit val userIdEncoder: Encoder[UserId] = deriveUnwrappedEncoder

  case class FirstName(value: String) extends AnyVal

  implicit val firstNameDecoder: Decoder[FirstName] = deriveUnwrappedDecoder
  implicit val firstNameEncoder: Encoder[FirstName] = deriveUnwrappedEncoder

  case class LastName(value: String) extends AnyVal
  implicit val lastNameDecoder: Decoder[LastName] = deriveUnwrappedDecoder
  implicit val lastNameEncoder: Encoder[LastName] = deriveUnwrappedEncoder

  case class UserCreationRequest(email: EmailAddress, user_id: UserId)

  final case class User(
      userId: UserId,
      email: EmailAddress,
      firstName: FirstName,
      lastName: LastName
  )

  implicit val userJsonDecoder: EntityDecoder[Task, User] = jsonOf[Task, User]

  case class UserData(first_name: FirstName, last_name: LastName)

  case class ReqResUserResponse(data: UserData)
  implicit val reqResUserJsonDecoder: EntityDecoder[Task, ReqResUserResponse] =
    jsonOf[Task, ReqResUserResponse]

  case class ResponseError(message: String)
  implicit val responseErrorJsonDecoder: EntityDecoder[Task, ResponseError] =
    jsonOf[Task, ResponseError]

  /** Exceptions
    */

  sealed trait UserServiceError extends NoStackTrace {

    /** Provides a message appropriate for logging. */
    def message: String
  }

  case object UserDataNotAvailableFailure extends UserServiceError {
    override def message: String = "User data with this user_id is not available."
  }

  case object UserAlreadyExistsFailure extends UserServiceError {
    override def message: String = "User with this email already exists."
  }

  case object UserNotExistsFailure extends UserServiceError {
    override def message: String = "User with this email does not exists."
  }

  case object InvalidEmailFormatFailure extends UserServiceError {
    override def message: String = "Invalid email format."
  }

  case class UserCreationRequestInvalidFailure(errorMessage: String) extends UserServiceError {
    override def message: String = errorMessage
  }

  /** Form Validation
    */

  private val emailRegex =
    "(?:[a-z0-9!#$%&'*+\\/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+\\/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"

  sealed trait DomainValidation {
    def errorMessage: String
  }

  case object EmailInvalid extends DomainValidation {
    def errorMessage: String = "Invalid email address."
  }

  sealed trait CreateRequestValidatorNec {

    type ValidationResult[A] = ValidatedNec[DomainValidation, A]

    def validateEmail(email: EmailAddress): ValidationResult[EmailAddress] =
      if (email.value.matches(emailRegex))
        email.validNec
      else EmailInvalid.invalidNec

    def validateRequest(req: UserCreationRequest): ValidationResult[EmailAddress] = validateEmail(
      req.email
    )
  }

  object CreateRequestValidatorNec extends CreateRequestValidatorNec

}
