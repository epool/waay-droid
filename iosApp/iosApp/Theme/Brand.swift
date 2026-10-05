import SwiftUI

/// Fixed brand colours for fills under white labels (spec 002 FR-025).
///
/// The adaptive accent can't fill a control with a white label. Its dark-mode `#8257F1` leaves white at
/// 4.6:1, and with Increase Contrast the system lightens it further for contrast with dark
/// backgrounds: white labels measured 3.1:1 on iOS 27 and about 1.9:1 on iOS 18.6 (`#C5B0FF`), and the
/// audit failed both. Under prominent glass the same accent measured 4.0:1 even without Increase
/// Contrast. Fixed colours are drawn as given.
enum Brand {
    /// The backdrop's violet, `#4527A0`. White text on it is 10:1.
    static let violet = Color(red: 0x45 / 255, green: 0x27 / 255, blue: 0xA0 / 255)
}
