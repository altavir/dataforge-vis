package hep.dataforge.vis.spatial.fx

import eu.mihosoft.jcsg.PropertyStorage
import eu.mihosoft.jcsg.ext.quickhull3d.HullUtil
import eu.mihosoft.vvecmath.Vector3d
import hep.dataforge.vis.spatial.Convex
import javafx.scene.Node
import kotlin.reflect.KClass


object FXConvexFactory : FX3DFactory<Convex> {
    override val type: KClass<in Convex> get() = Convex::class

    override fun invoke(obj: Convex, binding: DisplayObjectFXBinding): Node {
        val hull = HullUtil.hull(obj.points.map { Vector3d.xyz(it.x, it.y, it.z) }, PropertyStorage())
        return hull.toNode()
    }

}