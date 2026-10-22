// Recursive local functions cannot be inlined, so conversion reports an internal error.
fun recursiveLocalFunction() {
    <!INTERNAL_ERROR!>fun local() {
        local()
    }<!>
    local()
}
