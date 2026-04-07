package com.example.celestis.ui.utils

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import com.example.celestis.model.ApodResponse
import com.example.celestis.ui.utils.VideoUrlUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Utility object for predictive image prefetching.
 * Implements the "Perceived Performance" strategy by preloading images
 * and buffering images in the background with smart throttling.
 */
object ImagePrefetcher {
    
    /**
     * Prefetch a batch of APOD images for the buffer queue.
     * This implements the "Zero-Latency Random" strategy with optimized sizing.
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
                    // Prefer explicit thumbnailUrl, then derive YouTube thumbnail, then raw url
                    apod.thumbnailUrl
                        ?: apod.url?.let { url ->
                            VideoUrlUtils.extractYouTubeId(url)
                                ?.let { id -> VideoUrlUtils.getYouTubeThumbnail(id) }
                        }
                        ?: apod.url
                } else {
                    apod.url
                }

                if (imageUrl != null) {
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .precision(Precision.INEXACT)
                        // Add size constraint to reduce memory usage - grid items are small
                        .size(400, 400)
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
                apod.thumbnailUrl
                    ?: apod.url?.let { url ->
                        VideoUrlUtils.extractYouTubeId(url)
                            ?.let { id -> VideoUrlUtils.getYouTubeThumbnail(id) }
                    }
                    ?: apod.url
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
