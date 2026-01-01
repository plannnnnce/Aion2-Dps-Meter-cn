package com.tbread

import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.Pcaps
import kotlin.system.exitProcess

object PcapHandler {

    private val SERVER_IP = PropertyHandler.getProperty("server.ip")
    private val SERVER_PORT = PropertyHandler.getProperty("server.port")
    //추후 배포시엔 디폴트값 넣은채로 빌드하거나, deviceIdx 처럼 자동 저장 추가하기

    private val TIMEOUT_WAIT_TIME = PropertyHandler.getProperty("server.timeout", "10")?.toInt()!!
    private val MAX_SNAPSHOT_SIZE = PropertyHandler.getProperty("server.maxSnapshotSize", "65536")?.toInt()!!

    private val nif by lazy {
        devices[PropertyHandler.getProperty("device")!!.toInt()]
    }

    private val PCAP_HANDLE by lazy {
        nif.openLive(MAX_SNAPSHOT_SIZE, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, TIMEOUT_WAIT_TIME)
    }

    private val devices by lazy {
        try {
            Pcaps.findAllDevs()
        } catch (e: PcapNativeException) {
            println("Pcap 핸들러 초기화 실패 : 네트워크 기기 검색 실패")
            exitProcess(2)
        }
    }


    fun printDevices() {
        for ((i, device) in devices.withIndex()) {
            println(i.toString() + " - " + device.description)
        }
    }

    fun getDeviceSize(): Int {
        return devices.size
    }
    


}