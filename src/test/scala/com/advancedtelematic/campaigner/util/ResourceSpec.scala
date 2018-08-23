package com.advancedtelematic.campaigner.util

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import cats.syntax.show._
import com.advancedtelematic.campaigner.data.Codecs._
import com.advancedtelematic.campaigner.data.DataType._
import com.advancedtelematic.campaigner.http.Routes
import com.advancedtelematic.libats.data.DataType.Namespace
import com.advancedtelematic.libats.data.PaginationResult
import com.advancedtelematic.libats.test.DatabaseSpec
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import org.scalatest.Suite
import org.scalatest.time.{Seconds, Span}

trait ResourceSpec extends ScalatestRouteTest
  with DatabaseSpec {
  self: Suite with CampaignerSpec =>

  implicit val defaultTimeout = RouteTestTimeout(Span(5, Seconds))

  def apiUri(path: String): Uri = "/api/v2/" + path
  val director = new FakeDirectorClient

  def testNs = Namespace("testNs")

  def header = RawHeader("x-ats-namespace", testNs.get)

  val fakeRegistry = new FakeDeviceRegistry

  val fakeResolver = new FakeResolver

  lazy val routes = new Routes(director, fakeRegistry, fakeResolver).routes

  def createCampaignOk(request: CreateCampaign): CampaignId =
    Post(apiUri("campaigns"), request).withHeaders(header) ~> routes ~> check {
      status shouldBe Created
      responseAs[CampaignId]
    }

  def getCampaignOk(id: CampaignId): GetCampaign =
    Get(apiUri("campaigns/" + id.show)).withHeaders(header) ~> routes ~> check {
      status shouldBe OK
      responseAs[GetCampaign]
    }

  def getCampaignsOk(): PaginationResult[CampaignId] =
    Get(apiUri("campaigns")).withHeaders(header) ~> routes ~> check {
      status shouldBe OK
      responseAs[PaginationResult[CampaignId]]
    }
}

