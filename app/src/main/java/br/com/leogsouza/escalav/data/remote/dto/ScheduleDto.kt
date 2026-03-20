package br.com.leogsouza.escalav.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScheduleDto(
    val id: Int,
    val name: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    val status: String,
    @Json(name = "church_id") val churchId: Int
)

@JsonClass(generateAdapter = true)
data class PaginatedSchedulesDto(
    val data: List<ScheduleDto>,
    val pagination: SchedulePaginationDto? = null
)

@JsonClass(generateAdapter = true)
data class SchedulePaginationDto(
    val page: Int? = null,
    @Json(name = "page_size") val pageSize: Int? = null,
    @Json(name = "total_items") val totalItems: Int? = null,
    @Json(name = "total_pages") val totalPages: Int? = null
)

fun List<ScheduleDto>.firstScheduleByStatus(status: String): ScheduleDto? =
    firstOrNull { it.status.equals(status, ignoreCase = true) }

fun List<ScheduleDto>.firstPublishedSchedule(): ScheduleDto? =
    firstScheduleByStatus("PUBLISHED")

fun List<ScheduleDto>.firstDraftSchedule(): ScheduleDto? =
    firstScheduleByStatus("DRAFT")

@JsonClass(generateAdapter = true)
data class EventDto(
    val id: Int,
    val date: String,
    val weekday: String,
    val service: String,
    @Json(name = "service_code") val serviceCode: String,
    val time: String,
    @Json(name = "template_id") val templateId: Int?,
    @Json(name = "schedule_id") val scheduleId: Int?,
    @Json(name = "is_special") val isSpecial: Boolean,
    val notes: String
)

@JsonClass(generateAdapter = true)
data class RoleDto(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class PositionDto(
    val id: Int,
    val name: String,
    @Json(name = "role_id") val roleId: Int,
    val role: RoleDto?
)

@JsonClass(generateAdapter = true)
data class VolunteerDto(
    val id: Int,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val active: Boolean?,
    @Json(name = "main_role_id") val mainRoleId: Int?,
    @Json(name = "secondary_role_id") val secondaryRoleId: Int?,
    @Json(name = "main_role") val mainRole: RoleDto?,
    @Json(name = "secondary_role") val secondaryRole: RoleDto?
)

@JsonClass(generateAdapter = true)
data class AssignmentDto(
    val id: Int,
    @Json(name = "event_id") val eventId: Int,
    @Json(name = "volunteer_id") val volunteerId: Int,
    @Json(name = "position_id") val positionId: Int,
    val status: String,
    val event: EventDto?,
    val position: PositionDto?,
    val volunteer: VolunteerDto?
)
