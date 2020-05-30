package com.jacobshao.userservice

trait TestFixture {

  val someValidUserId: UserId = UserId(1)
  val someInValidUserId: UserId = UserId(99)
  val someValidUserEmail: EmailAddress = EmailAddress("someuser@test.com")
  val someInValidUserEmail: EmailAddress = EmailAddress("someusertest.com")
  val someValidUserFirstName: FirstName = FirstName("Bob")
  val someValidUserLastName: LastName = LastName("McGrill")
  val someValidReqResUserResponse: ReqResUserResponse =
    ReqResUserResponse(UserData(someValidUserFirstName, someValidUserLastName))

  val someUser: User = User(someValidUserId, someValidUserEmail, someValidUserFirstName, someValidUserLastName)

  val someValidUserCreationBody = s"""{"email": "${someValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""
  val someMalformedUserCreationBody = s"""{"email "${someValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""
  val someInValidEmailUserCreationBody = s"""{"email": "${someInValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""
  val someInvalidUserIdUserCreationBody = s"""{"email": "${someValidUserEmail.value}","user_id": "${someInValidUserId.value}"}"""

  def generateExpectedResponseError(message: String): ResponseError = ResponseError(s"$message")
}
