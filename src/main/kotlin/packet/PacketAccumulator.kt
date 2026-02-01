package com.tbread.packet

import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.util.*

class PacketAccumulator {
    private val logger = LoggerFactory.getLogger(PacketAccumulator::class.java)

    private val buffer = ByteArrayOutputStream()

    // 是否要移到属性配置？优先级较低
    private val MAX_BUFFER_SIZE = 2 * 1024 * 1024
    private val WARN_BUFFER_SIZE = 1024 * 1024

    @Synchronized
    fun append(data: ByteArray) {
        // 出现问题时清空一次以避免OOM，之后加上时间检查来进行容量调整和发生情况监控？
        if (buffer.size() in (WARN_BUFFER_SIZE + 1)..<MAX_BUFFER_SIZE) {
            logger.warn("{} : 缓冲区容量限制临近",logger.name)
        }
        if (buffer.size() > MAX_BUFFER_SIZE) {
            logger.error("{} : 缓冲区容量限制超限，强制初始化进行",logger.name)
            buffer.reset()
        }
        buffer.write(data)
    }

    @Synchronized
    fun indexOf(target: ByteArray): Int {
        // 用于查找魔法包
        val allBytes = buffer.toByteArray()
        if (allBytes.size < target.size) return -1

        for (i in 0..allBytes.size - target.size) {
            var match = true
            for (j in target.indices) {
                if (allBytes[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    @Synchronized
    fun getRange(start: Int, endExclusive: Int): ByteArray {
        // 找到完整包时用于复制
        //待办：如果包结构确认得更清楚可以更改？（载荷内有类似魔法包的数据的情况 / 目前还没有这种情况）
        val allBytes = buffer.toByteArray()
        if (start < 0 || endExclusive > allBytes.size || start > endExclusive) {
            return ByteArray(0)
        }
        return Arrays.copyOfRange(allBytes, start, endExclusive)
    }

    @Synchronized
    fun discardBytes(length: Int) {
        // 找到完整包时用于删除
        // 需要多查一些GC相关的文档来检查效果如何
        val allBytes = buffer.toByteArray()
        buffer.reset()
        // 全部清空再重组是最好的方法吗？之后需要确认，优先级较低

        if (length < allBytes.size) {
            buffer.write(allBytes, length, allBytes.size - length)
        }
    }

}