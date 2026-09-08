package oblivion.v2.core.guard

data class GuardConfig(
    val masterEnabled: Boolean = false,

    val typeAEnabled: Boolean = false,
    val typeAHash: String = "",
    val typeASalt: String = "",

    val typeALength: Int = 0,

    val typeBEnabled: Boolean = false,
    val typeBLength: Int = 0,

    val emergencyEnabled: Boolean = false,
    val emergencyHash: String = "",
    val emergencySalt: String = "",

    val emergencyLength: Int = 0,

    val failedAttemptsEnabled: Boolean = false,
    val failedAttemptsThreshold: Int = 10,
) {
    companion object {
        const val MIN_SECRET_LENGTH: Int = 4

        const val MIN_TRAP_LENGTH: Int = 4

        const val MIN_FAILED_ATTEMPTS: Int = 3
    }

    fun isValid(): Boolean {
        if (typeAEnabled && (typeAHash.isEmpty() || typeASalt.isEmpty())) return false
        if (typeBEnabled && typeBLength < MIN_TRAP_LENGTH) return false
        if (emergencyEnabled && (emergencyHash.isEmpty() || emergencySalt.isEmpty())) return false
        if (failedAttemptsEnabled && failedAttemptsThreshold < MIN_FAILED_ATTEMPTS) return false
        return true
    }

    fun hasAnyDetectorEnabled(): Boolean =
        typeAEnabled || typeBEnabled || emergencyEnabled || failedAttemptsEnabled
}
