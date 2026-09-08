package oblivion.v2.core.guard.detector

class EmergencyDetector(
    expectedHash: String,
    salt: String,
    expectedLength: Int = 0,
) : PinBufferDetector(
    expectedHash = expectedHash,
    salt = salt,
    expectedLength = expectedLength,
    name = "Emergency",
    resetOnWindowChange = true,
)
