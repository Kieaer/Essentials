import com.github.lalyos.jfiglet.FigletFont
import essentials.Main
import org.junit.Test

const val TILE_SIZE = 16
const val MAP_WIDTH = 10
const val MAP_HEIGHT = 10

class SimpleTets {
    @Test
    fun test() {
        println(FigletFont.convertOneLine(Main::class.java.classLoader.getResourceAsStream("5x7.flf"), "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-="))
    }
}