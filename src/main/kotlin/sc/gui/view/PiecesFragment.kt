package sc.gui.view

import javafx.scene.image.Image
import javafx.scene.image.ImageView
import sc.gui.controller.*
import sc.gui.model.PiecesModel
import sc.plugin2021.Color
import sc.plugin2021.PieceShape
import sc.plugin2021.Rotation
import tornadofx.Fragment
import tornadofx.hbox
import tornadofx.plusAssign
import tornadofx.tooltip

class PiecesFragment(color: Color, shape: PieceShape) : Fragment() {
    private val boardController: BoardController by inject()
    val model: PiecesModel = PiecesModel(color, shape)
    private val image: ImageView = resources.imageview("/graphics/blokus/${model.colorProperty().get().name.toLowerCase()}/${model.shapeProperty().get().name.toLowerCase()}.png")

    constructor(selectedColor: ColorBinding, selectedShape: ShapeBinding, selectedRotation: RotationBinding, selectedFlip: FlipBinding) : this(selectedColor.value, selectedShape.value) {
        selectedColor.addListener { _, _, new -> model.colorProperty().set(new) }
        selectedShape.addListener { _, _, new -> model.shapeProperty().set(new) }
        selectedRotation.addListener { _, _, new -> model.rotationProperty().set(new) }
        selectedFlip.addListener { _, _, new -> model.flipProperty().set(new) }
    }

    override val root = hbox {
        this += image
        tooltip(model.shapeProperty().get().name)
    }

    init {
        model.colorProperty().addListener { _, _, _ -> updateImage() }
        model.shapeProperty().addListener { _, _, _ ->
            updateImage()
            root.tooltip(model.shapeProperty().get().name)
        }
        model.rotationProperty().addListener { _, _, _ -> updateImage() }
        model.flipProperty().addListener { _, _, _ -> updateImage() }
        updateImage()
    }

    fun updateImage() {
        val imagePath = "/graphics/blokus/${model.colorProperty().get().name.toLowerCase()}/${model.shapeProperty().get().name.toLowerCase()}.png"
        val size = boardController.boardModel.calculatedBlockSize.get() * 2
        image.image = Image(resources.stream(imagePath), size, size, true, false)

        // apply rotation to imageview
        image.rotate = when (model.rotationProperty().get()) {
            Rotation.RIGHT -> 90.0
            Rotation.LEFT -> -90.0
            Rotation.MIRROR -> 180.0
            else -> 0.0
        }

        // flip imageview
        val rotation = model.rotationProperty().get()
        val flip = model.flipProperty().get()
        image.scaleX = if (flip && (rotation == Rotation.NONE || rotation == Rotation.MIRROR)) -1.0 else 1.0
        image.scaleY = if (flip && (rotation == Rotation.RIGHT || rotation == Rotation.LEFT)) -1.0 else 1.0
    }
}