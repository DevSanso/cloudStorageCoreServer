package directory

import java.security.MessageDigest
import java.nio.file.Paths


import database.dir.*


class Directory(val rootPath : String,private val localDb : DirDB) {
    val infoDB : OnlyGetInfoDb get() = localDb
    val tempPath : String = Paths.get(rootPath,".temp").toString()


    private fun sha512Path(path : String) : String {
        val d = MessageDigest.getInstance("SHA-512")
        return d.digest(path.toByteArray()).toString()

    }
    fun createTree(tree : String) {
        localDb.insertNodeTree(tree)
    }
    fun deleteTree(tree : String) {
        localDb.deleteTree(tree)
    }


    /* dead code
    fun createNode(info : NodeInfo) : AccessNode {
        if (localDb.existFileInOrigin(info.tree,info.fileName))
            throw IllegalArgumentException("Already Exist File")

        val originPath = Paths.get(info.tree,info.fileName).toString()
        val hashP = sha512Path(originPath)
        val node = PhysicsNode.create(hashP,info.sectorSize)
        localDb.insertOriginNodeInfo(info)

        return node
    }*/

    fun createTemp(tree: String, fileName: String, sectorSize: Int): AccessNode {
        if (localDb.existFileInOrigin(tree, fileName))
            throw IllegalArgumentException("Already Exist File")
        else if (!localDb.existTree(tree)) {
            throw IllegalArgumentException("Not exists tree")
        }

        return PhysicsNode.createTemp(
            tempPath,
            sha512Path(Paths.get(tree, fileName).toString()),
            sectorSize
        )
    }

    fun tempDone(tempNode : AccessNode,info : NodeInfo) {
        localDb.insertOriginNodeInfo(info) {
            PhysicsNode.move(tempNode as PhysicsNode,rootPath)
        }
    }

    fun loadNode(tree : String,fileName : String,sectorSize : Int) : AccessNode {
        if (!localDb.existFileInOrigin(tree,fileName))
            throw IllegalArgumentException("not exist file")

        val hashP = sha512Path(Paths.get(tree,fileName).toString())
        return PhysicsNode.load(rootPath,hashP,sectorSize)
    }

    fun deleteNode(tree : String,fileName : String,an : AccessNode) {
        val p = an as PhysicsNode
        if(sha512Path(Paths.get(tree,fileName).toString()) != p.hashPath)
            throw IllegalArgumentException()
        localDb.deleteOriginNodeInfo(tree,fileName) {
            PhysicsNode.delete(p)
        }

    }

}