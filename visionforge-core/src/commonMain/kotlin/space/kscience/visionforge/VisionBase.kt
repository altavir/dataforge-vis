package space.kscience.visionforge

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.meta.descriptors.NodeDescriptor
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.values.Null
import space.kscience.dataforge.values.ValueType
import space.kscience.visionforge.Vision.Companion.STYLE_KEY
import kotlin.jvm.Synchronized

internal data class PropertyListener(
    val owner: Any? = null,
    val action: (name: Name) -> Unit,
)

/**
 * A full base implementation for a [Vision]
 * @param properties Object own properties excluding styles and inheritance
 */
@Serializable
@SerialName("vision")
public open class VisionBase(
    @Transient override var parent: VisionGroup? = null,
    protected var properties: Config? = null
) : Vision {

    @Synchronized
    protected fun getOrCreateProperties(): Config {
        if (properties == null) {
            val newProperties = Config()
            properties = newProperties
        }
        return properties!!
    }

    /**
     * A fast accessor method to get own property (no inheritance or styles
     */
    override fun getOwnProperty(name: Name): MetaItem? = if (name == Name.EMPTY) {
        properties?.asMetaItem()
    } else {
        properties?.getItem(name)
    }

    override fun getProperty(
        name: Name,
        inherit: Boolean,
        includeStyles: Boolean,
        includeDefaults: Boolean,
    ): MetaItem? = if (!inherit && !includeStyles && !includeDefaults) {
        getOwnProperty(name)
    } else {
        buildList {
            add(getOwnProperty(name))
            if (includeStyles) {
                addAll(getStyleItems(name))
            }
            if (inherit) {
                add(parent?.getProperty(name, inherit, includeStyles, includeDefaults))
            }
            if (includeDefaults) {
                add(descriptor?.defaultMeta?.get(name))
            }
        }.merge()
    }

    override fun setProperty(name: Name, item: MetaItem?, notify: Boolean) {
        getOrCreateProperties().setItem(name, item)
        if (notify) {
            invalidateProperty(name)
        }
    }

    override val descriptor: NodeDescriptor? get() = null

    private suspend fun updateStyles(names: List<String>) {
        names.mapNotNull { getStyle(it) }.asSequence()
            .flatMap { it.items.asSequence() }
            .distinctBy { it.key }
            .forEach {
                invalidateProperty(it.key.asName())
            }
    }

    //TODO check memory consumption for the flow
    @Transient
    private val propertyInvalidationFlow: MutableSharedFlow<Name> = MutableSharedFlow()

    @DFExperimental
    override val propertyChanges: Flow<Name>
        get() = propertyInvalidationFlow

    override fun invalidateProperty(propertyName: Name) {
        launch {
            if (propertyName == STYLE_KEY) {
                updateStyles(styles)
            }
            propertyInvalidationFlow.emit(propertyName)
        }
    }

    override fun update(change: VisionChange) {
        change.properties?.let {
            updateProperties(Name.EMPTY, it.asMetaItem())
        }
    }

    public companion object {
        public val descriptor: NodeDescriptor = NodeDescriptor {
            value(STYLE_KEY) {
                type(ValueType.STRING)
                multiple = true
            }
        }

        public fun Vision.updateProperties(at: Name, item: MetaItem) {
            when (item) {
                is MetaItemValue -> {
                    if (item.value == Null) {
                        setProperty(at, null)
                    } else
                        setProperty(at, item)
                }
                is MetaItemNode -> item.node.items.forEach { (token, childItem) ->
                    updateProperties(at + token, childItem)
                }
            }
        }

    }
}

//fun VisualObject.findStyle(styleName: Name): Meta? {
//    if (this is VisualGroup) {
//        val style = resolveStyle(styleName)
//        if (style != null) return style
//    }
//    return parent?.findStyle(styleName)
//}