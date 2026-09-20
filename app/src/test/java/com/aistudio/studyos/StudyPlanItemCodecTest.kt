package com.aistudio.studyos

import com.aistudio.studyos.data.local.entity.StudyPlanItem
import com.aistudio.studyos.data.local.entity.StudyPlanItemCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudyPlanItemCodecTest {

    @Test
    fun strictDecodeAcceptsValidPlan() {
        val items = listOf(
            StudyPlanItem("Math", "Algebra", 25),
            StudyPlanItem("Physics", "Motion", 10)
        )
        val decoded = StudyPlanItemCodec.decodeStrict(StudyPlanItemCodec.encode(items))
        assertEquals(items, decoded)
    }

    @Test
    fun strictDecodeRejectsPartiallyCorruptedPlan() {
        val valid = StudyPlanItemCodec.encode(
            listOf(StudyPlanItem("Math", "Algebra", 25))
        )
        assertNull(StudyPlanItemCodec.decodeStrict("$valid\ncorrupted"))
    }

    @Test
    fun strictDecodeKeepsBlankLegacyPlansRecoverable() {
        assertEquals(emptyList<StudyPlanItem>(), StudyPlanItemCodec.decodeStrict(""))
    }
}
