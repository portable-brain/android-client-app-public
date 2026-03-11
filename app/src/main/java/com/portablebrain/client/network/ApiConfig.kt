package com.portablebrain.client.network

import com.portablebrain.client.BuildConfig

object ApiConfig {
    // BASE_URL is set via buildConfigField in app/build.gradle.kts.
    // To change the backend URL, edit the buildConfigField value there.
    val BASE_URL: String get() = BuildConfig.BASE_URL
}
