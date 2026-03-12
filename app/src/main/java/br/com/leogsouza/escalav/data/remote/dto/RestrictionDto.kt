package br.com.leogsouza.escalav.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RestrictionTypeDto(
    val id: Int,
    val name: String,
    val description: String?
)

@JsonClass(generateAdapter = true)
data class SpecificDateDto(
    val date: String,
    @Json(name = "positionID") val positionId: Int?,
    val notes: String?
)

@JsonClass(generateAdapter = true)
data class DateRangeEntryDto(
    val start: String,
    val end: String,
    @Json(name = "positionID") val positionId: Int?
)

@JsonClass(generateAdapter = true)
data class RestrictionRulesDto(
    val mode: String,
    val operator: String?,
    val weekdays: List<Int>?,
    val periods: List<String>?,
    @Json(name = "specificDates") val specificDates: List<SpecificDateDto>?,
    @Json(name = "dateRanges") val dateRanges: List<DateRangeEntryDto>?
)

@JsonClass(generateAdapter = true)
data class RestrictionDto(
    val id: Int?,
    @Json(name = "volunteer_id") val volunteerId: Int,
    @Json(name = "schedule_id") val scheduleId: Int,
    val description: String,
    @Json(name = "restriction_type_id") val restrictionTypeId: Int,
    @Json(name = "restriction_type") val restrictionType: RestrictionTypeDto?,
    @Json(name = "rules_json") val rulesJson: String?,
    val active: Boolean?,
    val fixed: Boolean?
)

@JsonClass(generateAdapter = true)
data class PaginationInfoDto(
    val page: Int,
    @Json(name = "page_size") val pageSize: Int,
    @Json(name = "total_items") val totalItems: Int,
    @Json(name = "total_pages") val totalPages: Int
)

@JsonClass(generateAdapter = true)
data class PaginatedRestrictionsDto(
    val data: List<RestrictionDto>,
    val pagination: PaginationInfoDto
)
