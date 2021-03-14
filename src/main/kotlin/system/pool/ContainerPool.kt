package system.pool

import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Path
import java.nio.file.Paths

import database.system.*
import database.dir.*
import container.Container
import container.ReadNode
import container.Sector
import container.WriteNode
import errors.*
import utils.pathHashing256

import kotlin.random.Random
import kotlin.random.nextUInt

typealias ContainerId = Int
typealias NodeId = UInt




class ContainerModel  {
    val id : ContainerId
    private val container : Container

    internal constructor(id : ContainerId,container : Container) {
        this.id = id
        this.container = container
    }

    inner class Tree internal constructor() {
        fun create(tree : String) {
            container.createTree(tree)
        }
        fun delete(tree : String) {
            container.deleteTree(tree)
        }
    }

    inner class Node internal constructor() {
        private val willDeleteSet = HashSet<Path>()
        private val writeNodes = ConcurrentHashMap<NodeId, WriteNode>()
        private val readNodes = ConcurrentHashMap<NodeId, ReadNode>()
        private fun ConcurrentHashMap<NodeId, ReadNode>.exist(eqOp :  Path) : Boolean {
            return this.values.any { it.hash ==  eqOp }
        }




        fun deleteRequest(tree : String,fileName : String) {
            val eqOp = Paths.get(pathHashing256(tree,fileName).toString())
            if(readNodes.exist(eqOp)) {

            }else {

            }
        }


        fun createEmpty(info : NodeInfo) {
            container.createTemp(info)
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
        fun reEditPermission(tree : String, name : String,permission : Int) {

        }
        fun doneWriteNode(id : NodeId) {

        }

    }


    val infoDb : OnlyGetInfoDb get() {return container.getDb}
    fun checkKey(key : ByteArray) : Boolean {
        return container.hashKey == key
    }
    val tree = Tree()
    val node = Node()
}

class ContainerPool(private val systemDb: SystemDb) {
    object CacheStore {


    }

    fun deleteContainer(id : ContainerId){

    }
    fun loadContainer(id : ContainerId) : ContainerModel {

    }

    fun getContainerSectorSize(id : ContainerId) : Int {

    }
    fun createContainer(hash : ByteArray,sectorSize : Int) : ContainerId {

    }




}