package system.pool


import java.lang.Runnable
import java.time.LocalTime

import kotlinx.coroutines.*


import context.Context
import system.stream.Stream
import kotlin.coroutines.coroutineContext


private class InternalThread : Runnable {
    var isRunning = true

    private fun loop(block : () -> Unit) {
        while(isRunning)block()
    }

    override fun run() = loop(){
        runBlocking {
            getCtx()?.let { execute(it) }
        }
    }

    private fun execute(ctx : Context) {
        if(!checkWorker(ctx)){
            sendIdleOrPass(ctx)
            return
        }
        var w = ctx.workerQ.poll()

        while(w != null) {
            CoroutineScope(Dispatchers.IO).launch { w.run() }
            w = ctx.workerQ.poll()
        }
        ctx.nowModifed()
        sendRun(ctx)
    }

    private suspend inline fun getCtx() = Stream.run.receive()

    private fun checkWorker(ctx : Context) : Boolean = ctx.workerQ.size != 0

    private inline fun sendIdleOrPass(ctx : Context) {
        if(checkModifed(ctx)) {
            CoroutineScope(Dispatchers.Main).launch {
                Stream.idle.send(ctx)
            }
        }
    }

    private inline fun sendRun(ctx : Context) {
        CoroutineScope(Dispatchers.Main).launch {
            Stream.run.send(ctx)
        }
    }

    private inline fun checkModifed(ctx : Context) : Boolean {
        return LocalTime.now().toSecondOfDay() - ctx.modifed.toSecondOfDay() > 20
    }

    fun off() {isRunning = false}
}

class ThreadPool(val count : Int) {
    private val threads : List<Thread> = makeThread()

    private fun makeThread() = List<Thread>(count) {
        Thread(InternalThread())
    }

}