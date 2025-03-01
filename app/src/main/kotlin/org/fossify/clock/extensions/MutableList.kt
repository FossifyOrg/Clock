package org.fossify.clock.extensions

@Deprecated(
    message = "Use the `move` extension function from commons",
    replaceWith = ReplaceWith(
        expression = "this.move(index1, index2)",
        imports = ["org.fossify.commons.extensions.move"]
    )
)
fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    this[index1] = this[index2].also {
        this[index2] = this[index1]
    }
}
