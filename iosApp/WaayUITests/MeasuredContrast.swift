import UIKit
import XCTest

/// WCAG contrast of an element as rendered: the dominant fill against the label's extreme colour,
/// sampled from the middle of the element's screenshot (away from glass rims and shadows).
///
/// Xcode's audit misreads Liquid Glass controls both ways: it reported "Contrast failed" for white text
/// on a `#301880` glass fill that measures 13.4:1, and passed white text on near-white glass that
/// measures 1.03:1 (spec 002, T032). For those controls the tests measure the pixels instead.
enum MeasuredContrast {
    static func of(_ element: XCUIElement) -> Double {
        guard let (fill, lums) = sample(element) else { return 0 }
        // The label is whichever extreme is farther from the fill.
        let text = fill > 0.5 ? (lums.min() ?? fill) : (lums.max() ?? fill)
        return (max(fill, text) + 0.05) / (min(fill, text) + 0.05)
    }

    /// Relative luminance of the element's dominant colour: its background, for a text element.
    static func fillLuminance(of element: XCUIElement) -> Double {
        sample(element)?.fill ?? 0
    }

    /// The dominant colour's luminance and every sampled pixel's luminance.
    private static func sample(_ element: XCUIElement) -> (fill: Double, lums: [Double])? {
        guard let image = element.screenshot().image.cgImage else { return nil }
        let width = image.width
        let height = image.height
        var pixels = [UInt8](repeating: 0, count: width * height * 4)
        guard
            let context = CGContext(
                data: &pixels, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width * 4,
                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)
        else { return nil }
        context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))

        // Pixels are grouped into coarse colour bins; the fullest bin is the fill, and its exact colour
        // is the average of the pixels in it.
        var bins: [Int: (count: Int, r: Int, g: Int, b: Int)] = [:]
        var lums: [Double] = []
        for y in stride(from: height / 4, to: height * 3 / 4, by: 2) {
            for x in stride(from: width / 5, to: width * 4 / 5, by: 2) {
                let i = (y * width + x) * 4
                let (r, g, b) = (Int(pixels[i]), Int(pixels[i + 1]), Int(pixels[i + 2]))
                let bin = bins[(r / 8) << 10 | (g / 8) << 5 | (b / 8), default: (0, 0, 0, 0)]
                bins[(r / 8) << 10 | (g / 8) << 5 | (b / 8)] = (bin.count + 1, bin.r + r, bin.g + g, bin.b + b)
                lums.append(luminance(Double(r), Double(g), Double(b)))
            }
        }
        guard let fill = bins.values.max(by: { $0.count < $1.count }) else { return nil }
        let n = Double(fill.count)
        return (luminance(Double(fill.r) / n, Double(fill.g) / n, Double(fill.b) / n), lums)
    }

    private static func luminance(_ r: Double, _ g: Double, _ b: Double) -> Double {
        func channel(_ value: Double) -> Double {
            let c = value / 255
            return c <= 0.03928 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }
}
