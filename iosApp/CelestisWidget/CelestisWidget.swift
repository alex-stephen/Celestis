import WidgetKit
import SwiftUI

/**
 * Main APOD Widget for iOS using WidgetKit.
 * 
 * This widget displays the latest Astronomy Picture of the Day on the iOS home screen.
 * It integrates with the shared Kotlin Multiplatform codebase to access cached APOD data.
 * 
 * Features:
 * - Multiple widget sizes (small, medium, large)
 * - Reads cached APOD from shared SQLDelight database
 * - Displays pre-downloaded images from shared storage
 * - Timeline updates based on APOD sync schedule
 * 
 * Integration with Shared Code:
 * - Uses KoinHelper to access shared ApodRepository
 * - Reads from SQLDelight database via Kotlin code
 * - Accesses pre-downloaded images from app's file storage
 */
struct CelestisWidget: Widget {
    let kind: String = "CelestisWidget"
    
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ApodTimelineProvider()) { entry in
            CelestisWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("APOD Widget")
        .description("View the latest Astronomy Picture of the Day")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

/**
 * SwiftUI view for rendering the widget content.
 * Adapts layout based on widget family (small, medium, large).
 */
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

/**
 * Small widget view - Image only
 */
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
                // Fallback when no image is available
                Color.black
                VStack {
                    Text("🌌")
                        .font(.system(size: 40))
                    Text("No APOD")
                        .font(.caption)
                        .foregroundColor(.white)
                }
            }
        }
        .widgetURL(URL(string: "celestis://apod/\(entry.date)"))
    }
}

/**
 * Medium widget view - Image with title
 */
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
                Color.black
            }
            
            // Title overlay
            LinearGradient(
                gradient: Gradient(colors: [.clear, .black.opacity(0.8)]),
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 100)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(entry.title)
                    .font(.headline)
                    .foregroundColor(.white)
                    .lineLimit(2)
                
                Text(entry.formattedDate)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.9))
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .widgetURL(URL(string: "celestis://apod/\(entry.date)"))
    }
}

/**
 * Large widget view - Image with title and explanation preview
 */
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
                Color.black
            }
            
            // Gradient overlay
            LinearGradient(
                gradient: Gradient(colors: [.clear, .black.opacity(0.9)]),
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 150)
            
            VStack(alignment: .leading, spacing: 8) {
                Text(entry.title)
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .lineLimit(2)
                
                Text(entry.formattedDate)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.9))
                
                if let explanation = entry.explanation {
                    Text(explanation)
                        .font(.caption2)
                        .foregroundColor(.white.opacity(0.8))
                        .lineLimit(3)
                        .padding(.top, 4)
                }
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .widgetURL(URL(string: "celestis://apod/\(entry.date)"))
    }
}

/**
 * Preview provider for Xcode previews
 */
struct CelestisWidget_Previews: PreviewProvider {
    static var previews: some View {
        let sampleEntry = ApodWidgetEntry(
            date: Date(),
            title: "Pillars of Creation",
            apodDate: "2026-03-29",
            imageData: nil,
            explanation: "The famous Pillars of Creation in the Eagle Nebula captured by the James Webb Space Telescope."
        )
        
        CelestisWidgetEntryView(entry: sampleEntry)
            .previewContext(WidgetPreviewContext(family: .systemMedium))
    }
}
