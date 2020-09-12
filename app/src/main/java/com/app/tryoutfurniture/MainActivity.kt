package com.app.tryoutfurniture

import android.annotation.SuppressLint
import android.content.res.Resources
import android.media.CamcorderProfile
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

//TODO: If models are rotating or scaling, don't show delete button
//TODO: Load model from api
//TODO: Make models don't respond to single click on camera button

private const val BOTTOM_SHEET_PEEK_HEIGHT = 50f
private const val DOUBE_TAP_TOLERANCE_MS = 1000L //how many mills to wait till system consider it as a valid tap
class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private val viewNodes = mutableListOf<Node>()

    private val modelsList = mutableListOf(
        Model(R.drawable.chair, "Chair", R.raw.chair),
        Model(R.drawable.table, "Table", R.raw.table),
        Model(R.drawable.oven, "Oven", R.raw.oven),
        Model(R.drawable.piano, "Piano", R.raw.piano)
    )

    private lateinit var selectedModel: Model
    private lateinit var photoSaver: PhotoSaver
    private lateinit var videoRecorder: VideoRecorder
    private var currentlyRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = fragment as ArFragment

        setUpBottomSheet()
        setupRecyclerView()
        setupDoubleArPlaneListener()

        photoSaver = PhotoSaver(this)
        videoRecorder = VideoRecorder(this).apply {
            sceneView = arFragment.arSceneView
            setVideoQuality(CamcorderProfile.QUALITY_1080P,resources.configuration.orientation)
        }

        setUpFab()

        //called on each frame
        getCurrentScene().addOnUpdateListener{
            rotateViewNodesTowardUser()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpFab(){
        fabCamera?.setOnClickListener {
            if(!currentlyRecording){
                photoSaver.takePhoto(arFragment.arSceneView)
            }
        }

        fabCamera?.setOnLongClickListener {
            currentlyRecording = videoRecorder.toggleRecordingState()
            true
        }

        fabCamera.setOnTouchListener { _, event ->
            //if we release the finger
            if(event.action == MotionEvent.ACTION_UP && currentlyRecording){
                currentlyRecording = videoRecorder.toggleRecordingState()
                Toast.makeText(this,"Video saved successfully",Toast.LENGTH_SHORT).show()
                true
            }
            false
        }

    }

    private fun setupDoubleArPlaneListener(){

        var firstTapTime = 0L

        arFragment.setOnTapArPlaneListener{ hitResult, _, _ ->

            if(firstTapTime == 0L){
                firstTapTime = System.currentTimeMillis()
            }
            else if(System.currentTimeMillis() - firstTapTime < DOUBE_TAP_TOLERANCE_MS){
                firstTapTime = 0L
                loadModel { modelRenderable, viewRenderable -> addNodeToScene(
                    hitResult.createAnchor(),
                    modelRenderable, viewRenderable
                ) }
            }
            else{
                firstTapTime = System.currentTimeMillis()
            }
        }
    }

    private fun setupRecyclerView() {
        rvModel.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvModel.adapter = ModelAdapter(this, modelsList).apply {
            this.selectedModel.observe(this@MainActivity, Observer {
                this@MainActivity.selectedModel = it
                val newTitle = "Models (${it.title})"
                tvMdodel?.text = newTitle
            })
        }
    }

    private fun setUpBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomsheet)

        //setup peek height which is always visible on the screen - set peek height
        bottomSheetBehavior.peekHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            BOTTOM_SHEET_PEEK_HEIGHT, resources.displayMetrics
        ).toInt()

        //we need to make bottom sheet to the front of the layout where text is
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomSheet.bringToFront()
            }

        })

    }

    private fun createDeleteButton(): Button {
        return Button(this).apply {
            text = "Delete"
            setBackgroundColor(android.graphics.Color.RED)
            setTextColor(android.graphics.Color.WHITE)
        }
    }

    private fun rotateViewNodesTowardUser(){
        for (node in viewNodes){
            //check if node is visible
            node.renderable?.let {
                val cameraPos = getCurrentScene().camera.worldPosition // pos of camera
                val viewNodePos = node.worldPosition //pos of viewnode
                val dir = Vector3.subtract(cameraPos, viewNodePos)
                node.worldRotation = Quaternion.lookRotation(dir, Vector3.up())

            }
        }
    }

    private fun getCurrentScene() = arFragment.arSceneView.scene

    /*
        Node ->  we need to attach model to a node. A node contains information about an object
        in the ar scene i.e position, rotation, scale n so on...
        Anchor node -> used to create anchor node and anchor node makes sure the object will
        have it's place in ar scene so if the user moves in the ar scene the anchor maintains the
        position.
     */
    private fun addNodeToScene(
        anchor: Anchor,
        modelRenderable: ModelRenderable,
        viewRenderable: ViewRenderable
    ) {
        val anchorNode = AnchorNode(anchor)
        /*
            node can also can have an hierarchy, so every 3d object respond will have anchor node
            as parent and we can add child nodes to this parent anchor node that can have different
            behavior and whatever changes happens to parent node will also happen to children nodes.
            if we change position of parent node and corresponding children will also react.
         */
        val modelNode = TransformableNode(arFragment.transformationSystem).apply {
            renderable = modelRenderable
            scaleController.maxScale = 0.5f
            scaleController.minScale = 0.1f
            setParent(anchorNode)
            getCurrentScene().addChild(anchorNode)
            select() //select the node
        } //for resize, rotate and move our object that belong to this node

        val viewNode = Node().apply {
            renderable = null
            setParent(modelNode)
            val box = modelNode.renderable?.collisionShape as Box //each node has collision node, cube represents height of node
            localPosition = Vector3(0f, box.size.y, 0f)
            (viewRenderable.view as Button).setOnClickListener {
                getCurrentScene().removeChild(anchorNode)
                viewNodes.remove(this)
            }

        }

        viewNodes.add(viewNode)

        modelNode.setOnTapListener{ _, _ ->
            if(!modelNode.isTransforming) { //this node if not moving
                if(viewNode.renderable == null){
                    viewNode.renderable = viewRenderable
                }
                else{
                    viewNode.renderable = null
                }
            }
        }

    }

    /*
        ModelRenderable -> it represent 3D object
        ViewRenderable -> it is 3d representation of xml view for android to able to make layouts
        display as 3d views or AR scene. Every 250dp of views you use in view renderable will of
        1m size in your AR scene
        -> The rotation of the model is saved in node which it is attached to
     */
    private fun loadModel(callback: (ModelRenderable, ViewRenderable) -> Unit) {
        val modelRenderable = ModelRenderable.Builder()
            .setSource(this, selectedModel.modelResourceId) //which app resource
            .build()

        val viewRenderable = ViewRenderable.builder()
            .setView(this, createDeleteButton())
            .build()

        //check if renderable finished up loading
        CompletableFuture.allOf(modelRenderable, viewRenderable).thenAccept {
            callback(modelRenderable.get(), viewRenderable.get())
        } //it will wait until all renderable finish loading
            .exceptionally {
                Toast.makeText(this, "Error loading model : $it", Toast.LENGTH_SHORT).show()
                null
            }

    }

    // Quaternions used to rotate the models

}