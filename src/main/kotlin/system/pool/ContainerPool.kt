package system.pool

import database.system.*
import database.dir.*
import container.Container

typealias ContainerId = Int




class ContainerPool(private val systemDb: SystemDb) {
    object CacheStore {


    }

    fun deleteContainer(id : ContainerId){

    }

    fun loadContainer(id : ContainerId,hash : ByteArray){

    }
    fun getContainerSectorSize(id : ContainerId) : Int {

    }
    fun createContainer(hash : ByteArray,sectorSize : Int) : ContainerId {

    }
    fun getContainer(id : ContainerId) : Container {

    }

}