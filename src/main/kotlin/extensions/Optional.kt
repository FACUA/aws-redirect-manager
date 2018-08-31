package extensions

import java.util.*

fun <T> Optional<T>.toNullable(): T? = if (isPresent) get() else null
fun <T> T?.toOptional(): Optional<T> = Optional.ofNullable(this)
