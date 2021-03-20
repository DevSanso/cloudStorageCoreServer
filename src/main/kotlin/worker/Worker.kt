package worker

import java.lang.Exception
import java.lang.ClassCastException

import com.google.protobuf.GeneratedMessageV3
import StorageGrpcService.*
import com.google.protobuf.ByteString
import container.Sector

import system.pool.ContainerPool


import errors.*
import kotlinx.coroutines.channels.Channel
import org.joda.time.DateTime
import java.io.File
import java.nio.file.Paths


enum class WorKerKind {
    GetLimitSectorSize,
    CreateContainer,
    DeleteContainer,
    GetNodeInfo,
    GetChildNodeInfos,
    CreateTree,
    DeleteTree,
    DeleteNode,
    CreateNode,
    LoadNode,
    CloseNode,
    WriteSectorArray,
    ReadSectorArray,
    ReEditPermissionToNode,
}


data class WorkerResponse(val workerName : String,val data : GeneratedMessageV3)

abstract class Worker {
    companion object {
        fun  factory(mk : WorKerKind, pool: ContainerPool, arg : GeneratedMessageV3) : Worker {
            return when(mk) {
                WorKerKind.GetLimitSectorSize -> GetLimitSectorSizeWorker(pool,arg)
                WorKerKind.CreateContainer -> CreateContainerWorker(pool,arg)
                WorKerKind.DeleteContainer -> DeleteContainerWorker(pool,arg)
                WorKerKind.GetNodeInfo -> GetNodeInfoWorker(pool,arg)
                WorKerKind.GetChildNodeInfos -> GetChildNodeInfosWorker(pool,arg)
                WorKerKind.CreateTree -> CreateTreeWorker(pool,arg)
                WorKerKind.DeleteTree -> DeleteTreeWorker(pool,arg)
                WorKerKind.DeleteNode -> DeleteNodeWorker(pool,arg)
                WorKerKind.LoadNode -> LoadNodeWorker(pool,arg)
                WorKerKind.WriteSectorArray -> WriteSectorArrayWorker(pool,arg)
                WorKerKind.ReadSectorArray -> ReadSectorArrayWorker(pool,arg)
                WorKerKind.CreateNode -> CreateNodeWorker(pool,arg)
                WorKerKind.ReEditPermissionToNode -> ReEditPermissionToNodeWorker(pool,arg)
                WorKerKind.CloseNode -> CloseNodeWorker(pool,arg)
            }

        }


    }
    protected abstract fun entry(arg : GeneratedMessageV3) : GeneratedMessageV3


    abstract val name : String
    abstract suspend fun run()
    val response = Channel<WorkerResponse>()
}

private class GetLimitSectorSizeWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : SectorSize
        try {
            val castArg = arg as ContainerId
            val size = pool.getContainerSectorSize(castArg.id.toInt())
            res = SectorSize.newBuilder().setByteSize(size).build()
        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }

    override val name : String get() {return "GetLimitSectorSizeWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}

private class CreateContainerWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : CommonMessage
        try {
            val castArg = arg as CreatePool
            val id = pool.createContainer(castArg.hash.hash.toByteArray(),castArg.size.byteSize)
            res = CommonMessage.newBuilder()
                .setStatusCode(200)
                .setMessageBytes(ByteString.copyFromUtf8(id.toString()))
                .build()
        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }

    override val name : String get() {return "CreateContainerWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}



private class DeleteContainerWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : CommonMessage
        try {
            val castArg = arg as ContainerId
            pool.deleteContainer(castArg.id.toInt())
            res = CommonMessage.newBuilder()
                .setStatusCode(200)
                .setMessageBytes(ByteString.copyFromUtf8(castArg.id.toString()))
                .build()
        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }

    override val name : String get() {return "DeleteContainerWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}


private data class NodePath(val tree : String,val name : String) {
    companion object{
        fun parsing(path : String) : NodePath {
            val p = Paths.get(path)

            return NodePath(parent(p.parent.toString()),p.fileName.toString())
        }
        private fun parent(p : String) : String {
            return if(File.separatorChar != '/') {
                p.replace('\\','/')
            }else p
        }
    }
}

private class GetNodeInfoWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : NodeInfo
        try {
            val castArg = arg as CommonRpcUrl
            val p = NodePath.parsing(castArg.path)

            val container = pool.loadContainer(castArg.id.id.toInt())
            val nodeInfo = container.infoDb.getNodeOriginInfo(p.tree,p.name)
            if(nodeInfo != null) {
                res = NodeInfo.newBuilder()
                    .setFileName(nodeInfo.fileName)
                    .setPath(nodeInfo.tree)
                    .setSize(nodeInfo.size.toLong())
                    .setPermission(nodeInfo.permission)
                    .setDate(nodeInfo.fileDate.toString())
                    .build()
            }else {
                throw NotExistException()
            }
        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }

    override val name : String get() {return "GetNodeInfoWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}

private class GetChildNodeInfosWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : NodeList
        try {
            val castArg = arg as CommonRpcUrl
            val container = pool.loadContainer(castArg.id.id.toInt())
            val nodeInfos = container.infoDb.getNodeOriginInfos(castArg.path)
            res = NodeList.newBuilder().addAllNodes(makeList(nodeInfos)).build()
        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }
    private fun makeList(li : ArrayList<database.dir.NodeInfo>) : ArrayList<NodeInfo>{
        val res = ArrayList<NodeInfo>()
        val builder = NodeInfo.newBuilder()
        for(i in li) {
            res.add(builder
                .setFileName(i.fileName)
                .setPath(i.tree)
                .setSize(i.size.toLong())
                .setPermission(i.permission)
                .setDate(i.fileDate.toString())
                .build())
        }
        return res
    }
    override val name : String get() {return "GetChildNodeInfosWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }

}

private class CreateTreeWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : CommonMessage
        try {
            val castArg = arg as CommonRpcUrl
            val container = pool.loadContainer(castArg.id.id.toInt())
            container.tree.create(castArg.path)
            res = CommonMessage.newBuilder()
                .setStatusCode(200)
                .setMessage("create : " + castArg.path)
                .build()
        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }

    override val name : String get() {return "CreateTreeWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}

private class DeleteTreeWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : CommonMessage
        try {
            val castArg = arg as CommonRpcUrl
            val conatainer = pool.loadContainer(castArg.id.id.toInt())
            conatainer.tree.delete(castArg.path)
            res = CommonMessage.newBuilder()
                .setStatusCode(200)
                .setMessage("delete : " + castArg.path)
                .build()
        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }

    override val name : String get() {return "DeleteTreeWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}

private class DeleteNodeWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res: CommonMessage
        try {
            val castArg = arg as CommonRpcUrl
            val conatainer = pool.loadContainer(castArg.id.id.toInt())
            val parsePath = NodePath.parsing(castArg.path)
            conatainer.node.delete(parsePath.tree, parsePath.name)
            res = CommonMessage
                .newBuilder()
                .setStatusCode(200)
                .setMessage("delete : "+castArg.path)
                .build()

        } catch (e: Exception) {
            response.close()
            if (e is ClassCastException) throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }

    override val name: String
        get() {
            return "DeleteTreeWorker"
        }

    override suspend fun run() {
        response.send(WorkerResponse(name, entry(arg)))
    }


}



private class LoadNodeWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : NodeAccessId
        try {
            val castArg = arg as NodeAccess
            val p = pool.loadContainer(castArg.id.id.id.toInt())

            if(p.checkKey(castArg.id.hash.hash.toByteArray()))
                throw NotMatchingHashException()

            val parseNode = NodePath.parsing(castArg.path)
            val id = p.node.loadNode(parseNode.tree,parseNode.name)

            res = NodeAccessId.newBuilder()
                .setNodeId(id.toInt())
                .build()

        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }

    override val name : String get() {return "LoadNodeWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}



private class WriteSectorArrayWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : WriteMessage
        try {
            val castArg = arg as NodePoint
            val p = pool.loadContainer(castArg.key.id.id.toInt())
            val key = castArg.key.hash.hash.toByteArray()
            if(p.checkKey(key))
                throw NotMatchingHashException()

            val parseNode = NodePath.parsing(castArg.path)
            val off = castArg.offset
            p.node.write(key,castArg.nodeId.nodeId.toUInt(),
                off.start,off.end,
                castArg.block.toByteArray())

            res = WriteMessage.newBuilder()
                .setMessage(CommonMessage
                    .newBuilder()
                    .setStatusCode(200)
                    .setMessage("writing : "+castArg.path)
                    .build())
                .addAllCompleteOffset((off.start..off.end).map{it}).build()

        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }



    override val name : String get() {return "WriteSectorArrayWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}
private class ReadSectorArrayWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : ReadMessage
        try {
            val castArg = arg as NodePoint
            val p = pool.loadContainer(castArg.key.id.id.toInt())
            val key = castArg.key.hash.hash.toByteArray()
            if(p.checkKey(key))
                throw NotMatchingHashException()

            val parseNode = NodePath.parsing(castArg.path)
            val off = castArg.offset
            val sectors = p.node.read(key,castArg.nodeId.nodeId.toUInt(),off.start,off.end)


            res = ReadMessage.newBuilder()
                .setMessage(CommonMessage
                    .newBuilder()
                    .setStatusCode(200)
                    .setMessage("reading : "+castArg.path)
                    .build())
                .setOffset(Offset.newBuilder()
                    .setOffsetCount(p.node.getNodeSectorCount(castArg.nodeId.nodeId.toUInt()))
                    .setStart(off.start)
                    .setEnd(off.end)
                    .build()
                )
                .setBlock(ByteString.copyFrom(convertSectorToBlock(sectors)))
                .build()

        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }


    private inline fun convertSectorToBlock(sector : List<Sector>) : ByteArray {
        var res = listOf<Byte>()
        sector.forEach {
            res = res + it.data.sliceArray(IntRange(0,it.originSize)).toList()
        }
        return res.toByteArray()
    }
    override val name : String get() {return "ReadSectorArrayWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}
private class CreateNodeWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : CommonMessage
        try {
            val castArg = arg as NodeInfo
            val p = pool.loadContainer(castArg.id.id.toInt())
            p.node.createEmpty(convertInfoClass(castArg));
            res = CommonMessage.newBuilder()
                .setStatusCode(200)
                .setMessage("create empty : ${castArg.path}/${castArg.fileName}")
                .build()

        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }


    private inline fun convertInfoClass(info : NodeInfo) : database.dir.NodeInfo {
        return database.dir.NodeInfo(info.fileName,info.path, DateTime(info.date),info.size,info.permission)
    }
    override val name : String get() {return "CreateNodeWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }

}

private class ReEditPermissionToNodeWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : CommonMessage
        try {
            val castArg = arg as Permission
            val p = pool.loadContainer(castArg.id.id.toInt())
            val path = NodePath.parsing(castArg.path)
            p.node.reEditPermission(path.tree,path.name,castArg.permission)
            res = CommonMessage.newBuilder()
                .setStatusCode(200)
                .setMessage(makeMessage(path,castArg.permission))
                .build()
        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }


    private inline fun makeMessage(p : NodePath,permission: Int) : String {
        return "reedit permission : ${p.tree}/${p.name} : ${permission.toString()}"
    }
    override val name : String get() {return "ReEditPermissionToNodeWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}

private class CloseNodeWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : CommonMessage
        try {
            val castArg = arg as NodePoint
            val p = pool.loadContainer(castArg.key.id.id.toInt())
            p.node.closeNode(castArg.nodeId.nodeId.toUInt())
            res = CommonMessage.newBuilder()
                .setStatusCode(200)
                .setMessage(makeMessage(castArg.path))
                .build()
        }catch (e : Exception) {
            response.close()
            if(e is ClassCastException)throw CantConvertGrpcArgsException()
            throw e
        }
        return res
    }


    private inline fun makeMessage(p : String) : String {
        return "write node close : $p"
    }
    override val name : String get() {return "CloseNodeWorker"}
    override suspend fun run()  {
        response.send(WorkerResponse(name,entry(arg)))
    }


}



