package com.tbread

class DataStorage {
    private val damageStorage = HashMap<String, HashMap<String, MutableSet<ParsedDamagePacket>>>()
    private val nicknameStorage = HashMap<Int, String>()

    fun appendDamage(pdp: ParsedDamagePacket) {
        damageStorage.getOrPut("${pdp.getActorId()}") { hashMapOf() }
            .getOrPut("${pdp.getTargetId()}") { hashSetOf() }
            .add(pdp)
    }

    fun appendNickname(uid: Int, nickname: String) {
        if (nicknameStorage[uid] != null && nicknameStorage[uid] == nickname) return
        println("$uid 할당 닉네임 변경됨 이전: ${nicknameStorage[uid]} 현재: $nickname")
    }

    private fun flushDamageStorage() {
        damageStorage.clear()
    }

    private fun flushNicknameStorage() {
        nicknameStorage.clear()
    }
}