package com.campsitereservationapp.model

import junit.framework.TestCase.assertEquals
import org.junit.Test

class CsvTest {
    private val nl = "\n"
    @Test
    fun onlySingleRowAftertoString() {
        val csv = Csv(listOf("id", "name"))
        csv.addRow(listOf("1", "Lakeside"))

        assertEquals("id,name${nl}1,Lakeside$nl", csv.toString())
    }

    @Test
    fun onlySingleColumnAftertoString() {
        val csv = Csv(listOf("id"))
        csv.addRow(listOf("42"))

        assertEquals("id${nl}42$nl", csv.toString())
    }
    @Test
    fun escapeValueQuotesNewLine() {
        val csv = Csv(listOf("notes"))
        csv.addRow(listOf("line1\nline2"))

        assertEquals("notes$nl\"line1\nline2\"$nl", csv.toString())
    }
    @Test(expected = IllegalArgumentException::class)
    fun addRow_tooManyValues_throws() {
        Csv(listOf("a")).addRow(listOf("1", "2"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun addRow_emptyRow_throwsWhenHeadersNotEmpty() {
        Csv(listOf("a")).addRow(emptyList())
    }
}