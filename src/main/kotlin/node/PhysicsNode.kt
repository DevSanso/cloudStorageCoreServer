package node

import java.io.File;
import java.io.FileNotFoundException
import java.lang.Exception
import java.security.MessageDigest;

class PhysicsNode  {
    companion object {
        fun delete(node : PhysicsNode) {
            try {
                val isOk = node.file.delete();
                if(!isOk)throw FileNotFoundException();
            }catch(e : Exception) {
                throw e;
            }
        }
        fun create(originPath : String,sectorSize: Int) : PhysicsNode {
            val sha = MessageDigest.getInstance("SHA-512");
            var physicsPath = sha.digest(originPath.toByteArray()).toString();

            val file = File(physicsPath);

            try {
                val isOk = file.createNewFile();
                if(!isOk)throw IllegalArgumentException("Already Exist File");
            }catch(e : Exception) {
                throw e;
            }
            return PhysicsNode(file,originPath,sectorSize);
        }

        fun load(originPath : String,sectorSize: Int) : PhysicsNode {
            val sha = MessageDigest.getInstance("SHA-512");
            var physicsPath = sha.digest(originPath.toByteArray()).toString();
            val file = File(physicsPath);

            try {
                if(!file.exists())throw FileNotFoundException();
            }catch(e : Exception) {
                throw e;
            }
            return PhysicsNode(file,originPath,sectorSize);
        }
    }
    private val file : File;
    private val originPath : String;
    private val sectorSize : Int;

    private constructor(file : File,originPath : String,sectorSize : Int) {
        this.file = file;
        this.originPath = originPath;
        this.sectorSize = sectorSize;
    }


    fun readSector() {

    }

    fun writeSector() {

    }

    fun readAll() {

    }

    fun writeAll() {

    }


}