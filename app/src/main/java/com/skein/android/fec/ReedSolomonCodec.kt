package com.skein.android.fec

/** Transport-independent GF(256) Reed-Solomon codec used by opt-in FEC blocks. */
class ReedSolomonCodec(val dataShards: Int = 8, val parityShards: Int = 4) {
    val totalShards = dataShards + parityShards

    init {
        require(dataShards in 1..254 && parityShards > 0 && totalShards <= 255)
    }

    fun encode(data: List<ByteArray>): List<ByteArray> {
        require(data.size == dataShards)
        val size = data.firstOrNull()?.size ?: throw IllegalArgumentException("No data shards")
        require(size > 0 && data.all { it.size == size })
        val result = data.map(ByteArray::copyOf).toMutableList()
        val matrix = generatorMatrix()
        repeat(parityShards) { parityRow ->
            val out = ByteArray(size)
            for (column in 0 until dataShards) addMultiply(out, data[column], matrix[dataShards + parityRow][column])
            result += out
        }
        return result
    }

    /** Returns all data and parity shards after recovering up to [parityShards] erasures. */
    fun reconstruct(shards: Array<ByteArray?>): List<ByteArray> {
        require(shards.size == totalShards)
        require(shards.count { it != null } >= dataShards) { "Insufficient shards" }
        val size = shards.firstNotNullOf { it }.size
        require(size > 0 && shards.filterNotNull().all { it.size == size })
        val matrix = generatorMatrix()
        val indexes = shards.indices.filter { shards[it] != null }.take(dataShards)
        val inverse = invert(Array(dataShards) { row -> ByteArray(dataShards) { col -> matrix[indexes[row]][col] } })
        val recoveredData = Array(dataShards) { ByteArray(size) }
        for (row in 0 until dataShards) for (column in 0 until dataShards) {
            addMultiply(recoveredData[row], shards[indexes[column]]!!, inverse[row][column])
        }
        return encode(recoveredData.toList())
    }

    private fun generatorMatrix(): Array<ByteArray> = Array(totalShards) { row ->
        ByteArray(dataShards) { col ->
            when {
                row < dataShards -> if (row == col) 1 else 0
                else -> power(row - dataShards + 1, col)
            }.toByte()
        }
    }

    private fun invert(input: Array<ByteArray>): Array<ByteArray> {
        val size = input.size
        val work = Array(size) { row -> ByteArray(size * 2) { col -> if (col < size) input[row][col] else if (col - size == row) 1 else 0 } }
        for (pivotColumn in 0 until size) {
            val pivotRow = (pivotColumn until size).firstOrNull { work[it][pivotColumn].toInt() and 0xff != 0 }
                ?: throw IllegalArgumentException("Singular shard matrix")
            if (pivotRow != pivotColumn) {
                val temporary = work[pivotRow]
                work[pivotRow] = work[pivotColumn]
                work[pivotColumn] = temporary
            }
            val scale = inverse(work[pivotColumn][pivotColumn].toInt() and 0xff)
            for (col in work[pivotColumn].indices) work[pivotColumn][col] = multiply(work[pivotColumn][col].toInt() and 0xff, scale).toByte()
            for (row in 0 until size) if (row != pivotColumn) {
                val factor = work[row][pivotColumn].toInt() and 0xff
                if (factor != 0) for (col in work[row].indices) {
                    work[row][col] = (work[row][col].toInt() xor multiply(factor, work[pivotColumn][col].toInt() and 0xff)).toByte()
                }
            }
        }
        return Array(size) { row -> ByteArray(size) { col -> work[row][col + size] } }
    }

    private fun addMultiply(target: ByteArray, source: ByteArray, coefficient: Byte) {
        val c = coefficient.toInt() and 0xff
        for (index in target.indices) target[index] = (target[index].toInt() xor multiply(source[index].toInt() and 0xff, c)).toByte()
    }

    private fun power(base: Int, exponent: Int): Int = if (exponent == 0) 1 else EXP[(LOG[base] * exponent) % 255]
    private fun inverse(value: Int): Int = if (value == 0) throw IllegalArgumentException("Zero has no inverse") else EXP[255 - LOG[value]]
    private fun multiply(left: Int, right: Int): Int = if (left == 0 || right == 0) 0 else EXP[(LOG[left] + LOG[right]) % 255]

    companion object {
        private val EXP = IntArray(512)
        private val LOG = IntArray(256)
        init {
            var value = 1
            for (index in 0 until 255) {
                EXP[index] = value; LOG[value] = index
                value = value shl 1
                if (value and 0x100 != 0) value = value xor 0x11d
            }
            for (index in 255 until EXP.size) EXP[index] = EXP[index - 255]
        }
    }
}
