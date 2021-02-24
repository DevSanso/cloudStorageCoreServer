package context


import java.util.Queue
import java.util.LinkedList
import java.time.LocalTime

import container.Container

class Context(private val container : Container)   {
    private var modifiedDate : LocalTime = LocalTime.now()
    val modifed : LocalTime  get() {return modifed}
    val workerQ : Queue<Worker> = LinkedList<Worker>()

    fun nowModifed() {modifiedDate = LocalTime.now() }
}