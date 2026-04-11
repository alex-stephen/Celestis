import WidgetKit
import SwiftUI

/**
 * Main APOD Widget for iOS using WidgetKit.
 *
 * Supports three sizes:
 *   • Small  – full-bleed APOD image (tap opens the detail screen)
 *   • Medium – image with title + date overlay at the bottom
 *   • Large  – image with title, date, and explanation preview
 *
 * Data flow:
 *   The widget reads APOD data that the main app's `IosSyncManager` writes to
 *   a shared App Group after every background sync.  If no cached data is
 *   available, `ApodTimelineProvider` fetches it directly from the network.
 *   See `ApodTimelineProvider.swift` for the full update-without-launch story.
 */
struct CelestisWidget: Widget {
    let kind: String = "CelestisWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ApodTimelineProvider()) { entry in
            CelestisWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("APOD Widget")
        .description("Today's Astronomy Picture of the Day, always up to date.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

// MARK: - Entry View (size dispatcher)

struct CelestisWidgetEntryView: View {
    @Environment(\.widgetFamily) var family
    var entry: ApodWidgetEntry

    var body: some View {
        switch family {
        case .systemSmall:
            SmallWidgetView(entry: entry)
        case .systemMedium:
            MediumWidgetView(entry: entry)
        case .systemLarge:
            LargeWidgetView(entry: entry)
        default:
            MediumWidgetView(entry: entry)
        }
    }
}

// MARK: - Small (image only)

struct SmallWidgetView: View {
    var entry: ApodWidgetEntry

    var body: some View {
        ZStack {
            if let imageData = entry.imageData,
               let uiImage = UIImage(data: imageData) {
                Image(uiImage: uiImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } else {
                // Shown when the image hasn't downloaded yet.
                Color(red: 0.1, green: 0.1, blue: 0.18)
                VStack(spacing: 4) {
                    Text("🌌")
                        .font(.system(size: 36))
                    Text("APOD")
                        .font(.caption2)
                        .fontWeight(.semibold)
                        .foregroundColor(.white.opacity(0.7))
                }
            }
        }
        .clipped()
        .widgetURL(URL(string: "celestis://apod/\(entry.apodDate)"))
    }
}

// MARK: - Medium (image + title + date)

struct MediumWidgetView: View {
    var entry: ApodWidgetEntry

    var body: some View {
        ZStack(alignment: .bottom) {
            // Background image
            if let imageData = entry.imageData,
               let uiImage = UIImage(data: imageData) {
                Image(uiImage: uiImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } else {
                Color(red: 0.1, green: 0.1, blue: 0.18)
            }

            // Gradient scrim
            LinearGradient(
                gradient: Gradient(colors: [.clear, .black.opacity(0.85)]),
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(maxWidth: .infinity)
            .frame(height: 90)

            // Text overlay
            VStack(alignment: .leading, spacing: 3) {
                Text(entry.title)
                    .font(.headline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .lineLimit(2)

                Text(entry.formattedDate)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.85))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .clipped()
        .widgetURL(URL(string: "celestis://apod/\(entry.apodDate)"))
    }
}

// MARK: - Large (image + title + date + explanation)

struct LargeWidgetView: View {
    var entry: ApodWidgetEntry

    var body: some View {
        ZStack(alignment: .bottom) {
            // Background image
            if let imageData = entry.imageData,
               let uiImage = UIImage(data: imageData) {
                Image(uiImage: uiImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } else {
                Color(red: 0.1, green: 0.1, blue: 0.18)
            }

            // Gradient scrim – taller to accommodate the explanation text.
            LinearGradient(
                gradient: Gradient(colors: [.clear, .black.opacity(0.92)]),
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(maxWidth: .infinity)
            .frame(height: 180)

            // Text overlay
            VStack(alignment: .leading, spacing: 6) {
                Text(entry.title)
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .lineLimit(2)

                Text(entry.formattedDate)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.85))

                if let explanation = entry.explanation, !explanation.isEmpty {
                    Text(explanation)
                        .font(.caption2)
                        .foregroundColor(.white.opacity(0.75))
                        .lineLimit(4)
                        .padding(.top, 2)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .clipped()
        .widgetURL(URL(string: "celestis://apod/\(entry.apodDate)"))
    }
}

// MARK: - Previews

#Preview("Small", as: .systemSmall) {
    CelestisWidget()
} timeline: {
    ApodWidgetEntry(
        date: .now,
        title: "Pillars of Creation",
        apodDate: "2026-04-11",
        imageData: nil,
        explanation: "The famous pillars of gas and dust in the Eagle Nebula."
    )
}

#Preview("Medium", as: .systemMedium) {
    CelestisWidget()
} timeline: {
    ApodWidgetEntry(
        date: .now,
        title: "Pillars of Creation",
        apodDate: "2026-04-11",
        imageData: nil,
        explanation: "The famous pillars of gas and dust in the Eagle Nebula."
    )
}

#Preview("Large", as: .systemLarge) {
    CelestisWidget()
} timeline: {
    ApodWidgetEntry(
        date: .now,
        title: "Pillars of Creation",
        apodDate: "2026-04-11",
        imageData: nil,
        explanation: "The famous pillars of gas and dust in the Eagle Nebula."
    )
}