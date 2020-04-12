package hep.dataforge.vis.spatial

import hep.dataforge.names.toName
import hep.dataforge.vis.VisualObject
import hep.dataforge.vis.get
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {
    @Test
    fun testCubeSerialization() {
        val cube = Box(100f, 100f, 100f).apply {
            color(222)
            x = 100
            z = -100
        }
        val string =  cube.stringify()
        println(string)
        val newCube = VisualObject.parseJson(string)
        assertEquals(cube.config, newCube.config)
    }

    @Test
    fun testProxySerialization() {
        val cube = Box(100f, 100f, 100f).apply {
            color(222)
            x = 100
            z = -100
        }
        val group = VisualGroup3D().apply {
            proxy("cube", cube)
            proxyGroup("pg", "pg.content".toName()){
                sphere(50){
                    x = -100
                }
            }
        }
        val string = group.stringify()
        println(string)
        val reconstructed = VisualGroup3D.parseJson(string)
        assertEquals(group["cube"]?.config, reconstructed["cube"]?.config)
    }
}