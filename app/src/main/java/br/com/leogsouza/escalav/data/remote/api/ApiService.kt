package br.com.leogsouza.escalav.data.remote.api

import br.com.leogsouza.escalav.data.remote.dto.*
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("login")
    suspend fun login(@Body body: LoginRequest): AuthTokens

    @POST("refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthTokens

    // Schedules
    @GET("schedules/active")
    suspend fun getActiveSchedules(): List<ScheduleDto>

    // Events
    @GET("events/month/{year}/{month}")
    suspend fun getEventsByMonth(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("schedule_id") scheduleId: Int? = null
    ): List<EventDto>

    // Assignments
    @GET("assignments/event/{eventId}")
    suspend fun getAssignmentsByEvent(@Path("eventId") eventId: Int): List<AssignmentDto>

    @GET("schedules/{scheduleId}/assignments/published")
    suspend fun getPublishedAssignments(@Path("scheduleId") scheduleId: Int): List<AssignmentDto>

    // Volunteers
    @GET("volunteers/search")
    suspend fun searchVolunteers(@Query("q") query: String): List<VolunteerDto>

    // Restriction types
    @GET("restriction-types")
    suspend fun getRestrictionTypes(): List<RestrictionTypeDto>

    // Restrictions
    @GET("restrictions")
    suspend fun getRestrictions(
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
        @Query("schedule_id") scheduleId: Int? = null,
        @Query("volunteer_id") volunteerId: Int? = null
    ): PaginatedRestrictionsDto

    @GET("restrictions/{id}")
    suspend fun getRestrictionById(@Path("id") id: Int): RestrictionDto

    @POST("restrictions")
    suspend fun createRestriction(@Body body: RestrictionDto): RestrictionDto

    @PUT("restrictions/{id}")
    suspend fun updateRestriction(@Path("id") id: Int, @Body body: RestrictionDto): Map<String, String>

    @DELETE("restrictions/{id}")
    suspend fun deleteRestriction(@Path("id") id: Int)
}
