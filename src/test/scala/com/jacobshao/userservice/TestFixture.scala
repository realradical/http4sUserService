package com.jacobshao.userservice

trait TestFixture {

  val someValidUserId: UserId = UserId(12)
  val someInValidUserId: UserId = UserId(99)
  val someValidUserEmail: EmailAddress = EmailAddress("someuser@test.com")
  val someInValidUserEmail: EmailAddress = EmailAddress("someusertest.com")
  val someValidUserFirstName: FirstName = FirstName("Rachel")
  val someValidUserLastName: LastName = LastName("Howell")
  val someValidReqResUserResponse: ReqResUserResponse =
    ReqResUserResponse(UserData(someValidUserFirstName, someValidUserLastName))

  val someUser: User =
    User(someValidUserId, someValidUserEmail, someValidUserFirstName, someValidUserLastName)

  val someValidUserCreationBody =
    s"""{"email": "${someValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""
  val someMalformedUserCreationBody =
    s"""{"email "${someValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""
  val someInValidEmailUserCreationBody =
    s"""{"email": "${someInValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""
  val someInvalidUserIdUserCreationBody =
    s"""{"email": "${someValidUserEmail.value}","user_id": "${someInValidUserId.value}"}"""

  val someValidReqResResponse =
    """{"data":{"id":12,"email":"rachel.howell@reqres.in","first_name":"Rachel","last_name":"Howell","avatar":"https://s3.amazonaws.com/uifaces/faces/twitter/hebertialmeida/128.jpg"},"ad":{"company":"StatusCode Weekly","url":"http://statuscode.org/","text":"A weekly newsletter focusing on software development, infrastructure, the server, performance, and the stack end of things."}}"""

  def generateExpectedResponseError(message: String): ResponseError = ResponseError(s"$message")
}
