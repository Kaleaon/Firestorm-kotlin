package com.firestorm.newview

class LLBox {

    protected val mVertex: Array<FloatArray> = Array(8) { FloatArray(3) }
    protected var mTriangleCount: UInt = 0u

    fun prerender() {
        val size = 1.0f

        mTriangleCount = 12u

        mVertex[0][0] = -size / 2; mVertex[1][0] = -size / 2
        mVertex[2][0] = -size / 2; mVertex[3][0] = -size / 2
        mVertex[4][0] =  size / 2; mVertex[5][0] =  size / 2
        mVertex[6][0] =  size / 2; mVertex[7][0] =  size / 2

        mVertex[0][1] = -size / 2; mVertex[1][1] = -size / 2
        mVertex[4][1] = -size / 2; mVertex[5][1] = -size / 2
        mVertex[2][1] =  size / 2; mVertex[3][1] =  size / 2
        mVertex[6][1] =  size / 2; mVertex[7][1] =  size / 2

        mVertex[0][2] = -size / 2; mVertex[3][2] = -size / 2
        mVertex[4][2] = -size / 2; mVertex[7][2] = -size / 2
        mVertex[1][2] =  size / 2; mVertex[2][2] =  size / 2
        mVertex[5][2] =  size / 2; mVertex[6][2] =  size / 2
    }

    fun cleanupGL() {
        // No GL state to clean up.
    }

    fun renderface(whichFace: Int) {
        val faces = arrayOf(
            intArrayOf(0, 1, 2, 3),
            intArrayOf(3, 2, 6, 7),
            intArrayOf(7, 6, 5, 4),
            intArrayOf(4, 5, 1, 0),
            intArrayOf(5, 6, 2, 1),
            intArrayOf(7, 4, 0, 3)
        )

        // no-op
    }

    fun render() {
        renderface(5)
        renderface(4)
        renderface(3)
        renderface(2)
        renderface(1)
        renderface(0)
        // no-op
    }

    fun getTriangleCount(): UInt = mTriangleCount
}

val gBox = LLBox()
