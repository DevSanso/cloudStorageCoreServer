package system.pool

import java.lang.Runnable
import java.time.LocalTime

import kotlinx.coroutines.*


import java.util.Queue
import java.util.LinkedList




import container.Container
import worker.Worker


class Context(val id : Int,private val container : Container)   {
    val workerQ : Queue<Worker> = LinkedList<Worker>()
}


private class InternalThread : Runnable {
    var isRunning = true

    private fun loop(block : () -> Unit) {
        while(isRunning)block()
    }

    override fun run() = loop(){
        runBlocking {

        }
    }



    fun off() {isRunning = false}
}

class ThreadPool(val count : Int) {
    private val threads : List<Thread> = makeThread()

    private fun makeThread() = List<Thread>(count) {
        Thread(InternalThread())
    }

}

