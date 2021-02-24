package system.stream

import java.util.concurrent.ConcurrentHashMap

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.receiveOrNull

import context.Context
import context.Worker



class WorkerInputStream private constructor(val name : String)  {
    private val channel : Channel<Context> = Channel<Context>()
    private val cacheWorkerStore = ConcurrentHashMap<Int,HashSet<Worker>>()


    companion object {
        internal fun create(name : String) = WorkerInputStream(name)
    }

    fun addWorker(containerId: Int, worker: Worker) {
        cacheWorkerStore[containerId]?.add(worker)
    }
    fun removeWorkerCache(containerId : Int) {
        cacheWorkerStore.remove(containerId)
    }

    suspend fun send(ctx : Context) {
        channel.send(ctx)
        if(!cacheWorkerStore.containsKey(ctx.id))
            cacheWorkerStore.put(ctx.id,HashSet<Worker>())
    }

    suspend fun receive() : Context? {
        val ctx = channel.receiveOrNull()
        return if(ctx == null) {
            null
        } else {
            val workers = cacheWorkerStore[ctx.id]

            if(workers != null && workers?.size != 0) {
                ctx.workerQ.addAll(workers)
                coroutineScope { workers?.clear() }
            }

            ctx
        }
    }
}

object Stream {
    val run = WorkerInputStream.create("run")
    val idle = WorkerInputStream.create("idle")
    val free = Channel<Context>()
}





