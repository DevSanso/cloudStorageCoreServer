package system.pool

import database.system.*
import database.dir.*
import container.Container
import container.Sector

typealias ContainerId = Int
typealias NodeId = Int

class ContainerContext(val id : ContainerId,private val container : Container) {
    inner class Tree internal constructor() {
        fun create(tree : String) {

        }
        fun delete(tree : String) {

        }
    }

    inner class Node internal constructor() {
        fun delete(tree : String,fileName : String) {

        }
        fun exists(tree : String,name : String) : Boolean {

        }

        fun createWriteNode(tree : String, name : String) : NodeId {

        }
        fun createReadNode(tree : String, name : String) : NodeId {

        }
        fun getNodeSize(id : NodeId) : Long {

        }
        fun write(key : ByteArray,id : NodeId,start : Long,end : Long,data : ByteArray) {

        }
        fun read(key : ByteArray,id : NodeId,start : Long,end : Long) : List<Sector> {

        }

        fun doneNode(id : NodeId) {

        }
    }
    val infoDb : OnlyGetInfoDb get() {return container.getDb}
    fun checkKey(key : ByteArray) : Boolean {
        return container.checkHash(key)
    }
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