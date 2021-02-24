package context

import com.google.protobuf.GeneratedMessageV3


interface Worker {
    companion object {
        internal fun factory()  {

        }
    }

    val name : String
    fun setArgs(arg : GeneratedMessageV3)
    suspend fun run()

}