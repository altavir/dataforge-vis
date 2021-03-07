package space.kscience.visionforge

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.misc.DFExperimental

public actual object VisionForge

@DFExperimental
private val visionForgeContext = Context("VisionForge")

@DFExperimental
public actual val VisionForge.context: Context get() = visionForgeContext

@DFExperimental
public operator fun VisionForge.invoke(vararg modules: PluginFactory<out VisionPlugin>, block: VisionForge.() -> Unit): Unit {
    modules.forEach {
        plugins.fetch(it)
    }
    run(block)
}