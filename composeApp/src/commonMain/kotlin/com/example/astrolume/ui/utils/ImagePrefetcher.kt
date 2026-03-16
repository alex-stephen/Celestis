package com.example.astrolume.ui.utils

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.example.astrolume.model.ApodResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Utility object for predictive image prefetching.
 * Implements the "Perceived Performance" strategy by preloading HD images
 * and buffering images in the background.
 */
object ImagePrefetcher {
    
    /**
     * Prefetch a batch of APOD images for the buffer queue.
     * This implements the "Zero-Latency Random" strategy.
     */
    fun prefetchApodBatch(
        imageLoader: ImageLoader,
        context: PlatformContext,
        apods: List<ApodResponse>,
        scope: CoroutineScope
    ) {
        scope.launch {
            apods.forEach { apod ->
                // Prefetch standard resolution image
                val imageUrl = if (apod.mediaType.equals("video", ignoreCase = true)) {
                    apod.thumbnailUrl ?: apod.url
                } else {
                    apod.url
                }
                
                if (imageUrl != null) {
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .precision(Precision.INEXACT)
                        .build()
                    
                    imageLoader.enqueue(request)
                }
            }
        }
    }
    
    /**
     * Prefetch a single APOD's standard and HD images.
     */
    fun prefetchApodComplete(
        imageLoader: ImageLoader,
        context: PlatformContext,
        apod: ApodResponse,
        scope: CoroutineScope,
        includeHd: Boolean = true
    ) {
        scope.launch {
            // Prefetch standard image
            val imageUrl = if (apod.mediaType.equals("video", ignoreCase = true)) {
                apod.thumbnailUrl ?: apod.url
            } else {
                apod.url
            }
            
            if (imageUrl != null) {
                val standardRequest = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .precision(Precision.INEXACT)
                    .build()
                
                imageLoader.enqueue(standardRequest)
            }
            
            // Prefetch HD image if available and requested
            if (includeHd && apod.urlHD != null) {
                val hdRequest = ImageRequest.Builder(context)
                    .data(apod.urlHD)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .precision(Precision.INEXACT)
                    .build()
                
                imageLoader.enqueue(hdRequest)
            }
        }
    }
}
