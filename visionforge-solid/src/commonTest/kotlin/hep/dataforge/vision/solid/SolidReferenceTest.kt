package hep.dataforge.vision.solid

import hep.dataforge.misc.DFExperimental
import hep.dataforge.vision.get
import hep.dataforge.vision.style
import hep.dataforge.vision.useStyle
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals

@DFExperimental
class SolidReferenceTest {
    val groupWithReference = SolidGroup {
        val theStyle by style {
            SolidMaterial.MATERIAL_COLOR_KEY put "red"
        }
        ref("test", Box(100f,100f,100f).apply {
            color("blue")
            useStyle(theStyle)
        })
    }


    @Test
    fun testReferenceProperty(){
        assertEquals("blue", (groupWithReference["test"] as Solid).color.string)
    }

    @Test
    fun testReferenceSerialization(){
        val serialized = Solids.jsonForSolids.encodeToJsonElement(groupWithReference)
        val deserialized = Solids.jsonForSolids.decodeFromJsonElement(SolidGroup.serializer(), serialized)
        assertEquals("blue", (deserialized["test"] as Solid).color.string)
    }
}