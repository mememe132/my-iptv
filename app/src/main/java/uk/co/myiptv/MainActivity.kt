
package uk.co.myiptv

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navy = Color.rgb(8, 18, 42)
        val blue = Color.rgb(20, 110, 220)
        val white = Color.WHITE

        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
            setBackgroundColor(navy)
        }

        val heading = TextView(this).apply {
            text = "MY IPTV"
            textSize = 34f
            setTextColor(white)
            gravity = Gravity.CENTER
        }

        screen.addView(heading)

        val subtitle = TextView(this).apply {
            text = "YOUR ENTERTAINMENT HUB"
            textSize = 13f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 32)
        }

        screen.addView(subtitle)

        val sections = listOf(
            "LIVE TV",
            "TV GUIDE",
            "MOVIES",
            "SERIES",
            "FAVOURITES",
            "SETTINGS"
        )

        sections.forEach { section ->
            val button = Button(this).apply {
                text = section
                textSize = 16f
                setTextColor(white)
                setBackgroundColor(blue)
                isFocusable = true
            }

            screen.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    58
                ).apply {
                    setMargins(0, 6, 0, 6)
                }
            )
        }

        setContentView(screen)
    }
}
