fun recursiveLocalFunction() {
    <!INTERNAL_ERROR!>fun local() {
        local()
    }<!>
    local()
}
