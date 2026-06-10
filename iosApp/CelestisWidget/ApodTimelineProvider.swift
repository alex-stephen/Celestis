import WidgetKit
import SwiftUI
import Foundation

/**
 * Timeline provider for the Celestis APOD widget.
 *
 * ## How automatic updates work (no app launch required)
 *
 * 1. **BGAppRefreshTask** – iOS calls `handleAppRefresh()` in iOSApp.swift daily.
 *    The Kotlin `IosSyncManager` fetches the latest APOD, writes it to the shared
 *    App Group UserDefaults/file container, then Swift calls
 *    `WidgetCenter.shared.reloadAllTimelines()`.  WidgetKit immediately calls
 *    `getTimeline()`  on this provider, which reads the freshly written data.
 *
 * 2. **Timeline policy** – `getTimeline()` schedules the next WidgetKit refresh
 *    for 6:30 AM UTC.  If `BGAppRefreshTask` somehow didn't run, WidgetKit will
 *    call `getTimeline()` at 6:30 AM UTC anyway.  At that point this provider
 *    fetches fresh data directly from the network as a fallback.
 *
 * 3. **Network fallback** – If the App Group has no data *or* the cached APOD
 *    is stale (not today's), this provider makes a direct URLSession call to
 *    the Celestis API and caches the result into the App Group.  This makes
 *    the widget self-sufficient: it works even before the user opens the app.
 *
 * ## Data sharing between the app and the widget extension
 *
 * Both the main app target and the CelestisWidget extension must have the
 * "group.com.alexstephen.celestis80085" App Group capability enabled in their Xcode
 * entitlements.  The IosSyncManager (Kotlin) writes to this group after every
 * successful sync; this provider reads from it.
 *
 * Keys written by IosSyncManager / this provider:
 *   - apod_title          – APOD title string
 *   - apod_date           – APOD date "YYYY-MM-DD"
 *   - apod_explanation    – APOD explanation text
 *   - apod_api_base_url   – Backend base URL (set once on first app launch)
 *
 * Image path: `<AppGroupContainer>/apod_images/apod_<date>.jpg`
 */
struct ApodTimelineProvider: TimelineProvider {

    // MARK: - Constants

    private let appGroupID = "group.com.alexstephen.celestis80085"

    // MARK: - TimelineProvider

    /// Synchronous placeholder shown while WidgetKit renders for the first time.
    func placeholder(in context: Context) -> ApodWidgetEntry {
        ApodWidgetEntry(
            date: Date(),
            title: "Astronomy Picture of the Day",
            apodDate: utcTodayString(),
            imageData: nil,
            explanation: "Loading today's cosmic wonder…"
        )
    }

    /// Fast snapshot used in the widget gallery / preview sheet.
    func getSnapshot(in context: Context, completion: @escaping (ApodWidgetEntry) -> Void) {
        if context.isPreview {
            completion(placeholder(in: context))
            return
        }
        Task {
            let entry = await buildEntry()
            completion(entry)
        }
    }

    /// Called by WidgetKit whenever the timeline expires or after
    /// `WidgetCenter.shared.reloadAllTimelines()` is invoked.
    func getTimeline(in context: Context, completion: @escaping (Timeline<ApodWidgetEntry>) -> Void) {
        Task {
            let entry = await buildEntry()
            let timeline = Timeline(
                entries: [entry],
                policy: .after(nextRefreshDate())
            )
            completion(timeline)
        }
    }

    // MARK: - Entry building

    /// Returns the best available entry:
    ///   1. Fresh cached data from the App Group (has today's image).
    ///   2. Network-fetched data (written back to App Group for next time).
    ///   3. Stale cached data (better than nothing).
    ///   4. Placeholder.
    private func buildEntry() async -> ApodWidgetEntry {
        // 1. Try App Group cache first – fastest and offline-capable.
        if let cached = loadFromAppGroup(), isFreshEntry(cached) {
            return cached
        }

        // 2. Fetch from network when cache is missing or stale.
        if let fetched = await fetchFromNetwork() {
            persistToAppGroup(fetched)   // update cache for next time
            return fetched
        }

        // 3. Return stale cache rather than an empty placeholder.
        if let stale = loadFromAppGroup() {
            return stale
        }

        // 4. Nothing available yet.
        return placeholder(in: .init())
    }

    // MARK: - App Group cache

    private func loadFromAppGroup() -> ApodWidgetEntry? {
        guard
            let defaults = UserDefaults(suiteName: appGroupID),
            let apodDate = defaults.string(forKey: "apod_date"),
            !apodDate.isEmpty
        else { return nil }

        let title = defaults.string(forKey: "apod_title") ?? "Astronomy Picture of the Day"
        let explanation = defaults.string(forKey: "apod_explanation")
        let imageData = loadImageFromAppGroup(date: apodDate)

        return ApodWidgetEntry(
            date: Date(),
            title: title,
            apodDate: apodDate,
            imageData: imageData,
            explanation: explanation
        )
    }

    private func loadImageFromAppGroup(date: String) -> Data? {
        guard
            let containerURL = FileManager.default.containerURL(
                forSecurityApplicationGroupIdentifier: appGroupID)
        else { return nil }

        let imageURL = containerURL
            .appendingPathComponent("apod_images")
            .appendingPathComponent("apod_\(date).jpg")

        return try? Data(contentsOf: imageURL)
    }

    /// Persists a network-fetched entry into the App Group so future timeline
    /// reloads don't need another network round-trip.
    private func persistToAppGroup(_ entry: ApodWidgetEntry) {
        guard let defaults = UserDefaults(suiteName: appGroupID) else { return }
        defaults.set(entry.title, forKey: "apod_title")
        defaults.set(entry.apodDate, forKey: "apod_date")
        if let explanation = entry.explanation {
            defaults.set(explanation, forKey: "apod_explanation")
        }
        defaults.synchronize()

        // Save image file.
        if
            let imageData = entry.imageData,
            let containerURL = FileManager.default.containerURL(
                forSecurityApplicationGroupIdentifier: appGroupID)
        {
            let imagesDir = containerURL.appendingPathComponent("apod_images")
            try? FileManager.default.createDirectory(
                at: imagesDir, withIntermediateDirectories: true)
            let imagePath = imagesDir.appendingPathComponent("apod_\(entry.apodDate).jpg")
            try? imageData.write(to: imagePath)
        }
    }

    /// An entry is "fresh" if it carries today's date AND has image data.
    private func isFreshEntry(_ entry: ApodWidgetEntry) -> Bool {
        entry.apodDate == utcTodayString() && entry.imageData != nil
    }

    // MARK: - Network fetch

    private func fetchFromNetwork() async -> ApodWidgetEntry? {
        // The API base URL is written by the main app (from BuildKonfig) into the
        // shared App Group on first launch. Never hardcode it here.
        guard
            let baseURL = UserDefaults(suiteName: appGroupID)?
                .string(forKey: "apod_api_base_url"),
            let url = URL(string: baseURL)
        else { return nil }

        do {
            let (data, response) = try await URLSession.shared.data(from: url)

            guard
                let http = response as? HTTPURLResponse,
                http.statusCode == 200
            else { return nil }

            let apod = try JSONDecoder().decode(ApodNetworkResponse.self, from: data)

            // Determine which URL to use for the image.
            let isVideo = apod.mediaType?.lowercased() == "video"
            let imageURLString = isVideo ? (apod.thumbnailUrl ?? apod.url) : apod.url
            let imageData = await downloadImageData(from: imageURLString)

            return ApodWidgetEntry(
                date: Date(),
                title: apod.title ?? "Astronomy Picture of the Day",
                apodDate: apod.date,
                imageData: imageData,
                explanation: apod.explanation
            )
        } catch {
            return nil
        }
    }

    private func downloadImageData(from urlString: String?) async -> Data? {
        guard
            let urlString = urlString,
            let url = URL(string: urlString)
        else { return nil }

        return try? await URLSession.shared.data(from: url).0
    }

    // MARK: - Timeline scheduling

    /// Schedules the next widget refresh for 6:30 AM UTC – 30 minutes after
    /// NASA publishes the daily APOD and the BGAppRefreshTask should have run.
    private func nextRefreshDate() -> Date {
        var calendar = Calendar(identifier: .gregorian)
        guard let utc = TimeZone(identifier: "UTC") else {
            return Date().addingTimeInterval(6 * 3600)
        }
        calendar.timeZone = utc

        let now = Date()
        var components = calendar.dateComponents(
            [.year, .month, .day, .hour, .minute, .second], from: now)

        let hour = components.hour ?? 0
        let minute = components.minute ?? 0

        // If it is already past 06:30 UTC, target tomorrow.
        if hour > 6 || (hour == 6 && minute >= 30) {
            components.day = (components.day ?? 0) + 1
        }
        components.hour = 6
        components.minute = 30
        components.second = 0
        components.nanosecond = 0

        return calendar.date(from: components) ?? Date().addingTimeInterval(6 * 3600)
    }

    private func utcTodayString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = TimeZone(identifier: "UTC")
        return formatter.string(from: Date())
    }
}

// MARK: - Network response model

/// Mirrors the JSON shape returned by the Celestis backend (camelCase keys).
private struct ApodNetworkResponse: Decodable {
    let date: String
    let title: String?
    let explanation: String?
    let url: String?
    let mediaType: String?
    let thumbnailUrl: String?
}
