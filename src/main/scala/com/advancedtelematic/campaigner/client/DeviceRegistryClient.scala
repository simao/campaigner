package com.advancedtelematic.campaigner.client

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.Materializer
import cats.syntax.show._
import com.advancedtelematic.campaigner.data.DataType._
import com.advancedtelematic.libats.data.{Namespace, PaginationResult}
import com.advancedtelematic.libats.messaging_datatype.DataType.DeviceId
import scala.concurrent.{ExecutionContext, Future}

trait DeviceRegistryClient {
  def devicesInGroup(namespace: Namespace,
                     groupId: GroupId,
                     offset: Long,
                     limit: Long): Future[Seq[DeviceId]]
}

class DeviceRegistryHttpClient(uri: Uri)
    (implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer)
    extends HttpClient("device_registry", uri) with DeviceRegistryClient {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

  override def devicesInGroup(namespace: Namespace,
                              groupId: GroupId,
                              offset: Long,
                              limit: Long): Future[Seq[DeviceId]] = {
    val path  = uri.path / "api" / "v1" / "device_groups" / groupId.show / "devices"
    val query = Uri.Query(Map("offset" -> offset.toString, "limit" -> limit.toString))
    val req   = HttpRequest(HttpMethods.GET, uri = uri.withPath(path).withQuery(query))
    execHttp[PaginationResult[DeviceId]](namespace, req).map(_.values)
  }

}