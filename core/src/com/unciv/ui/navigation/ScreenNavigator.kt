package com.unciv.ui.navigation

import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.worldscreen.WorldScreen
import kotlin.collections.ArrayDeque
import kotlin.reflect.KClass

/**
 * Owns the navigation stack of the application.
 *
 * ScreenNavigator is deliberately unaware of UncivGame.
 * Application-specific behavior, such as exit confirmation,
 * belongs to the application layer.
 */
class ScreenNavigator(
    private val applyScreen: (BaseScreen) -> Unit
) {

    private val stack = ArrayDeque<BaseScreen>()

    val current: BaseScreen?
        get() = stack.lastOrNull()

    val isEmpty: Boolean
        get() = stack.isEmpty()

    val size: Int
        get() = stack.size

    fun setRoot(screen: BaseScreen) {
        disposeAll()

        stack.addLast(screen)
        applyScreen(screen)
    }

    fun push(screen: BaseScreen) {
        stack.addLast(screen)
        applyScreen(screen)
    }

    fun pop(): BaseScreen? {
        if (stack.size <= 1) return null

        val oldScreen = stack.removeLast()
        val newScreen = stack.last()

        applyScreen(newScreen)
        newScreen.resume()
        oldScreen.dispose()

        return newScreen
    }

    fun replaceCurrent(screen: BaseScreen) {
        check(stack.isNotEmpty()) {
            "Cannot replace the current screen when the screen stack is empty"
        }

        val oldScreen = stack.removeLast()

        stack.addLast(screen)
        applyScreen(screen)

        oldScreen.dispose()
    }

    fun getWorldScreen(): WorldScreen? =
        stack.lastOrNull { it is WorldScreen } as? WorldScreen

    fun resetToWorldScreen(): WorldScreen {
        val worldScreen = getWorldScreen()
            ?: error("Cannot reset to WorldScreen: no WorldScreen exists")

        val screensToRemove = stack.filter { it !== worldScreen }

        for (screen in screensToRemove) {
            screen.dispose()
            stack.remove(screen)
        }

        applyScreen(worldScreen)

        return worldScreen
    }

    fun getScreensOfType(
        clazz: KClass<out BaseScreen>
    ): Sequence<BaseScreen> =
        stack.asSequence()
            .filter { it::class == clazz }

    fun removeScreensOfType(
        clazz: KClass<out BaseScreen>
    ) {
        val screens = getScreensOfType(clazz).toList()

        for (screen in screens) {
            screen.dispose()
            stack.remove(screen)
        }
    }

    fun disposeAll() {
        for (screen in stack) {
            screen.dispose()
        }

        stack.clear()
    }
}
