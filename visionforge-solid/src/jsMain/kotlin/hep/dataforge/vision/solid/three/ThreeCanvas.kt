package hep.dataforge.vision.solid.three

import hep.dataforge.meta.getItem
import hep.dataforge.meta.string
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.names.toName
import hep.dataforge.vision.Colors
import hep.dataforge.vision.layout.Output
import hep.dataforge.vision.solid.Solid
import hep.dataforge.vision.solid.specifications.*
import hep.dataforge.vision.solid.three.ThreeMaterials.HIGHLIGHT_MATERIAL
import hep.dataforge.vision.solid.three.ThreeMaterials.SELECTED_MATERIAL
import info.laht.threekt.WebGLRenderer
import info.laht.threekt.cameras.PerspectiveCamera
import info.laht.threekt.core.BufferGeometry
import info.laht.threekt.core.Object3D
import info.laht.threekt.core.Raycaster
import info.laht.threekt.external.controls.OrbitControls
import info.laht.threekt.external.controls.TrackballControls
import info.laht.threekt.geometries.EdgesGeometry
import info.laht.threekt.helpers.AxesHelper
import info.laht.threekt.materials.LineBasicMaterial
import info.laht.threekt.math.Vector2
import info.laht.threekt.objects.LineSegments
import info.laht.threekt.objects.Mesh
import info.laht.threekt.scenes.Scene
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Node
import org.w3c.dom.events.MouseEvent
import kotlin.math.cos
import kotlin.math.sin

/**
 *
 */
public class ThreeCanvas(
    public val three: ThreePlugin,
    public val options: Canvas3DOptions,
) : Output<Solid> {
    private var root: Object3D? = null

    private val raycaster = Raycaster()
    private val mousePosition: Vector2 = Vector2()

    public var content: Solid? = null
        private set

    public var axes: AxesHelper = AxesHelper(options.axes.size.toInt()).apply { visible = options.axes.visible }
        private set

    private val scene: Scene = Scene().apply {
        add(axes)
    }

    public var camera: PerspectiveCamera = buildCamera(options.camera)
        private set

    private var picked: Object3D? = null

    private val renderer = WebGLRenderer { antialias = true }.apply {
        setClearColor(Colors.skyblue, 1)
    }

    public val canvas: HTMLCanvasElement = renderer.domElement as HTMLCanvasElement

    /**
     * Force camera aspect ration and renderer size recalculation
     */
    public fun updateSize() {
        val width = canvas.clientWidth
        val height = canvas.clientHeight
        renderer.setSize(width, height, false)
        camera.aspect = width.toDouble() / height.toDouble()
        camera.updateProjectionMatrix()
    }

    /**
     * Attach canvas to given [HTMLElement]
     */
    init {
        canvas.addEventListener("pointerdown", {
            val picked = pick()
            options.onSelect?.invoke(picked?.fullName())
        }, false)

        //Attach listener to track mouse changes
        canvas.addEventListener("mousemove", { event ->
            (event as? MouseEvent)?.run {
                val rect = canvas.getBoundingClientRect()
                mousePosition.x = ((event.clientX - rect.left) / canvas.clientWidth) * 2 - 1
                mousePosition.y = -((event.clientY - rect.top) / canvas.clientHeight) * 2 + 1
            }
        }, false)


        canvas.style.apply {
            width = "100%"
            minWidth = "${options.minWith.toInt()}px"
            maxWidth = "${options.maxWith.toInt()}px"
            height = "100%"
            minHeight = "${options.minHeight.toInt()}px"
            maxHeight = "${options.maxHeight.toInt()}px"
            display = "block"
        }


        canvas.onresize = {
            updateSize()
        }

        addControls(canvas, options.controls)

        renderer.setAnimationLoop {
            val picked = pick()

            if (picked != null && this.picked != picked) {
                this.picked?.toggleHighlight(false, HIGHLIGHT_NAME, HIGHLIGHT_MATERIAL)
                picked.toggleHighlight(true, HIGHLIGHT_NAME, HIGHLIGHT_MATERIAL)
                this.picked = picked
            }

            renderer.render(scene, camera)
        }
    }

    public fun attach(element: Element) {
        element.appendChild(canvas)
        updateSize()
    }

    /**
     * Resolve full name of the object relative to the global root
     */
    private fun Object3D.fullName(): Name {
        if (root == null) error("Can't resolve element name without the root")
        return if (parent == root) {
            name.toName()
        } else {
            (parent?.fullName() ?: Name.EMPTY) + name.toName()
        }
    }

    //find first non-static parent in this object ancestry
    private fun Object3D?.upTrace(): Object3D? = if (this?.name?.startsWith("@") == true) parent else this

    private fun pick(): Object3D? {
        // update the picking ray with the camera and mouse position
        raycaster.setFromCamera(mousePosition, camera)

        // calculate objects intersecting the picking ray
        return root?.let { root ->
            val intersects = raycaster.intersectObject(root, true)
            //skip invisible objects
            val obj = intersects.map { it.`object` }.firstOrNull { it.visible }
            obj.upTrace()
        }
    }


    private fun buildCamera(spec: Camera) = PerspectiveCamera(
        spec.fov,
        1.0,
        spec.nearClip,
        spec.farClip
    ).apply {
        translateX(spec.distance * sin(spec.zenith) * sin(spec.azimuth))
        translateY(spec.distance * cos(spec.zenith))
        translateZ(spec.distance * sin(spec.zenith) * cos(spec.azimuth))
    }

    private fun addControls(element: Node, controls: Controls) {
        when (controls.getItem("type").string) {
            "trackball" -> TrackballControls(camera, element)
            else -> OrbitControls(camera, element)
        }
    }

    public fun clear() {
        scene.children.find { it.name == "@root" }?.let {
            scene.remove(it)
        }
    }

    public override fun render(vision: Solid) {
        //clear old root
        clear()

        val object3D = three.buildObject3D(vision)
        object3D.name = "@root"
        scene.add(object3D)
        content = vision
        root = object3D
    }

    private var selected: Object3D? = null

    /**
     * Toggle highlight for the given [Mesh] object
     */
    private fun Object3D.toggleHighlight(
        highlight: Boolean,
        edgesName: String,
        material: LineBasicMaterial = SELECTED_MATERIAL,
    ) {
        if (userData[DO_NOT_HIGHLIGHT_TAG] == true) {
            return
        }
        if (this is Mesh) {
            if (highlight) {
                val edges = LineSegments(
                    EdgesGeometry(geometry as BufferGeometry),
                    material
                ).apply {
                    name = edgesName
                }
                add(edges)
            } else {
                val highlightEdges = children.find { it.name == edgesName }
                highlightEdges?.let { remove(it) }
            }
        } else {
            children.filter { it.name != edgesName }.forEach {
                it.toggleHighlight(highlight, edgesName, material)
            }
        }
    }

    /**
     * Toggle highlight for element with given name
     */
    public fun select(name: Name?) {
        if (name == null) {
            selected?.toggleHighlight(false, SELECT_NAME, SELECTED_MATERIAL)
            selected = null
            return
        }
        val obj = root?.findChild(name)
        if (obj != null && selected != obj) {
            selected?.toggleHighlight(false, SELECT_NAME, SELECTED_MATERIAL)
            obj.toggleHighlight(true, SELECT_NAME, SELECTED_MATERIAL)
            selected = obj
        }
    }

    public companion object {
        public const val DO_NOT_HIGHLIGHT_TAG: String = "doNotHighlight"
        private const val HIGHLIGHT_NAME = "@highlight"
        private const val SELECT_NAME = "@select"
    }
}