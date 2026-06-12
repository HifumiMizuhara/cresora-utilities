package hifumi.cresora.domain

import net.minecraft.util.math.random.Random
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

class DomainMaterialDropRollingTest {

    @Test
    fun testRandomCountRolling() {
        val random = Random.create(12345L)
        val randomCountMethod = DomainService::class.java.getDeclaredMethod(
            "randomCount",
            Random::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        randomCountMethod.isAccessible = true

        val min = 1
        val max = 3
        val rolledCounts = mutableSetOf<Int>()

        // Roll 1000 times and verify they are all within range [1, 3] and we hit all values
        for (i in 1..1000) {
            val count = randomCountMethod.invoke(DomainService, random, min, max) as Int
            assertTrue(count in min..max, "Rolled count $count is outside of [$min, $max]")
            rolledCounts.add(count)
        }

        assertEquals(setOf(1, 2, 3), rolledCounts, "Expected to roll all values in [1, 3] inclusive")

        // Test boundary when min == max
        val singleCount = randomCountMethod.invoke(DomainService, random, 2, 2) as Int
        assertEquals(2, singleCount)
        
        // Test boundary when max < min (which should return min coerced to at least 0)
        val reversedCount = randomCountMethod.invoke(DomainService, random, 5, 2) as Int
        assertEquals(5, reversedCount)
    }
}
