package hep.dataforge.vis.spatial.gdml

import hep.dataforge.meta.Meta
import hep.dataforge.vis.spatial.*
import scientifik.gdml.*
import kotlin.math.cos
import kotlin.math.sin


private fun VisualObject3D.withPosition(
    lUnit: LUnit,
    pos: GDMLPosition? = null,
    rotation: GDMLRotation? = null,
    scale: GDMLScale? = null
): VisualObject3D = apply {
    pos?.let {
        this@withPosition.position.x = pos.x(lUnit)
        this@withPosition.position.y = pos.y(lUnit)
        this@withPosition.position.z = pos.z(lUnit)
    }
    rotation?.let {
        this@withPosition.rotation.x = rotation.x()
        this@withPosition.rotation.y = rotation.y()
        this@withPosition.rotation.z = rotation.z()
    }
    scale?.let {
        this@withPosition.scale.x = scale.x.toFloat()
        this@withPosition.scale.y = scale.y.toFloat()
        this@withPosition.scale.z = scale.z.toFloat()
    }
    //TODO convert units if needed
}

private inline operator fun Number.times(d: Double) = toDouble() * d
private inline operator fun Number.times(f: Float) = toFloat() * f

private fun VisualGroup3D.addSolid(
    context: GDMLTransformer,
    solid: GDMLSolid,
    name: String? = null,
    block: VisualObject3D.() -> Unit = {}
): VisualObject3D {
    context.solidAdded(solid)
    val lScale = solid.lscale(context.lUnit)
    val aScale = solid.ascale()
    return when (solid) {
        is GDMLBox -> box(solid.x * lScale, solid.y * lScale, solid.z * lScale, name)
        is GDMLTube -> tube(
            solid.rmax * lScale,
            solid.z * lScale,
            solid.rmin * lScale,
            solid.startphi * aScale,
            solid.deltaphi * aScale,
            name
        )
        is GDMLXtru -> extrude(name) {
            shape {
                solid.vertices.forEach {
                    point(it.x * lScale, it.y * lScale)
                }
            }
            solid.sections.sortedBy { it.zOrder }.forEach { section ->
                layer(
                    section.zPosition * lScale,
                    section.xOffset * lScale,
                    section.yOffset * lScale,
                    section.scalingFactor
                )
            }
        }
        is GDMLScaledSolid -> {
            //Add solid with modified scale
            val innerSolid = solid.solidref.resolve(context.root)
                ?: error("Solid with tag ${solid.solidref.ref} for scaled solid ${solid.name} not defined")

            addSolid(context, innerSolid) {
                block()
                scale.x *= solid.scale.x.toFloat()
                scale.y *= solid.scale.y.toFloat()
                scale.z = solid.scale.z.toFloat()
            }
        }
        is GDMLSphere -> sphere(solid.rmax * lScale, solid.deltaphi * aScale, solid.deltatheta * aScale, name) {
            phiStart = solid.startphi * aScale
            thetaStart = solid.starttheta * aScale
        }
        is GDMLOrb -> sphere(solid.r * lScale, name = name)
        is GDMLPolyhedra -> extrude(name) {
            //getting the radius of first
            require(solid.planes.size > 1) { "The polyhedron geometry requires at least two planes" }
            val baseRadius = solid.planes.first().rmax * lScale
            shape {
                (0..solid.numsides).forEach {
                    val phi = solid.deltaphi * aScale / solid.numsides * it + solid.startphi * aScale
                    (baseRadius * cos(phi) to baseRadius * sin(phi))
                }
            }
            solid.planes.forEach { plane ->
                //scaling all radii relative to first layer radius
                layer(plane.z * lScale, scale = plane.rmax * lScale / baseRadius)
            }
        }
        is GDMLBoolSolid -> {
            val first = solid.first.resolve(context.root) ?: error("")
            val second = solid.second.resolve(context.root) ?: error("")
            val type: CompositeType = when (solid) {
                is GDMLUnion -> CompositeType.UNION
                is GDMLSubtraction -> CompositeType.SUBTRACT
                is GDMLIntersection -> CompositeType.INTERSECT
            }

            return composite(type, name) {
                addSolid(context, first) {
                    withPosition(
                        context.lUnit,
                        solid.resolveFirstPosition(context.root),
                        solid.resolveFirstRotation(context.root),
                        null
                    )
                }
                addSolid(context, second) {
                    withPosition(
                        context.lUnit,
                        solid.resolvePosition(context.root),
                        solid.resolveRotation(context.root),
                        null
                    )
                }
            }
        }
    }.apply(block)
}

private fun VisualGroup3D.addPhysicalVolume(
    context: GDMLTransformer,
    physVolume: GDMLPhysVolume
) {
    val volume: GDMLGroup = physVolume.volumeref.resolve(context.root)
        ?: error("Volume with ref ${physVolume.volumeref.ref} could not be resolved")

    if (context.acceptGroup(volume)) {

        this[physVolume.name] = volume(
            context,
            volume,
            physVolume.resolvePosition(context.root),
            physVolume.resolveRotation(context.root),
            physVolume.resolveScale(context.root)
        )
    }
}

private fun VisualGroup3D.addDivisionVolume(
    context: GDMLTransformer,
    divisionVolume: GDMLDivisionVolume
) {
    val volume: GDMLGroup = divisionVolume.volumeref.resolve(context.root)
        ?: error("Volume with ref ${divisionVolume.volumeref.ref} could not be resolved")

    //TODO add divisions
    add(
        volume(
            context,
            volume
        )
    )
}

private fun VisualGroup3D.addVolume(
    context: GDMLTransformer,
    group: GDMLGroup,
    position: GDMLPosition? = null,
    rotation: GDMLRotation? = null,
    scale: GDMLScale? = null
) {
    this[group.name] = volume(context, group, position, rotation, scale)
}

private fun volume(
    context: GDMLTransformer,
    group: GDMLGroup,
    position: GDMLPosition? = null,
    rotation: GDMLRotation? = null,
    scale: GDMLScale? = null
): VisualGroup3D {

    return VisualGroup3D().apply {
        withPosition(context.lUnit, position, rotation, scale)

        if (group is GDMLVolume) {
            val solid = group.solidref.resolve(context.root)
                ?: error("Solid with tag ${group.solidref.ref} for volume ${group.name} not defined")
            val material = group.materialref.resolve(context.root) ?: GDMLElement(group.materialref.ref)

            if (context.acceptSolid(solid)) {
                val cachedSolid = context.templates[solid.name]
                    ?: context.templates.addSolid(context, solid, solid.name) {
                        this.material = context.resolveColor(group, material, solid)
                    }
                proxy(cachedSolid, solid.name)
            }

            when (val vol = group.placement) {
                is GDMLPhysVolume -> addPhysicalVolume(context, vol)
                is GDMLDivisionVolume -> addDivisionVolume(context, vol)
            }
        }

        group.physVolumes.forEach { physVolume ->
            addPhysicalVolume(context, physVolume)
        }
    }
}

typealias ColorResolver = GDMLGroup?.(GDMLMaterial, GDMLSolid?) -> Meta


fun GDML.toVisual(block: GDMLTransformer.() -> Unit = {}): VisualGroup3D {

    val context = GDMLTransformer(this).apply(block)

    return volume(context, world).also {
        context.finished()
    }
}