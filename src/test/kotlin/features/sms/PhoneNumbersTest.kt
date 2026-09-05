package features.sms

import com.ducks.features.sms.PhoneNumbers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneNumbersTest {

    @Test
    fun `международный номер очищается от разделителей`() {
        assertEquals("375291234567", PhoneNumbers.toOperatorFormat("+375 (29) 123-45-67"))
    }

    @Test
    fun `местная запись с восьмёркой переводится в международную`() {
        assertEquals("375291234567", PhoneNumbers.toOperatorFormat("8 029 123-45-67"))
    }

    @Test
    fun `местная запись без восьмёрки переводится в международную`() {
        assertEquals("375291234567", PhoneNumbers.toOperatorFormat("029 123 45 67"))
    }

    @Test
    fun `номер без кода страны считается белорусским`() {
        assertEquals("375291234567", PhoneNumbers.toOperatorFormat("291234567"))
    }

    @Test
    fun `иностранный номер остаётся как есть`() {
        assertEquals("79161234567", PhoneNumbers.toOperatorFormat("+7 916 123-45-67"))
    }

    @Test
    fun `слишком короткий номер отбрасывается`() {
        assertNull(PhoneNumbers.toOperatorFormat("12345"))
    }

    @Test
    fun `строка без цифр отбрасывается`() {
        assertNull(PhoneNumbers.toOperatorFormat("не телефон"))
    }
}
