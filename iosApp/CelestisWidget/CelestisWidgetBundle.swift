import WidgetKit
import SwiftUI

/**
 * Widget bundle for grouping multiple widgets together.
 * 
 * Currently includes:
 * - CelestisWidget: Main APOD widget
 * 
 * Future widgets can be added here (e.g., multi-APOD gallery, favorites widget).
 */
@main
struct CelestisWidgetBundle: WidgetBundle {
    var body: some Widget {
        CelestisWidget()
    }
}
