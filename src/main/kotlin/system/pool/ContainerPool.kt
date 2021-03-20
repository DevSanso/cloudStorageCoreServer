package system.pool


import container.*
import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Path
import java.nio.file.Paths

import database.system.*
import database.dir.*
import errors.*
import utils.pathHashing256
import utils.light32Hash



typealias ContainerId = Int
typealias NodeId = UInt




class ContainerModel internal constructor(id : ContainerId,container : Container)  {
    val id : ContainerId = id
    private val container : Container = container


    inner class Tree internal constructor() {
        fun create(tree : String) {
            container.createTree(tree)
        }
        fun delete(tree : String) {
            container.deleteTree(tree)
        }
    }

    inner class Node internal constructor() {
        private val nodes = ConcurrentHashMap<NodeId, AccessNode>()

        private fun ConcurrentHashMap<NodeId, AccessNode>.exist(eqOp :  Path) : Boolean {
            return this.values.any { it.hash ==  eqOp }
        }
        private fun ConcurrentHashMap<NodeId, AccessNode>.push(n : AccessNode) : NodeId {
            var k = light32Hash(n.hash.toString(),id.toUInt())

            while(this.keys.any { it == k }) { k += 1u }
            this.put(k,n)
            return k
        }


        fun delete(tree : String,fileName : String) {
            val eqOp = Paths.get(pathHashing256(tree,fileName).toString())
            if(nodes.exist(eqOp) ) {

            }else {

            }
        }


        fun createEmpty(info : NodeInfo) {
            container.createTemp(info)
        }


        fun loadNode(tree : String,name : String) : NodeId {
            val node = container.createNode(tree,name)
            return nodes.push(node)
        }
        fun getNodeSectorCount(id : NodeId) : Long {
            val n = nodes[id]
            if(n == null) {
                throw NotExistException()
            }else {
                return n.sectorCount
            }
        }

        fun write(key : ByteArray,id : NodeId,start : Long,end : Long,data : ByteArray) {
            val n = nodes[id]
            if(n == null) {
                throw NotExistException()
            }else {
                (start..end).forEach{
                    n.write(key.toString(),Sector(it.toInt()))
                }
            }
        }
        fun read(key : ByteArray,id : NodeId,start : Long,end : Long) : List<Sector> {

        }
        fun reEditPermission(tree : String, name : String,permission : Int) {

        }
        fun closeNode(id : NodeId) {

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