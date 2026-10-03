#!/usr/bin/env swift
// Renders the iOS app icon from the same geometry as the Android adaptive icon
// (androidApp/src/main/res/drawable/ic_launcher_foreground.xml), so both platforms share one design.
// Usage: scripts/render-ios-app-icon.swift   (writes into iosApp/iosApp/Assets.xcassets/AppIcon.appiconset)

import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

let size = 1024
// Android's 108-unit viewport; the iOS square shows units 12…96, leaving room around the card fan.
let visibleOrigin: CGFloat = 12
let visibleSpan: CGFloat = 84

func color(_ hex: UInt32, alpha: CGFloat = 1) -> CGColor {
    CGColor(
        srgbRed: CGFloat((hex >> 16) & 0xFF) / 255,
        green: CGFloat((hex >> 8) & 0xFF) / 255,
        blue: CGFloat(hex & 0xFF) / 255,
        alpha: alpha
    )
}

let card = CGPath(
    roundedRect: CGRect(x: 41, y: 36, width: 26, height: 36), cornerWidth: 3, cornerHeight: 3, transform: nil)
let star: CGPath = {
    let path = CGMutablePath()
    let points: [(CGFloat, CGFloat)] = [
        (54, 43), (56.6, 51.4), (65, 54), (56.6, 56.6), (54, 65), (51.4, 56.6), (43, 54), (51.4, 51.4),
    ]
    path.addLines(between: points.map { CGPoint(x: $0.0, y: $0.1) })
    path.closeSubpath()
    return path
}()

enum Variant {
    case light, dark, tinted
}

func render(_ variant: Variant, to url: URL) throws {
    let opaque = variant == .light
    let context = CGContext(
        data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0,
        space: CGColorSpace(name: CGColorSpace.sRGB)!,
        bitmapInfo: (opaque ? CGImageAlphaInfo.noneSkipLast : CGImageAlphaInfo.premultipliedLast).rawValue
    )!
    // Android coordinates: y grows downwards, rotation is clockwise for positive angles.
    let scale = CGFloat(size) / visibleSpan
    context.translateBy(x: 0, y: CGFloat(size))
    context.scaleBy(x: scale, y: -scale)
    context.translateBy(x: -visibleOrigin, y: -visibleOrigin)

    if opaque {
        context.setFillColor(color(0x23143F))
        context.fill(CGRect(x: 0, y: 0, width: 108, height: 108))
    }

    func fan(_ angle: CGFloat, _ fill: CGColor) {
        context.saveGState()
        context.translateBy(x: 54, y: 84)
        context.rotate(by: angle * .pi / 180)
        context.translateBy(x: -54, y: -84)
        context.addPath(card)
        context.setFillColor(fill)
        context.fillPath()
        context.restoreGState()
    }

    switch variant {
    case .light, .dark:
        fan(-22, color(0xC62828))
        fan(22, color(0xEF6C00))
        context.addPath(card)
        context.setFillColor(color(0xFFF3E0))
        context.fillPath()
        context.addPath(star)
        context.setFillColor(color(0xE64A19))
        context.fillPath()
    case .tinted:
        // Grayscale for the system tint, like Android's monochrome layer: the cards with the star cut out.
        fan(-22, color(0x8A8A8A))
        fan(22, color(0xB0B0B0))
        context.addPath(card)
        context.addPath(star)
        context.setFillColor(color(0xFFFFFF))
        context.fillPath(using: .evenOdd)
    }

    let image = context.makeImage()!
    let destination = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(destination, image, nil)
    guard CGImageDestinationFinalize(destination) else { throw CocoaError(.fileWriteUnknown) }
}

let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
let iconSet = root.appendingPathComponent("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset")
try FileManager.default.createDirectory(at: iconSet, withIntermediateDirectories: true)
try render(.light, to: iconSet.appendingPathComponent("AppIcon.png"))
try render(.dark, to: iconSet.appendingPathComponent("AppIcon-dark.png"))
try render(.tinted, to: iconSet.appendingPathComponent("AppIcon-tinted.png"))
print("Wrote \(iconSet.path)")
