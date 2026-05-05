import WidgetKit
import Foundation

/**
 * Timeline entry for the APOD widget.
 * 
 * This struct represents a single point in the widget's timeline,
 * containing all the data needed to render the widget at a specific time.
 * 
 * Properties:
 * - date: When this entry should be displayed (timeline date)
 * - title: APOD title
 * - apodDate: The actual APOD date (YYYY-MM-DD format)
 * - imageData: Pre-downloaded image data (optional)
 * - explanation: APOD explanation text (optional)
 */
struct ApodWidgetEntry: TimelineEntry {
    /// The date when this timeline entry should be displayed
    let date: Date
    
    /// The title of the APOD
    let title: String
    
    /// The APOD date in YYYY-MM-DD format
    let apodDate: String
    
    /// Pre-downloaded image data for the APOD
    let imageData: Data?
    
    /// Explanation text for the APOD
    let explanation: String?
    
    /// Formatted date string for display
    var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, yyyy"
        
        // Try to parse the apodDate string
        let apodFormatter = DateFormatter()
        apodFormatter.dateFormat = "yyyy-MM-dd"
        
        if let parsedDate = apodFormatter.date(from: apodDate) {
            return formatter.string(from: parsedDate)
        }
        
        return apodDate
    }

    var dateMonthAbbreviation: String {
        guard let parsedDate = parsedApodDate else {
            return String(apodDate.prefix(3)).uppercased()
        }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "MMM"
        return formatter.string(from: parsedDate).uppercased()
    }

    var dateDayNumber: String {
        guard let parsedDate = parsedApodDate else {
            let parts = apodDate.split(separator: "-")
            return parts.count == 3 ? String(Int(parts[2]) ?? 0) : ""
        }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "dd"
        return formatter.string(from: parsedDate)
    }

    private var parsedApodDate: Date? {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.date(from: apodDate)
    }
}
