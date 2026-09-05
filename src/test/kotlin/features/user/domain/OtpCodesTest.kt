package features.user.domain

import com.ducks.features.user.domain.OtpCodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OtpCodesTest {

    private val codes = OtpCodes()

    private val phoneNumber = "375291234567"
    private val otherPhoneNumber = "375291234568"

    @Test
    fun `код состоит из шести цифр`() {
        val code = codes.generate()

        assertEquals(OtpCodes.CODE_LENGTH, code.length)
        assertTrue(code.all { it.isDigit() })
    }

    @Test
    fun `запомненный код подходит`() {
        codes.remember(phoneNumber, "123456")

        assertTrue(codes.verify(phoneNumber, "123456"))
    }

    @Test
    fun `код чужого номера не подходит`() {
        codes.remember(phoneNumber, "123456")

        assertFalse(codes.verify(otherPhoneNumber, "123456"))
    }

    @Test
    fun `код сгорает после успешной проверки`() {
        codes.remember(phoneNumber, "123456")

        assertTrue(codes.verify(phoneNumber, "123456"))
        assertFalse(codes.verify(phoneNumber, "123456"))
    }

    @Test
    fun `новый код вытесняет предыдущий`() {
        codes.remember(phoneNumber, "111111")
        codes.remember(phoneNumber, "222222")

        assertFalse(codes.verify(phoneNumber, "111111"))
        assertTrue(codes.verify(phoneNumber, "222222"))
    }

    @Test
    fun `после пяти промахов код сгорает`() {
        codes.remember(phoneNumber, "123456")

        repeat(5) {
            assertFalse(codes.verify(phoneNumber, "000000"))
        }

        assertFalse(codes.verify(phoneNumber, "123456"))
    }

    @Test
    fun `без запомненного кода проверка не проходит`() {
        assertFalse(codes.verify(phoneNumber, "123456"))
    }
}
