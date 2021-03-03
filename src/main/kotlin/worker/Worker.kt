package worker

import java.lang.Exception
import java.lang.ClassCastException

import com.google.protobuf.GeneratedMessageV3
import StorageGrpcService.*
import com.google.protobuf.ByteString

import system.pool.ContainerPool


import errors.*
import kotlinx.coroutines.channels.Channel
import system.pool.ContainerContext
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
    LoadWriteNode,
    LoadReadNode,
    WriteSectorArray,
    ReadSectorArray,
    reEditPermissionToNode,
    GetNodeInfoOnlyPermission
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
                WorKerKind.LoadWriteNode -> LoadWriteNodeWorker(pool,arg)
                else -> throw Exception()
            }

        }


    }
    protected abstract fun entry(arg : GeneratedMessageV3) : GeneratedMessageV3


    abstract val name : String
    abstract suspend fun run()
    abstract val response : Channel<WorkerResponse>
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
    override val response = Channel<WorkerResponse>()

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
    override val response = Channel<WorkerResponse>()

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
    override val response = Channel<WorkerResponse>()

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
                    .setIsTree(false)
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
    override val response = Channel<WorkerResponse>()

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
                .setIsTree(false)
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
    override val response = Channel<WorkerResponse>()

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
    override val response = Channel<WorkerResponse>()

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
    override val response = Channel<WorkerResponse>()

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

    override val response = Channel<WorkerResponse>()
}

private class LoadWriteNodeWorker
    (private val pool : ContainerPool,val arg : GeneratedMessageV3) : Worker() {
    override fun entry(arg: GeneratedMessageV3): GeneratedMessageV3 {
        var res : NodeAccessId
        try {
            val castArg = arg as NodeAccess
            val p = pool.loadContainer(castArg.id.id.id.toInt())

            if(p.checkKey(castArg.id.hash.hash.toByteArray()))
                throw NotMatchingHashException()

            val parseNode = NodePath.parsing(castArg.path)
            val id = p.node.createWriteNode(parseNode.tree,parseNode.name)

            res = NodeAccessId.newBuilder()
                .setNodeId(id)
                .build()

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
    override val response = Channel<WorkerResponse>()

}





