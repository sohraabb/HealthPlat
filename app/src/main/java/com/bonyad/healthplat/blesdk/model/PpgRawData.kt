package com.bonyad.healthplat.blesdk.model

import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * PPG Raw Data Parser
 * Based on the BLE characteristic notification format
 */
data class PpgRawData(
    val timestamp: Long,
    val ppgGreen: Int,      // PPG Green LED value
    val ppgInfrared: Int,   // PPG Infrared LED value
    val ppgRed: Int,        // PPG Red LED value
    val accX: Int,          // Accelerometer X
    val accY: Int,          // Accelerometer Y
    val accZ: Int,          // Accelerometer Z
    val index: Int          // Sample index (0-255, loops)
)

object PpgRawDataParser {

    /**
     * Parse PPG raw data from BLE notification (20 bytes)
     *
     * Data format:
     * - Byte 1-2: uint16 ppg_g (Green LED)
     * - Byte 3-4: uint16 ppg_ir (Infrared LED)
     * - Byte 6-7: uint16 ppg_r (Red LED)
     * - Byte 9-14: int16 acc x,y,z (3-axis accelerometer)
     * - Byte 20: index (0-255 loop)
     */
    fun parse(rawData: ByteArray): PpgRawData? {
        if (rawData.size < 20) {
            Timber.e("Invalid PPG data size: ${rawData.size    /**
             * QUICK TEST: Add this to your MainActivity or any screen's onCreate:
             *
             * ```
             * // Test the parser with known data
             * PpgRawDataParser.testWithExampleData()
             * ```
             *
             * Then check Logcat for "PPG Test: PASSED ✓"
             *
             * To test with real device data, call parse() when you receive BLE notifications
             * from characteristic UUID: 2b2e1542-1549-4c7e-bca9-0d498500d191
             */
            }, expected 20 bytes")
            return null
        }

        try {
            val buffer = ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN)

            // Parse PPG values (uint16)
            val ppgGreen = buffer.getShort(0).toInt() and 0xFFFF
            val ppgInfrared = buffer.getShort(2).toInt() and 0xFFFF
            val ppgRed = buffer.getShort(5).toInt() and 0xFFFF

            // Parse accelerometer values (int16, signed)
            val accX = buffer.getShort(8).toInt()
            val accY = buffer.getShort(10).toInt()
            val accZ = buffer.getShort(12).toInt()

            // Parse index (byte 20, index 19 in 0-based array)
            val index = rawData[19].toInt() and 0xFF

            return PpgRawData(
                timestamp = System.currentTimeMillis(),
                ppgGreen = ppgGreen,
                ppgInfrared = ppgInfrared,
                ppgRed = ppgRed,
                accX = accX,
                accY = accY,
                accZ = accZ,
                index = index
            )
        } catch (e: Exception) {
            Timber.e(e, "Error parsing PPG data")
            return null
        }
    }

    /**
     * Test with example from image: 41-01-28-0E-00-09-04-05-AC-FB-18-ED-34-EE-52-71-21-35-00-8D
     * Expected results:
     * - Green: 321 (0x0141)
     * - Infrared: 3624 (0x0E28)
     * - Red: 1033 (0x0409)
     * - AccX: -1108 (0xFBAC)
     * - AccY: -4840 (0xED18)
     * - AccZ: -4556 (0xEE34)
     * - Index: 141 (0x8D)
     */
    fun testWithExampleData(): Boolean {
        val exampleData = byteArrayOf(
            0x41.toByte(), 0x01.toByte(), 0x28.toByte(), 0x0E.toByte(),
            0x00.toByte(), 0x09.toByte(), 0x04.toByte(), 0x05.toByte(),
            0xAC.toByte(), 0xFB.toByte(), 0x18.toByte(), 0xED.toByte(),
            0x34.toByte(), 0xEE.toByte(), 0x52.toByte(), 0x71.toByte(),
            0x21.toByte(), 0x35.toByte(), 0x00.toByte(), 0x8D.toByte()
        )

        val result = parse(exampleData) ?: return false

        val isCorrect = result.ppgGreen == 321 &&
                result.ppgInfrared == 3624 &&
                result.ppgRed == 1033 &&
                result.accX == -1108 &&
                result.accY == -4840 &&
                result.accZ == -4556 &&
                result.index == 141

        Timber.d("PPG Test: ${if (isCorrect) "PASSED ✓" else "FAILED ✗"}")
        Timber.d("Expected: Green=321, IR=3624, Red=1033, X=-1108, Y=-4840, Z=-4556, Idx=141")
        Timber.d("Got: Green=${result.ppgGreen}, IR=${result.ppgInfrared}, Red=${result.ppgRed}, X=${result.accX}, Y=${result.accY}, Z=${result.accZ}, Idx=${result.index}")

        return isCorrect
    }

    /**
     * Validate if PPG data looks reasonable
     * How to confirm correct PPG data:
     * 1. PPG values should be > 0 and < 65535
     * 2. PPG values should change over time (not static)
     * 3. Index should increment sequentially (0->255->0)
     * 4. Accelerometer should respond to movement
     */
    fun isValidPpgData(data: PpgRawData): Boolean {
        // PPG values in valid range
        val ppgInRange = data.ppgGreen in 1..65535 &&
                data.ppgInfrared in 1..65535 &&
                data.ppgRed in 1..65535

        // Accelerometer values reasonable (typically -4000 to 4000)
        val accInRange = data.accX in -8000..8000 &&
                data.accY in -8000..8000 &&
                data.accZ in -8000..8000

        return ppgInRange && accInRange
    }
}