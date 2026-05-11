package com.firestorm.newview

// A pie slice that does nothing and is never highlighted on mouse hover
class PieSeparator(params: Params = Params()) : LLUICtrl(params) {

    class Params : LLUICtrl.Params() {
        init {
            name = "pie_separator"
        }
    }
}
