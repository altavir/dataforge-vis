package space.kscience.visionforge.solid.three

import info.laht.threekt.core.BufferGeometry
import info.laht.threekt.geometries.CylinderGeometry
import space.kscience.visionforge.solid.ConeSegment
import space.kscience.visionforge.solid.detail
import kotlin.math.PI
import kotlin.math.pow

public object ThreeConeFactory : MeshThreeFactory<ConeSegment>(ConeSegment::class) {
    override fun buildGeometry(obj: ConeSegment): BufferGeometry {
        val cylinder =  obj.detail?.let {
            val segments = it.toDouble().pow(0.5).toInt()
            CylinderGeometry(
                radiusTop = obj.topRadius,
                radiusBottom = obj.bottomRadius,
                height = obj.height,
                radialSegments = segments,
                heightSegments = segments,
                openEnded = false,
                thetaStart = obj.startAngle,
                thetaLength = obj.angle
            )
        } ?: CylinderGeometry(
            radiusTop = obj.topRadius,
            radiusBottom = obj.bottomRadius,
            height = obj.height,
            openEnded = false,
            thetaStart = obj.startAngle,
            thetaLength = obj.angle
        )
        return cylinder.rotateX(PI/2)
    }
}