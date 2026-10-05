import Testing

@testable import Waay

/// Spec 002 FR-008 / FR-014: the card tilts with the drag, capped at 12°, and never with Reduce Motion.
struct CardMotionTests {
    @Test func tiltFollowsTheDragUpToTheCap() {
        #expect(CardMotion.tilt(offset: 150, width: 300, reduceMotion: false) == 6)
        #expect(CardMotion.tilt(offset: -900, width: 300, reduceMotion: false) == -12)
    }

    @Test func noTiltWithReduceMotion() {
        #expect(CardMotion.tilt(offset: 150, width: 300, reduceMotion: true) == 0)
        #expect(CardMotion.tilt(offset: -900, width: 300, reduceMotion: true) == 0)
    }
}
