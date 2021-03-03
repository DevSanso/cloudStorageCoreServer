package system.pool

import database.system.*
import database.dir.*
import container.Container

typealias ContainerId = Int


class ContainerContext(private val container : Container) {
    inner class Tree internal constructor() {
        fun create(tree : String) {

        }
        fun delete(tree : String) {

        }
    }

    inner class Node internal constructor() {
        fun delete(tree : String,fileName : String) {

        }
        fun createTemp(tree : String,fileName : String,sectorSize : Int) {

        }
    }
    val infoDb : OnlyGetInfoDb get() {return container.getDb}

    val tree = Tree()
    val node = Node()
}

class ContainerPool(private val systemDb: SystemDb) {
    object CacheStore {


    }

    fun deleteContainer(id : ContainerId){

    }
    fun loadContainer(id : ContainerId) : ContainerContext {

    }

    fun getContainerSectorSize(id : ContainerId) : Int {

    }
    fun createContainer(hash : ByteArray,sectorSize : Int) : ContainerId {

    }




}