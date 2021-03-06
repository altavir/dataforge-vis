package space.kscience.visionforge.solid.transform

import space.kscience.dataforge.meta.itemSequence
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.asName
import space.kscience.visionforge.MutableVisionGroup
import space.kscience.visionforge.Vision
import space.kscience.visionforge.VisionGroup
import space.kscience.visionforge.meta
import space.kscience.visionforge.solid.*

private operator fun Number.plus(other: Number) = toFloat() + other.toFloat()
private operator fun Number.times(other: Number) = toFloat() * other.toFloat()

@DFExperimental
internal fun Vision.updateFrom(other: Vision): Vision {
    if (this is Solid && other is Solid) {
        x += other.x
        y += other.y
        z += other.y
        rotationX += other.rotationX
        rotationY += other.rotationY
        rotationZ += other.rotationZ
        scaleX *= other.scaleX
        scaleY *= other.scaleY
        scaleZ *= other.scaleZ
        other.meta.itemSequence().forEach { (name, item) ->
            if (getProperty(name) == null) {
                setProperty(name, item)
            }
        }
    }
    return this
}


@DFExperimental
internal object RemoveSingleChild : VisualTreeTransform<SolidGroup>() {

    override fun SolidGroup.transformInPlace() {
        fun MutableVisionGroup.replaceChildren() {
            children.forEach { (childName, parent) ->
                if (parent is SolidReferenceGroup) return@forEach //ignore refs
                if (parent is MutableVisionGroup) {
                    parent.replaceChildren()
                }
                if (parent is VisionGroup && parent.children.size == 1) {
                    val child = parent.children.values.first()
                    val newParent = child.updateFrom(parent)
                    newParent.parent = null
                    set(childName.asName(), newParent)
                }
            }
        }

        replaceChildren()
        prototypes {
            replaceChildren()
        }
    }

    override fun SolidGroup.clone(): SolidGroup {
        TODO()
    }
}