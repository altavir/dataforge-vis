package space.kscience.visionforge.solid.three

import info.laht.threekt.materials.LineBasicMaterial
import info.laht.threekt.materials.Material
import info.laht.threekt.materials.MeshBasicMaterial
import info.laht.threekt.materials.MeshPhongMaterial
import info.laht.threekt.math.Color
import info.laht.threekt.objects.Mesh
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.values.ValueType
import space.kscience.dataforge.values.int
import space.kscience.dataforge.values.string
import space.kscience.visionforge.Colors
import space.kscience.visionforge.Vision
import space.kscience.visionforge.ownProperties
import space.kscience.visionforge.solid.SolidMaterial


public object ThreeMaterials {
    public val DEFAULT_COLOR: Color = Color(Colors.darkgreen)
    public val DEFAULT: MeshBasicMaterial = MeshBasicMaterial().apply {
        color.set(DEFAULT_COLOR)
        cached = true
    }
    public val DEFAULT_LINE_COLOR: Color = Color(Colors.black)
    public val DEFAULT_LINE: LineBasicMaterial = LineBasicMaterial().apply {
        color.set(DEFAULT_LINE_COLOR)
    }

    public val SELECTED_MATERIAL: LineBasicMaterial = LineBasicMaterial().apply {
        color.set(Colors.ivory)
        linewidth = 8.0
    }

    public val HIGHLIGHT_MATERIAL: LineBasicMaterial = LineBasicMaterial().apply {
        color.set(Colors.blue)
        linewidth = 8.0
    }

    private val lineMaterialCache = HashMap<Meta, LineBasicMaterial>()

    private fun buildLineMaterial(meta: Meta): LineBasicMaterial = LineBasicMaterial().apply {
        color = meta[SolidMaterial.COLOR_KEY]?.getColor() ?: DEFAULT_LINE_COLOR
        opacity = meta[SolidMaterial.OPACITY_KEY].double ?: 1.0
        transparent = opacity < 1.0
        linewidth = meta["thickness"].double ?: 1.0
    }

    public fun getLineMaterial(meta: Meta?, cache: Boolean): LineBasicMaterial {
        if (meta == null) return DEFAULT_LINE
        return if (cache) {
            lineMaterialCache.getOrPut(meta) { buildLineMaterial(meta) }
        } else {
            buildLineMaterial(meta)
        }
    }

    private val materialCache = HashMap<Meta, Material>()

    internal fun buildMaterial(meta: Meta): Material {
        return meta[SolidMaterial.SPECULAR_COLOR_KEY]?.let { specularColor ->
            MeshPhongMaterial().apply {
                color = meta[SolidMaterial.COLOR_KEY]?.getColor() ?: DEFAULT_COLOR
                specular = specularColor.getColor()
                emissive = specular
                reflectivity = 0.5
                refractionRatio = 1.0
                shininess = 100.0
                opacity = meta[SolidMaterial.OPACITY_KEY]?.double ?: 1.0
                transparent = opacity < 1.0
                wireframe = meta[SolidMaterial.WIREFRAME_KEY].boolean ?: false
                needsUpdate = true
            }
        } ?: MeshBasicMaterial().apply {
            color = meta[SolidMaterial.COLOR_KEY]?.getColor() ?: DEFAULT_COLOR
            opacity = meta[SolidMaterial.OPACITY_KEY]?.double ?: 1.0
            transparent = opacity < 1.0
            wireframe = meta[SolidMaterial.WIREFRAME_KEY].boolean ?: false
            needsUpdate = true
        }

    }

    internal fun cacheMaterial(meta: Meta): Material = materialCache.getOrPut(meta) {
        buildMaterial(meta).apply {
            cached = true
        }
    }

}

/**
 * Infer color based on meta item
 */
public fun MetaItem.getColor(): Color {
    return when (this) {
        is MetaItemValue -> if (this.value.type == ValueType.NUMBER) {
            val int = value.int
            Color(int)
        } else {
            Color(this.value.string)
        }
        is MetaItemNode -> {
            Color(
                node[Colors.RED_KEY]?.int ?: 0,
                node[Colors.GREEN_KEY]?.int ?: 0,
                node[Colors.BLUE_KEY]?.int ?: 0
            )
        }
    }
}

private var Material.cached: Boolean
    get() = userData["cached"] == true
    set(value) {
        userData["cached"] = value
    }

public fun Mesh.updateMaterial(vision: Vision) {
    //val meta = vision.getProperty(SolidMaterial.MATERIAL_KEY, inherit = true).node
    val ownMaterialMeta = vision.ownProperties[SolidMaterial.MATERIAL_KEY]
    val parentMaterialMeta = vision.parent?.getProperty(
        SolidMaterial.MATERIAL_KEY,
        inherit = true,
        includeStyles = false,
        includeDefaults = false
    )

    material = when {
        ownMaterialMeta == null && parentMaterialMeta == null -> {
            //If material is style-based, use cached
            vision.getProperty(
                SolidMaterial.MATERIAL_KEY,
                inherit = false,
                includeStyles = true,
                includeDefaults = false
            ).node?.let {
                ThreeMaterials.cacheMaterial(it)
            } ?: ThreeMaterials.DEFAULT
        }
        else -> {
            vision.getProperty(
                SolidMaterial.MATERIAL_KEY,
                inherit = true
            ).node?.let {
                ThreeMaterials.buildMaterial(it)
            } ?: ThreeMaterials.DEFAULT
        }
    }
}

public fun Mesh.updateMaterialProperty(vision: Vision, propertyName: Name) {
    if (material.cached || propertyName == SolidMaterial.MATERIAL_KEY) {
        //generate a new material since cached material should not be changed
        updateMaterial(vision)
    } else {
        when (propertyName) {
            SolidMaterial.MATERIAL_COLOR_KEY -> {
                material.asDynamic().color = vision.getProperty(
                    SolidMaterial.MATERIAL_COLOR_KEY,
                    inherit = true,
                    includeStyles = true,
                    includeDefaults = false
                )?.getColor() ?: ThreeMaterials.DEFAULT_COLOR
                material.needsUpdate = true
            }
            SolidMaterial.MATERIAL_OPACITY_KEY -> {
                val opacity = vision.getProperty(
                    SolidMaterial.MATERIAL_OPACITY_KEY,
                    inherit = true,
                    includeStyles = true,
                    includeDefaults = false
                ).double ?: 1.0
                material.opacity = opacity
                material.transparent = opacity < 1.0
                material.needsUpdate = true
            }
            SolidMaterial.MATERIAL_WIREFRAME_KEY -> {
                material.asDynamic().wireframe = vision.getProperty(
                    SolidMaterial.MATERIAL_WIREFRAME_KEY,
                    inherit = true,
                    includeStyles = true,
                    includeDefaults = false
                ).boolean ?: false
                material.needsUpdate = true
            }
            else -> console.warn("Unrecognized material property: $propertyName")
        }
    }
}