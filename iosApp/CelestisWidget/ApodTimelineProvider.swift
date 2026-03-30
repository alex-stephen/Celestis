import WidgetKit
import SwiftUI
import ComposeApp

/**
 * Timeline provider for the APOD widget.
 * 
 * This class is responsible for providing timeline entries to WidgetKit,
 * which determines when and what to display in the widget.
 * 
 * Key Integration Points with Shared Kotlin Code:
 * 
 * 1. **Accessing ApodRepository**:
 *    ```swift
 *    let koinHelper = KoinHelperKt.doInitKoin()
 *    let repository = // Get from Koin container
 *    ```
 * 
 * 2. **Reading Latest APOD from SQLDelight**:
 *    The shared ApodRepository provides `observeLatestApodForWidget()` Flow.
 *    You can collect this Flow to get the cached APOD:
 *    ```swift
 *    repository.observeLatestApodForWidget() // Returns Flow<ApodEntity?>
 *    ```
 * 
 * 3. **Loading Pre-Downloaded Images**:
 *    Images are stored in the app's file directory:
 *    ```swift
 *    let imagePath = "\(fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0])/apod_images/apod_\(date).jpg"
 *    let imageData = try? Data(contentsOf: URL(fileURLWithPath: imagePath))
 *    ```
 * 
 * Timeline Strategy:
 * - Widgets refresh based on APOD update schedule (daily at 6:00 AM UTC)
 * - No network calls in widget code (only read from cache)
 * - Background app refresh handles syncing new APOD data
 */
struct ApodTimelineProvider: TimelineProvider {
    
    // MARK: - TimelineProvider Protocol
    
    /**
     * Provides a placeholder entry for initial widget rendering.
     * This is shown while the widget is loading for the first time.
     */
    func placeholder(in context: Context) -> ApodWidgetEntry {
        ApodWidgetEntry(
            date: Date(),
            title: "Astronomy Picture of the Day",
            apodDate: "2026-03-29",
            imageData: nil,
            explanation: "Loading the latest cosmic wonder..."
        )
    }
    
    /**
     * Provides a snapshot entry for widget gallery/preview.
     * This should return quickly for smooth UI.
     */
    func getSnapshot(in context: Context, completion: @escaping (ApodWidgetEntry) -> Void) {
        // For previews, use a placeholder
        if context.isPreview {
            completion(placeholder(in: context))
            return
        }
        
        // For actual snapshot, try to load cached data
        Task {
            let entry = await loadCachedApodEntry()
            completion(entry)
        }
    }
    
    /**
     * Provides the timeline of entries for the widget.
     * 
     * This is where we integrate with the shared Kotlin code to:
     * 1. Access the ApodRepository via Koin
     * 2. Read the latest cached APOD from SQLDelight
     * 3. Load pre-downloaded image data
     * 4. Create timeline entries
     */
    func getTimeline(in context: Context, completion: @escaping (Timeline<ApodWidgetEntry>) -> Void) {
        Task {
            let entry = await loadCachedApodEntry()
            
            // Create timeline with next update at 6:00 AM UTC tomorrow
            // This aligns with the APOD sync schedule
            let nextUpdate = getNextUpdateDate()
            let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
            
            completion(timeline)
        }
    }
    
    // MARK: - Helper Methods
    
    /**
     * Loads the cached APOD entry from the shared Kotlin code.
     * 
     * IMPLEMENTATION REQUIRED:
     * This is where you'll integrate with the shared ApodRepository.
     * 
     * Steps to implement:
     * 1. Access Koin to get ApodRepository instance
     * 2. Call repository.observeLatestApodForWidget() to get the Flow
     * 3. Collect the first emission from the Flow to get ApodEntity
     * 4. Load the image from file storage using the entity's date
     * 5. Create and return an ApodWidgetEntry
     * 
     * Example implementation:
     * ```
     * // Initialize Koin if needed
     * KoinHelperKt.doInitKoin()
     * 
     * // Get repository from Koin (you'll need to expose this via KoinHelper)
     * let repository = KoinHelperKt.getApodRepository()
     * 
     * // Collect latest APOD (convert Kotlin Flow to Swift async)
     * let apodEntity = try? await repository.observeLatestApodForWidget().first()
     * 
     * // Load image from file storage
     * let imagePath = getApodImagePath(date: apodEntity?.date ?? "")
     * let imageData = try? Data(contentsOf: URL(fileURLWithPath: imagePath))
     * 
     * // Create entry
     * return ApodWidgetEntry(
     *     date: Date(),
     *     title: apodEntity?.title ?? "APOD",
     *     apodDate: apodEntity?.date ?? "",
     *     imageData: imageData,
     *     explanation: apodEntity?.explanation
     * )
     * ```
     */
    private func loadCachedApodEntry() async -> ApodWidgetEntry {
        // TODO: Implement integration with shared Kotlin code
        // For now, return a placeholder
        
        // PLACEHOLDER IMPLEMENTATION - Replace with actual Kotlin integration
        return ApodWidgetEntry(
            date: Date(),
            title: "Pillars of Creation",
            apodDate: "2026-03-29",
            imageData: nil,
            explanation: "Replace this with data from shared ApodRepository"
        )
        
        /* PRODUCTION IMPLEMENTATION SHOULD BE:
        
        do {
            // Initialize Koin and get repository
            KoinHelperKt.doInitKoin()
            
            // Access ApodRepository via KoinHelper
            // You'll need to add this function to KoinHelper.kt:
            // fun getApodRepository(): ApodRepository = get()
            guard let repository = KoinHelperKt.getApodRepository() else {
                return placeholder(in: WidgetContext())
            }
            
            // Get latest APOD from SQLDelight (convert Flow to Swift async)
            // This requires a bridge function in Kotlin to convert Flow to suspend function
            let apodEntity = try await repository.getLatestApodForWidget()
            
            guard let apod = apodEntity else {
                return placeholder(in: WidgetContext())
            }
            
            // Load pre-downloaded image
            let imageData = loadImageData(for: apod.date)
            
            return ApodWidgetEntry(
                date: Date(),
                title: apod.title ?? "Astronomy Picture of the Day",
                apodDate: apod.date,
                imageData: imageData,
                explanation: apod.explanation
            )
        } catch {
            print("Error loading APOD from shared code: \(error)")
            return placeholder(in: WidgetContext())
        }
        */
    }
    
    /**
     * Loads image data from the app's file storage.
     * Images are stored at: Documents/apod_images/apod_{date}.jpg
     * 
     * IMPLEMENTATION NOTE:
     * This path should match where ApodSyncWorker stores images on iOS.
     * You may need to adjust based on your actual iOS file storage location.
     */
    private func loadImageData(for date: String) -> Data? {
        let fileManager = FileManager.default
        
        // Get documents directory (shared with main app)
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return nil
        }
        
        // Construct image path
        let imagePath = documentsURL
            .appendingPathComponent("apod_images")
            .appendingPathComponent("apod_\(date).jpg")
        
        // Load image data
        return try? Data(contentsOf: imagePath)
    }
    
    /**
     * Calculates the next update time for the widget.
     * Updates at 6:30 AM UTC (30 minutes after APOD sync at 6:00 AM).
     */
    private func getNextUpdateDate() -> Date {
        let calendar = Calendar(identifier: .gregorian)
        let now = Date()
        
        // Get current UTC time components
        var components = calendar.dateComponents(in: TimeZone(identifier: "UTC")!, from: now)
        
        // If it's past 6:30 AM UTC today, schedule for tomorrow
        if let hour = components.hour, let minute = components.minute {
            if hour > 6 || (hour == 6 && minute >= 30) {
                // Add one day
                components.day! += 1
            }
        }
        
        // Set to 6:30 AM UTC
        components.hour = 6
        components.minute = 30
        components.second = 0
        
        return calendar.date(from: components) ?? Date().addingTimeInterval(3600) // Fallback: 1 hour from now
    }
}
