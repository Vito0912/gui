package sc.gui.model

import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import mu.KotlinLogging
import tornadofx.*
import java.util.prefs.Preferences

enum class ViewType {
    GAME_CREATION,
    GAME_LOADING,
    GAME,
}

// TODO this shouldn't be global, only for GuiApp
object AppModel: Component() {
    private val logger = KotlinLogging.logger { }
    
    val currentView = objectProperty(ViewType.GAME_CREATION)
    
    val darkMode = configurableBooleanProperty("dark", true)
    val animate = configurableBooleanProperty("animate", true)
    val scaling = configurableNumberProperty("scaling", 1)
    val decoratedWindow = configurableBooleanProperty("decoratedWindow", true) //System.getProperty("os.name").contains("mac"))
    
    fun save() {
        logger.debug { "Saving Preferences" }
        preferences {
            save(darkMode)
            save(animate)
            save(scaling)
            save(decoratedWindow)
        }
    }
}

fun Component.configurableNumberProperty(key: String, default: Number): DoubleProperty {
    var value = default
    preferences { value = getDouble(key, default.toDouble()) }
    return SimpleDoubleProperty(this, key, value.toDouble())
}

fun Component.configurableBooleanProperty(key: String, default: Boolean): BooleanProperty {
    var value = default
    preferences { value = getBoolean(key, default) }
    return SimpleBooleanProperty(this, key, value)
}

fun Preferences.save(prop: ReadOnlyBooleanProperty) = putBoolean(prop.name, prop.value)
fun Preferences.save(prop: ReadOnlyDoubleProperty) = putDouble(prop.name, prop.value)
