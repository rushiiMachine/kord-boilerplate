package bot

import dev.kord.common.Color
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

val Color.Companion.WARNING
    get() = Color(247, 149, 84)

val Color.Companion.ERROR
    get() = Color(255, 92, 92)

val Color.Companion.NO_ACTION
    get() = Color(47, 49, 54)
