/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.predicates

import common.{DelegatedAuthRules, EnrolmentIdentifiers, EnrolmentKeys}
import config.AppConfig
import models.User
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedActionSpec extends TestUtils {

  override lazy val mockAppConfig: AppConfig = mock[AppConfig]
  override val authorisedAction: AuthorisedAction = {
    new AuthorisedAction()(mockAuthConnector, defaultActionBuilder, mockAppConfig, mockControllerComponents)
  }

  ".enrolmentGetIdentifierValue" should {

    "return the value for a given identifier" in {
      val returnValue = "anIdentifierValue"
      val returnValueAgent = "anAgentIdentifierValue"

      val enrolments = Enrolments(Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, returnValue)), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, returnValueAgent)), "Activated")
      ))

      authorisedAction.enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments) mustBe Some(returnValue)
      authorisedAction.enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) mustBe Some(returnValueAgent)
    }

    "return a None" when {
      val key = "someKey"
      val identifierKey = "anIdentifier"
      val returnValue = "anIdentifierValue"

      val enrolments = Enrolments(Set(Enrolment(key, Seq(EnrolmentIdentifier(identifierKey, returnValue)), "someState")))


      "the given identifier cannot be found" in {
        authorisedAction.enrolmentGetIdentifierValue(key, "someOtherIdentifier", enrolments) mustBe None
      }

      "the given key cannot be found" in {
        authorisedAction.enrolmentGetIdentifierValue("someOtherKey", identifierKey, enrolments) mustBe None
      }

    }

    ".individualAuthentication" should {

      "perform the block action" when {

        "the correct enrolment exist and nino exist" which {
                  val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
                  val mtditid = "AAAAAA"
                  val enrolments = Enrolments(
                    Set(
                      Enrolment(EnrolmentKeys.Individual,
                        Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
                      Enrolment(
                        EnrolmentKeys.nino,
                        Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
                    )
                  )

                  lazy val result: Future[Result] = {
                    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
                      .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
                      .returning(Future.successful(enrolments and ConfidenceLevel.L250))
                    authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
                  }

                  "returns an OK status" in {
                    status(result) mustBe OK
                  }

                  "returns a body of the mtditid" in {
                    bodyOf(result) mustBe mtditid
                  }
                }

        "the correct enrolment and nino exist but the request is for a different id" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val mtditid = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment(
            EnrolmentKeys.Individual,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "123456")), "Activated"),
            Enrolment(
              EnrolmentKeys.nino,
              Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L250))
            authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
          }

          "returns an UNAUTHORIZED status" in {
            status(result) mustBe UNAUTHORIZED
          }
        }

        "the correct enrolment and nino exist but low CL" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val mtditid = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment(
            EnrolmentKeys.Individual,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
            Enrolment(
              EnrolmentKeys.nino,
              Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L50))
            authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
          }

          "returns an UNAUTHORIZED status" in {
            status(result) mustBe UNAUTHORIZED
          }
        }

        "the correct enrolment exist but no nino" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val mtditid = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment(
            EnrolmentKeys.Individual,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L250))
            authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
          }

          "returns an 401 status" in {
            status(result) mustBe UNAUTHORIZED
          }
        }

        "the correct nino exist but no enrolment" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val id = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment(
            EnrolmentKeys.nino,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, id)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L250))
            authorisedAction.individualAuthentication(block, id)(fakeRequest, emptyHeaderCarrier)
          }

          "returns an 401 status" in {
            status(result) mustBe UNAUTHORIZED
          }
        }

      }

      "return a UNAUTHORIZED" when {

        "the correct enrolment is missing" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val mtditid = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment("notAnIndividualOops", Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated")))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L250))
            authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
          }

          "returns a forbidden" in {
            status(result) mustBe UNAUTHORIZED
          }
        }
      }

      "the correct enrolment and nino exist but the request is for a different id" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(
          EnrolmentKeys.Individual,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "123456")), "Activated"),
          Enrolment(
            EnrolmentKeys.nino,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an UNAUTHORIZED status" in {
          status(result) mustBe UNAUTHORIZED
        }
      }

      "the correct enrolment and nino exist but low CL" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(
          EnrolmentKeys.Individual,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(
            EnrolmentKeys.nino,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L50))
          authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an UNAUTHORIZED status" in {
          status(result) mustBe UNAUTHORIZED
        }
      }

      "the correct enrolment exist but no nino" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(
          EnrolmentKeys.Individual,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an 401 status" in {
          status(result) mustBe UNAUTHORIZED
        }
      }

      "the correct nino exist but no enrolment" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val id = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(
          EnrolmentKeys.nino,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, id)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          authorisedAction.individualAuthentication(block, id)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an 401 status" in {
          status(result) mustBe UNAUTHORIZED
        }
      }
    }

    ".agentAuthenticated" should {

      val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(s"${user.mtditid} ${user.arn.get}"))

      "perform the block action" when {

        "the agent is authorised for the given user" which {

          val enrolments = Enrolments(Set(
            Enrolment(
              key = EnrolmentKeys.Individual,
              identifiers = Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")),
              state = "Activated",
              delegatedAuthRule = Some(DelegatedAuthRules.agentDelegatedAuthRule)
            ),
            Enrolment(
              key = EnrolmentKeys.Agent,
              identifiers = Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")),
              state = "Activated"
            )
          ))

          lazy val result = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments, *, *)
              .returning(Future.successful(enrolments))

            authorisedAction.agentAuthentication(block, "1234567890")(fakeRequest, emptyHeaderCarrier)
          }

          "has a status of OK" in {
            status(result) mustBe OK
          }

          "has the correct body" in {
            bodyOf(result) mustBe "1234567890 0987654321"
          }
        }

        "the agent is authorised as an ema supporting agent, and the supporting agent feature is enabled" which {

          val enrolments = Enrolments(Set(
            Enrolment(
              key = EnrolmentKeys.SupportingAgent,
              identifiers = Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")),
              state = "Activated",
              delegatedAuthRule = Some(DelegatedAuthRules.supportingAgentDelegatedAuthRule)
            ),
            Enrolment(
              key = EnrolmentKeys.Agent,
              identifiers = Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")),
              state = "Activated"
            )
          ))

          lazy val result = {

            //First auth call to fail
            object AuthException extends AuthorisationException("not primary agent")
            mockAuthReturnException(AuthException).once()

            //Then check if Supporting Agent is enabled
            (() => mockAppConfig.emaSupportingAgentsEnabled).expects().returning(true)

            //Second call for supporting agent
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments, *, *)
              .returning(Future.successful(enrolments))
              .once()

            authorisedAction.agentAuthentication(block,"1234567890")(fakeRequest, emptyHeaderCarrier)
          }

          "has a status of OK" in {
            status(result) mustBe OK
          }

          "has the correct body" in {
            bodyOf(result) mustBe "1234567890 0987654321"
          }
        }

        "the authorisation service returns an AuthorisationException exception (and ema supporting agent is disabled)" in {
          object AuthException extends AuthorisationException("Some reason")

          lazy val result = {
            mockAuthReturnException(AuthException)
            //Disable EMA Supporting Agent feature
            (() => mockAppConfig.emaSupportingAgentsEnabled).expects().returning(false)

            authorisedAction.agentAuthentication(block, "1234567890")(fakeRequest, emptyHeaderCarrier)
          }
          status(result) mustBe UNAUTHORIZED
        }

        "the authorisation service returns an AuthorisationException exception for a delegated ema Support Agent" in {
          object AuthException extends AuthorisationException("Some reason")

          lazy val result = {

            //First auth failure to simulate not being a primary agent
            mockAuthReturnException(AuthException).once()

            //Enable EMA Supporting Agent feature
            (() => mockAppConfig.emaSupportingAgentsEnabled).expects().returning(true)

            //Second auth failure to simulate not being a supporting agent
            mockAuthReturnException(AuthException).once()

            authorisedAction.agentAuthentication(block, "1234567890")(fakeRequest, emptyHeaderCarrier)
          }
          status(result) mustBe UNAUTHORIZED
        }

        "the authorisation service returns a NoActiveSession exception" in {
          object NoActiveSession extends NoActiveSession("Some reason")

          lazy val result = {
            mockAuthReturnException(NoActiveSession)
            authorisedAction.agentAuthentication(block, "1234567890")(fakeRequest, emptyHeaderCarrier)
          }

          status(result) mustBe UNAUTHORIZED
        }

        "the user does not have an enrolment for the agent" in {
          val enrolments = Enrolments(Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated")
          ))

          lazy val result = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments, *, *)
              .returning(Future.successful(enrolments))
            authorisedAction.agentAuthentication(block,"1234567890")(fakeRequest, emptyHeaderCarrier)
          }
          status(result) mustBe UNAUTHORIZED
        }
      }
    }

    ".async" should {

      lazy val block: User[AnyContent] => Future[Result] = user =>
        Future.successful(Ok(s"mtditid: ${user.mtditid}${user.arn.fold("")(arn => " arn: " + arn)}"))

      "perform the block action" when {

        "the user is successfully verified as an agent" which {

          lazy val result: Future[Result] = {
            mockAuthAsAgent()
            authorisedAction.async(block)(fakeRequest)
          }

          "should return an OK(200) status" in {

            status(result) mustBe OK
            bodyOf(result) mustBe "mtditid: 1234567890 arn: 0987654321"
          }
        }

        "the user is successfully verified as a ema supporting agent (feature is enabled)" which {

          lazy val result: Future[Result] = {
            mockAuthAsSupportingAgent()
            authorisedAction.async(block)(fakeRequest)
          }

          "should return an OK(200) status" in {

            status(result) mustBe OK
            bodyOf(result) mustBe "mtditid: 1234567890 arn: 0987654321"
          }
        }

        "the user is successfully verified as an individual" in {

          lazy val result = {
            mockAuth()
            authorisedAction.async(block)(fakeRequest)
          }

          status(result) mustBe OK
          bodyOf(result) mustBe "mtditid: 1234567890"
        }
      }

      "return an Unauthorised" when {

        "the authorisation service returns an AuthorisationException exception" in {
          object AuthException extends AuthorisationException("Some reason")

          lazy val result = {
            mockAuthReturnException(AuthException)
            authorisedAction.async(block)
          }

          status(result(fakeRequest)) mustBe UNAUTHORIZED
        }

      }

      "return an Unauthorised" when {

        "the authorisation service returns a NoActiveSession exception" in {
          object NoActiveSession extends NoActiveSession("Some reason")

          lazy val result = {
            mockAuthReturnException(NoActiveSession)
            authorisedAction.async(block)
          }

          status(result(fakeRequest)) mustBe UNAUTHORIZED
        }
        "the request does not contain mtditid header" in {
          lazy val result = {
            authorisedAction.async(block)
          }

          status(result(FakeRequest())) mustBe UNAUTHORIZED
        }
      }

    }
  }
}
