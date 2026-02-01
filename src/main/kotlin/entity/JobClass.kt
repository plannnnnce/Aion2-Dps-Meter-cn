package com.tbread.entity

enum class JobClass(val className: String, val basicSkillCode: Int) {
    GLADIATOR("剑星", 11020000),
    TEMPLAR("守护星", 12010000),
    RANGER("弓星", 14020000),
    ASSASSIN("杀星", 13010000),
    SORCERER("魔道星", 15210000), /* 마도 확인 필요함 */
    CLERIC("治愈星", 17010000),
    ELEMENTALIST("精灵星", 16010000),
    CHANTER("护法星", 18010000);

    companion object{
        fun convertFromSkill(skillCode:Int):JobClass?{
            return entries.find { it.basicSkillCode == skillCode }
        }
    }
}