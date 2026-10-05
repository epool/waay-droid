import Testing

@testable import Waay

/// Spec 002 FR-023 / FR-024 / SC-009: Liquid Glass on iOS 26 and later, the system materials before.
struct AnswerControlStyleTests {
    @Test(arguments: [26, 27, 30])
    func liquidGlassFromIOS26(majorVersion: Int) {
        #expect(AnswerControlStyle.for(majorVersion: majorVersion) == .glass)
    }

    @Test(arguments: [17, 18, 25])
    func systemMaterialsBeforeIOS26(majorVersion: Int) {
        #expect(AnswerControlStyle.for(majorVersion: majorVersion) == .materials)
    }
}
