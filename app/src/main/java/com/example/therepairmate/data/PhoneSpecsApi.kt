package com.example.therepairmate.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class PhoneSearchResponse(
    val status: Boolean,
    val data: PhoneSearchData?
)

data class PhoneSearchData(
    val phones: List<PhoneItem>?
)

data class LatestResponse(
    val status: Boolean,
    val data: LatestData?
)

data class LatestData(
    val phones: List<PhoneItem>?
)

data class PhoneItem(
    val brand: String?,
    val phone_name: String,
    val slug: String,
    val image: String,
    val detail: String?
)

data class PhoneDetailResponse(
    val status: Boolean,
    val data: PhoneDetailData?
)

data class PhoneDetailData(
    val phone_name: String,
    val brand: String,
    val thumbnail: String,
    val phone_images: List<String>?,
    val release_date: String?,
    val dimension: String?,
    val os: String?,
    val storage: String?,
    val specifications: List<PhoneSpecificationGroup>?
)

data class PhoneSpecificationGroup(
    val title: String,
    val specs: List<PhoneSpecificationItem>?
)

data class PhoneSpecificationItem(
    val key: String,
    val val_: List<String>?
)

interface PhoneSpecsService {
    @GET("search")
    suspend fun searchPhones(@Query("query") query: String): PhoneSearchResponse

    @GET("latest")
    suspend fun getLatestPhones(): LatestResponse

    @GET("{slug}")
    suspend fun getPhoneDetail(@Path("slug") slug: String): PhoneDetailResponse

    companion object {
        const val BASE_URL = "https://api-mobilespecs.azharimm.dev/"

        val instance: PhoneSpecsService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PhoneSpecsService::class.java)
        }
    }
}
