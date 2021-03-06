package space.kscience.visionforge.solid

import space.kscience.dataforge.meta.*
import space.kscience.dataforge.meta.descriptors.NodeDescriptor
import space.kscience.dataforge.meta.descriptors.attributes
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.values.ValueType
import space.kscience.dataforge.values.asValue
import space.kscience.visionforge.*
import space.kscience.visionforge.solid.SolidMaterial.Companion.MATERIAL_COLOR_KEY
import space.kscience.visionforge.solid.SolidMaterial.Companion.MATERIAL_KEY
import space.kscience.visionforge.solid.SolidMaterial.Companion.MATERIAL_OPACITY_KEY

@VisionBuilder
public class SolidMaterial : Scheme() {

    /**
     * Primary web-color for the material
     */
    public val color: ColorAccessor = ColorAccessor(this, COLOR_KEY)

    /**
     * Specular color for phong material
     */
    public val specularColor: ColorAccessor = ColorAccessor(this, SPECULAR_COLOR_KEY)

    /**
     * Opacity
     */
    public var opacity: Float by float(1f, key = OPACITY_KEY)

    /**
     * Replace material by wire frame
     */
    public var wireframe: Boolean by boolean(false, WIREFRAME_KEY)

    public companion object : SchemeSpec<SolidMaterial>(::SolidMaterial) {

        public val MATERIAL_KEY: Name = "material".asName()
        public val COLOR_KEY: Name = "color".asName()
        public val MATERIAL_COLOR_KEY: Name = MATERIAL_KEY + COLOR_KEY
        public val SPECULAR_COLOR_KEY: Name = "specularColor".asName()
        public val MATERIAL_SPECULAR_COLOR_KEY: Name = MATERIAL_KEY + SPECULAR_COLOR_KEY
        public val OPACITY_KEY: Name = "opacity".asName()
        public val MATERIAL_OPACITY_KEY: Name = MATERIAL_KEY + OPACITY_KEY
        public val WIREFRAME_KEY: Name = "wireframe".asName()
        public val MATERIAL_WIREFRAME_KEY: Name = MATERIAL_KEY + WIREFRAME_KEY

        public override val descriptor: NodeDescriptor by lazy {
            //must be lazy to avoid initialization bug
            NodeDescriptor {
                inherited = true
                usesStyles = true

                value(COLOR_KEY) {
                    inherited = true
                    usesStyles = true
                    type(ValueType.STRING, ValueType.NUMBER)
                    widgetType = "color"
                }

                value(SPECULAR_COLOR_KEY) {
                    inherited = true
                    usesStyles = true
                    type(ValueType.STRING, ValueType.NUMBER)
                    widgetType = "color"
                    hide()
                }

                value(OPACITY_KEY) {
                    inherited = true
                    usesStyles = true
                    type(ValueType.NUMBER)
                    default(1.0)
                    attributes {
                        this["min"] = 0.0
                        this["max"] = 1.0
                        this["step"] = 0.1
                    }
                    widgetType = "slider"
                }
                value(WIREFRAME_KEY) {
                    inherited = true
                    usesStyles = true
                    type(ValueType.BOOLEAN)
                    default(false)
                }
            }
        }
    }
}

public val Solid.color: ColorAccessor
    get() = ColorAccessor(
        allProperties(inherit = true),
        MATERIAL_COLOR_KEY
    )

public var Solid.material: SolidMaterial?
    get() = getProperty(MATERIAL_KEY, inherit = true).node?.let { SolidMaterial.read(it) }
    set(value) = setProperty(MATERIAL_KEY, value?.rootNode)

@VisionBuilder
public fun Solid.material(builder: SolidMaterial.() -> Unit) {
    ownProperties.getChild(MATERIAL_KEY).update(SolidMaterial, builder)
}

public var Solid.opacity: Number?
    get() = getProperty(MATERIAL_OPACITY_KEY, inherit = true).number
    set(value) {
        setProperty(MATERIAL_OPACITY_KEY, value?.asValue())
    }