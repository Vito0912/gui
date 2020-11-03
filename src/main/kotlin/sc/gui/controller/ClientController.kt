package sc.gui.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sc.api.plugins.IGameState
import sc.api.plugins.exceptions.GameLogicException
import sc.framework.plugins.Player
import sc.gui.*
import sc.gui.model.PlayerType
import sc.gui.model.TeamSettings
import sc.networking.clients.ILobbyClientListener
import sc.networking.clients.IUpdateListener
import sc.plugin2021.GameState
import sc.plugin2021.Move
import sc.plugin2021.util.GameRuleLogic
import sc.protocol.responses.PrepareGameProtocolMessage
import sc.protocol.responses.ProtocolErrorMessage
import sc.protocol.responses.ProtocolMessage
import sc.shared.GameResult
import tornadofx.Controller
import tornadofx.EventBus
import tornadofx.FXEvent
import java.util.concurrent.CompletableFuture

// This is the listener to update the global state of the server (lobby)
class UILobbyListener : ILobbyClientListener {
    override fun onNewState(roomId: String?, state: IGameState?) {
        logger.debug("listener: onNewState")
        val gameState = state as GameState
        logger.debug("This is what I got: $gameState")
    }

    override fun onError(roomId: String?, error: ProtocolErrorMessage?) {
        logger.debug("listener: onError")
    }

    override fun onRoomMessage(roomId: String?, data: ProtocolMessage?) {
        logger.debug("listener: onRoomMessage")
    }

    override fun onGamePrepared(response: PrepareGameProtocolMessage?) {
        logger.debug("listener: onGamePrepared")
    }

    override fun onGameLeft(roomId: String?) {
        logger.debug("listener: onGameLeft")
    }

    override fun onGameJoined(roomId: String?) {
        logger.debug("listener: onGameJoined")
    }

    override fun onGameOver(roomId: String?, data: GameResult?) {
        logger.debug("listener: onGameOver")
    }

    override fun onGamePaused(roomId: String?, nextPlayer: Player?) {
        logger.debug("listener: onGamePaused")
    }

    override fun onGameObserved(roomId: String?) {
        logger.debug("listener: onGameObserved")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ClientController::class.java)
    }
}

class UIGameListener(val onUpdateHandler: () -> Unit) : IUpdateListener {
    override fun onUpdate(sender: Any?) {
        logger.debug("game listener: onUpdate")
        onUpdateHandler()
    }

    override fun onError(sender: String?) {
        logger.debug("game listener: onError")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ClientController::class.java)
    }
}

class StartGameRequest(val playerOneSettings: TeamSettings, val playerTwoSettings: TeamSettings) : FXEvent(EventBus.RunOn.BackgroundThread)
class NewGameState(val gameState: GameState) : FXEvent()
class HumanMoveRequest(val gameState: GameState) : FXEvent()
class HumanMoveAction(val move: Move) : FXEvent()
class GameOverEvent(val result: GameResult) : FXEvent()


class ClientController : Controller() {
    var lobbyManager: LobbyManager? = null
    private val listener: UIGameListener = UIGameListener(::newGameState)

    // Do NOT call this directly in the UI thread, use fire(StartGameRequest(gameCreationModel)). This way, the game starting is done in the background
    // TODO put everything which is activated by events in a different class and call these from the controller by events
    fun startGame(host: String = "localhost", port: Int = 13050, playerOneSettings: TeamSettings, playerTwoSettings: TeamSettings) {
        // starting the game in the UI thread blocks the UI

        logger.debug("creating and observing")

        val players = arrayOf(playerOneSettings, playerTwoSettings).map {
             when (it.type.value) {
                PlayerType.HUMAN -> InternalClient(host, port, ::humanMoveRequest)
                PlayerType.COMPUTER_EXAMPLE -> InternalClient(host, port, ::testClientMoveRequest)
                PlayerType.COMPUTER -> ComputerClient(host, port, it.executable.get())
                PlayerType.MANUAL -> InternalClient(host, port, ::testClientMoveRequest)
                else -> throw IllegalArgumentException("Cannot create game: Invalid playerType ${it.type.value}")
            }
        }

        lobbyManager = LobbyManager(host, port).apply {
            startNewGame(players, listener) { result ->
                fire(GameOverEvent(result))
            }
        }
    }

    fun newGameState() {
        logger.debug("ClientController got new update")
        val gameControl = lobbyManager?.game
        if (gameControl != null) {
            val gameState = gameControl.currentState as? GameState
            if (gameState != null) {
                logger.debug("gamestate is $gameState")
                fire(NewGameState(gameState))
            } else {
                logger.debug("no gamestate, but $gameControl")
            }
        } else {
            logger.debug("no controlling client yet")
        }
    }

    fun humanMoveRequest(gameState: GameState): CompletableFuture<Move> {
        val future = CompletableFuture<Move>()
        subscribe<HumanMoveAction>(1) {
            future.complete(it.move)
        }
        fire(HumanMoveRequest(gameState))
        return future
    }
    
    fun testClientMoveRequest(state: GameState): CompletableFuture<Move> {
        val possibleMoves = GameRuleLogic.getPossibleMoves(state)
        if(possibleMoves.isEmpty())
            throw GameLogicException("No possible Moves found!")
        return CompletableFuture.completedFuture(possibleMoves.random())
    }
    
    fun updateGameState() {
        val gameState = lobbyManager?.game?.currentState as? GameState
        if (gameState != null) {
            logger.debug("gamestate is $gameState")
            fire(NewGameState(gameState))
        }
    }

    fun previous() {
        lobbyManager?.game?.previous()
        updateGameState()
    }

    fun next() {
        lobbyManager?.game?.next()
        updateGameState()
    }

    fun togglePause() {
        val game = lobbyManager?.game
        if (game != null) {
            if (game.isPaused) {
                game.unpause()
            } else {
                game.pause()
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ClientController::class.java)
    }
}
