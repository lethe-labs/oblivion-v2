package oblivion.v2.core.guard.detector

private const val DECOY_NAME = "Decoy"

class DecoyDetector(
    expectedHash: String,
    salt: String,
    expectedLength: Int = 0,
) : PinBufferDetector(
    expectedHash = expectedHash,
    salt = salt,
    expectedLength = expectedLength,
    name = DECOY_NAME,
    resetOnWindowChange = false,
) {
    companion object {
        const val NAME: String = DECOY_NAME
    }
}
