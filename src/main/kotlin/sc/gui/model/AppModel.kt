package sc.gui.model

import sc.gui.view.AppView
import tornadofx.*

enum class ViewType {
    START,
    GAME_CREATION,
    GAME_LOADING,
    GAME,
}

class AppModel : ItemViewModel<AppView>() {
    val isDarkMode = booleanProperty(true)
    val currentView = objectProperty(ViewType.START)
}
