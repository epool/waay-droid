import SwiftUI

/// The full-bleed backdrop behind every game phase (spec 002 FR-019): the app icon's deep indigo into
/// the accent violet. White text on it is at least 10:1 (FR-025).
struct Backdrop: View {
    static let top = Color(red: 0x23 / 255, green: 0x14 / 255, blue: 0x3F / 255)
    static let bottom = Color(red: 0x45 / 255, green: 0x27 / 255, blue: 0xA0 / 255)

    var body: some View {
        LinearGradient(colors: [Self.top, Self.bottom], startPoint: .top, endPoint: .bottom)
            .ignoresSafeArea()
            .accessibilityHidden(true)
    }
}
