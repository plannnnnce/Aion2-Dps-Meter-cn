package com.tbread.packet

import com.tbread.config.PcapCapturerConfig
import kotlinx.coroutines.channels.Channel
import org.pcap4j.core.*
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.TcpPacket
import org.slf4j.LoggerFactory
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.system.exitProcess


class PcapCapturer(private val config: PcapCapturerConfig, private val channel: Channel<ByteArray>) {

    companion object {
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

        private fun getAllDevices(): List<PcapNetworkInterface> {
            return try {
                Pcaps.findAllDevs() ?: emptyList()
            } catch (e: PcapNativeException) {
                logger.error("Pcap处理器初始化失败",e)
                exitProcess(2)
            }
        }
    }

    private fun getMainDevice(ip: String): PcapNetworkInterface? {
        val devices = getAllDevices()
        for (device in devices) {
            for (addr in device.addresses) {
                if (addr.address != null) {
                    if (addr.address.hostAddress.equals(ip)) {
                        return device
                    }
                }
            }
        }
        logger.warn("网络设备搜索失败")
        return null
    }


    fun start() {
        val socket = DatagramSocket()
        socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
        val ip = socket.localAddress.hostAddress
        if (ip == null) {
            logger.error("IP搜索失败.")
            exitProcess(1)
            //稍后连接GUI后整理并处理
        }
        val nif = getMainDevice(ip)
        if (nif == null){
            logger.error("网络设备搜索失败.")
            exitProcess(1)
        }
        val handle = nif.openLive(config.snapshotSize, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, config.timeout)
//        val filter = "src net ${config.serverIp} and port ${config.serverPort}"
        val filter = "tcp src port ${config.serverPort}"
        handle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE)
        logger.info("包过滤器设置 \"$filter\"")
        val listener = PacketListener { packet ->
            val ipv4 = packet.get(IpV4Packet::class.java)
            val srcIp: String? = ipv4.getHeader().getSrcAddr().getHostAddress()
            val dstIp: String? = ipv4.getHeader().getDstAddr().getHostAddress()
            logger.info("receive packet from {} to {}", srcIp, dstIp)
            if (packet.contains(TcpPacket::class.java)) {
                val tcpPacket = packet.get(TcpPacket::class.java)

                val payload = tcpPacket.payload
                tcpPacket.header.srcPort
                if (payload != null) {
                    val data = payload.rawData
                    if (data.isNotEmpty()) {
                        channel.trySend(data)
                    }
                }
            }
        }
        try {
            handle.use { h ->
                h.loop(-1, listener)
            }
        } catch (e: InterruptedException) {
            logger.error("通道消费中出现问题.",e)
        }
    }


}