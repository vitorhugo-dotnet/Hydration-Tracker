package com.example.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FitbitApiService {
    @GET("1/user/-/foods/log/water/date/{date}.json")
    suspend fun getWaterLogs(
        @Header("Authorization") bearerToken: String,
        @Path("date") dateStr: String // yyyy-MM-dd
    ): FitbitWaterResponse

    @POST("1/user/-/foods/log/water.json")
    suspend fun logWater(
        @Header("Authorization") bearerToken: String,
        @Query("amount") amountMl: Int,
        @Query("unit") unit: String = "ml",
        @Query("date") dateStr: String // yyyy-MM-dd
    ): FitbitLogResponse
}

data class FitbitSummary(val water: Int)
data class FitbitWaterResponse(val summary: FitbitSummary)
data class FitbitLogItem(val logId: Long, val amount: Int)
data class FitbitLogResponse(val waterLog: FitbitLogItem)
