package system.pool

import java.lang.Runnable

import kotlinx.coroutines.*

import java.util.Queue
import java.util.LinkedList

import container.Container
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.receiveOrNull
import worker.Worker


class ThreadPool(val count : Int) {
    object Stream {
        private val stream = Channel<Worker>()
        suspend fun getWorker() = stream.receive()
        suspend fun send(w : Worker) {
            stream.send(w)
        }
    }
    private val threads : List<Thread> = makeThread()

    private class InternalThread : Runnable {
        var isRunning = true

        private fun loop(block : () -> Unit) {
            while(isRunning)block()
        }

        override fun run() = loop(){
            CoroutineScope(Dispatchers.Default).launch {
                Stream.getWorker().run()
            }
        }



        fun off() {isRunning = false}
    }
    fun sendWorker(w : Worker) {
        CoroutineScope(Dispatchers.IO).launch {
            Stream.send(w)
        }
    }
    private fun makeThread() = List<Thread>(count) {
        Thread(InternalThread())
    }

}

