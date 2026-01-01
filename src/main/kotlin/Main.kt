package com.tbread

fun main() {
    var deviceIdx = PropertyHandler.getProperty("device")
    if (deviceIdx == null) {
        println("사용할 네트워크 기기 번호를 입력하세요.")
        PcapHandler.printDevices()
        deviceIdx = run {
            while (true) {
                readLine()?.toIntOrNull()?.takeIf { it in 0..<PcapHandler.getDeviceSize() }?.let { return@run it }
                println("유효하지 않은 값입니다. 다시 입력해주세요.")
            }
        }.toString()
        PropertyHandler.setProperty("device", deviceIdx)
    }
}