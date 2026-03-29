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
}
