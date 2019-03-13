package com.advancedtelematic.campaigner.data

import java.time.Instant
import java.util.UUID

import cats.data.NonEmptyList
import com.advancedtelematic.campaigner.data.DataType.CampaignStatus.CampaignStatus
import com.advancedtelematic.campaigner.data.DataType.CancelTaskStatus.CancelTaskStatus
import com.advancedtelematic.campaigner.data.DataType.DeviceStatus.DeviceStatus
import com.advancedtelematic.campaigner.data.DataType.GroupStatus.GroupStatus
import com.advancedtelematic.campaigner.data.DataType.MetadataType.MetadataType
import com.advancedtelematic.campaigner.data.DataType.UpdateType.UpdateType
import com.advancedtelematic.libats.data.DataType.Namespace
import com.advancedtelematic.libats.data.UUIDKey.{UUIDKey, UUIDKeyObj}
import com.advancedtelematic.libats.messaging_datatype.DataType.{DeviceId, UpdateId}

object DataType {

  final case class CampaignId(uuid: UUID) extends UUIDKey
  object CampaignId extends UUIDKeyObj[CampaignId]

  final case class GroupId(uuid: UUID) extends UUIDKey
  object GroupId extends UUIDKeyObj[GroupId]

  final case class Campaign(
    namespace: Namespace,
    id: CampaignId,
    name: String,
    updateId: UpdateId,
    status: CampaignStatus,
    createdAt: Instant,
    updatedAt: Instant,
    mainCampaignId: Option[CampaignId],
    autoAccept: Boolean = true
  )

  final case class ExternalUpdateId(value: String) extends AnyVal

  final case class UpdateSource(id: ExternalUpdateId, sourceType: UpdateType)

  final case class Update(
                           uuid: UpdateId,
                           source: UpdateSource,
                           namespace: Namespace,
                           name: String,
                           description: Option[String],
                           createdAt: Instant,
                           updatedAt: Instant
                         )

  final case class CreateUpdate(updateSource: UpdateSource, name: String, description: Option[String]) {
    def mkUpdate(ns: Namespace): Update =
      Update(UpdateId.generate(), updateSource, ns, name, description, Instant.now, Instant.now)
  }

  case class CampaignMetadata(campaignId: CampaignId, `type`: MetadataType, value: String)

  case class CreateCampaignMetadata(`type`: MetadataType, value: String) {
    def toCampaignMetadata(campaignId: CampaignId) = CampaignMetadata(campaignId, `type`, value)
  }

  final case class CreateCampaign(name: String,
                                  update: UpdateId,
                                  groups: NonEmptyList[GroupId],
                                  mainCampaignId: Option[CampaignId],
                                  metadata: Option[Seq[CreateCampaignMetadata]] = None,
                                  approvalNeeded: Option[Boolean] = Some(false))
  {
    def mkCampaign(ns: Namespace): Campaign = {
      Campaign(
        ns,
        CampaignId.generate(),
        name,
        update,
        CampaignStatus.prepared,
        Instant.now(),
        Instant.now(),
        mainCampaignId,
        !approvalNeeded.getOrElse(false)
      )
    }

    def mkCampaignMetadata(campaignId: CampaignId): Seq[CampaignMetadata] =
      metadata.toList.flatten.map(_.toCampaignMetadata(campaignId))
  }


  final case class GetCampaign(
    namespace: Namespace,
    id: CampaignId,
    name: String,
    update: UpdateId,
    status: CampaignStatus,
    createdAt: Instant,
    updatedAt: Instant,
    mainCampaignId: Option[CampaignId],
    retryCampaignIds: Set[CampaignId],
    // TODO remove the field when FE is not dependent on it anymore
    groups: Set[GroupId],
    metadata: Seq[CreateCampaignMetadata],
    autoAccept: Boolean
  )

  object GetCampaign {
    def apply(c: Campaign, retryCampaignIds: Set[CampaignId], groups: Set[GroupId], metadata: Seq[CampaignMetadata]): GetCampaign =
      GetCampaign(
        c.namespace,
        c.id,
        c.name,
        c.updateId,
        c.status,
        c.createdAt,
        c.updatedAt,
        c.mainCampaignId,
        retryCampaignIds,
        groups,
        metadata.map(m => CreateCampaignMetadata(m.`type`, m.value)),
        c.autoAccept
      )
  }

  final case class UpdateCampaign(name: String, metadata: Option[Seq[CreateCampaignMetadata]] = None)

  final case class Stats(processed: Long, affected: Long)

  final case class GroupStats(
    campaign: CampaignId,
    group: GroupId,
    status: GroupStatus,
    processed: Long,
    affected: Long
  )

  final case class CampaignStats(
    campaign: CampaignId,
    status: CampaignStatus,
    finished: Long,
    failed: Set[DeviceId],
    cancelled: Long,
    processed: Long,
    affected: Long
  )

  final case class DeviceUpdate(
    campaign: CampaignId,
    update: UpdateId,
    device: DeviceId,
    status: DeviceStatus,
    resultCode: Option[String] = None
  )

  final case class CancelTask(
    campaign: CampaignId,
    status: CancelTaskStatus
  )

  object SortBy {
    sealed trait SortBy
    case object Name      extends SortBy
    case object CreatedAt extends SortBy
  }

  object GroupStatus extends Enumeration {
    type GroupStatus = Value
    val scheduled, launched, cancelled = Value
  }

  object CampaignStatus extends Enumeration {
    type CampaignStatus = Value
    val prepared, launched, finished, cancelled = Value
  }

  /**
   * Status of a device in the campaign:
   * - `requested` when a device is initially added to the campaign
   * - `rejected` when a device is rejected by Director and is not a part of the
   *    campaign anymore
   * - `scheduled` when a device is approved by Director and is scheduled for
   *    an update
   * - `accepted` when an update was accepted on a device and is about to be
   *    installed
   * - `successful` when a device update was applied successfully
   * - `cancelled` when a device update was cancelled
   * - `failed` when a device update was failed
   */
  object DeviceStatus extends Enumeration {
    type DeviceStatus = Value
    val requested, rejected, scheduled, accepted, successful, cancelled, failed = Value
  }

  object CancelTaskStatus extends Enumeration {
    type CancelTaskStatus = Value
    val error, pending, inprogress, completed = Value
  }

  object UpdateType extends Enumeration {
    type UpdateType = Value
    val external, multi_target = Value
  }

  object MetadataType extends Enumeration {
    type MetadataType = Value
    val DESCRIPTION, ESTIMATED_INSTALLATION_DURATION, ESTIMATED_PREPARATION_DURATION = Value
  }
}
