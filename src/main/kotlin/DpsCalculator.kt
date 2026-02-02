package com.tbread

import com.tbread.entity.DpsData
import com.tbread.entity.JobClass
import com.tbread.entity.ParsedDamagePacket
import com.tbread.entity.PersonalData
import com.tbread.entity.TargetInfo
import com.tbread.logging.DebugLogWriter
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt
import java.util.UUID

class DpsCalculator(private val dataStorage: DataStorage) {
    private val logger = LoggerFactory.getLogger(DpsCalculator::class.java)

    enum class Mode {
        ALL, BOSS_ONLY
    }

    enum class TargetSelectionMode(val id: String) {
        MOST_DAMAGE("mostDamage"),
        MOST_RECENT("mostRecent"),
        ALL_TARGETS("allTargets");

        companion object {
            fun fromId(id: String?): TargetSelectionMode {
                return entries.firstOrNull { it.id == id } ?: MOST_DAMAGE
            }
        }
    }

    data class TargetDecision(
        val targetIds: Set<Int>,
        val targetName: String,
        val mode: TargetSelectionMode,
        val trackingTargetId: Int,
    )

    companion object {
        val POSSIBLE_OFFSETS: IntArray =
            intArrayOf(
                0, 10, 20, 30, 40, 50,
                120, 130, 140, 150,
                230, 240, 250,
                340, 350,
                450,
                1230, 1240, 1250,
                1340, 1350,
                1450,
                2340, 2350,
                2450,
                3450
            )

        val SKILL_MAP = mapOf(
            /*
        Cleric
         */
            17010000 to "大地的报复",
            17020000 to "雷电",
            17030000 to "放电",
            17040000 to "审判之闪电",
            17050000 to "天罚",
            17040007 to "天罚(追加施放1次)",
            17080000 to "虚弱烙印",
            17070000 to "痛苦连锁",
            17150000 to "神圣气息",
            17150002 to "神圣气息",
            17090000 to "再生之光",
            17350000 to "惩戒",
            17100000 to "治愈之光",
            17120000 to "快愈光辉",
            17120001 to "快愈光辉MAX",
            17370000 to "闪电乱射",
            17060000 to "霹雳",
            17060001 to "霹雳1阶段",
            17060002 to "霹雳2阶段",
            17060003 to "霹雳MAX",
            17240000 to "冲击解除",
            17320000 to "再生祝福",
            17280000 to "权能爆发",
            17290000 to "免罪",
            17160000 to "治愈气息",
            17430000 to "增幅祈祷",
            17390000 to "召唤复活",
            17400000 to "大地的惩罚",
            17270000 to "救赎",
            17190000 to "束缚",
            17410000 to "保护之光",
            17420000 to "伊丝蒂尔的权能",
            17300000 to "毁灭之声",
            17700000 to "强袭烙印",
            17710000 to "温暖护佑",
            17720000 to "主神护佑",
            17730001 to "主神恩赐",
            17730002 to "主神恩赐(增幅祈祷)",
            17740000 to "治愈力强化",
            17750000 to "不死屏障",
            17760000 to "恢复阻断",
            17770000 to "集中祈祷",
            17780000 to "大地的恩赐",
            17790000 to "生存意志",
            17800000 to "璀璨护佑",

            /*
        Gladiator
         */
            11020000 to "锐利一击",
            11030000 to "撕裂一击",
            11040000 to "愤怒一击",
            11180000 to "鲁莽一击",
            11010000 to "切断的猛击",
            11420000 to "击破的猛击",
            11190000 to "跳跃下击",
            11290000 to "蹂躏之剑",
            11290001 to "蹂躏之剑",
            11170000 to "下劈",
            11440000 to "上撩",
            11280000 to "剑气乱舞",
            11200000 to "割踝",
            11210000 to "碎踝",
            11050000 to "粉碎波动",
            11060000 to "狂暴波动",
            11360000 to "突进一击",
            11360001 to "突进一击-Max",
            11300000 to "空中束缚",
            11370000 to "强制下落",
            11100000 to "毁灭的猛击",
            11100001 to "毁灭的猛击-1阶段",
            11100002 to "毁灭的猛击-Max",
            11260000 to "冲击解除",
            11320000 to "愤怒爆发",
            11240000 to "愤怒波动",
            11400000 to "突击姿态",
            11250000 to "吉凯尔的祝福",
            11110000 to "集中格挡",
            11130000 to "平衡护甲",
            11080000 to "挥舞刀刃",
            11090000 to "刀刃火花",
            11380000 to "坚韧",
            11340000 to "吸血之剑",
            11390000 to "激怒爆发",
            11410000 to "波动护甲",
            11430000 to "强制束缚",
            11700000 to "强袭一击",
            11170037 to "下击",
            11710000 to "生存姿态",
            11720000 to "保护护甲",
            11730000 to "血液吸收",
            11740000 to "弱点掌握",
            11750000 to "攻击准备",
            11760000 to "冲击命中",
            11770000 to "破坏冲动",
            11770007 to "破坏",
            11780000 to "老练反击",
            11790000 to "生存意志",
            11800008 to "杀气爆发",

            /*
        Elementalist
         */
            16010000 to "寒气冲击",
            16020000 to "真空爆炸",
            16030000 to "大地震动",
            16040000 to "火焰全烧",
            16050000 to "灰烬召唤",
            16100004 to "火之精灵:跳跃下击",
            16110004 to "水之精灵:散发寒气",
            16110005 to "水之精灵:强化散发寒气",
            16140000 to "协攻:诅咒",
            16001104 to "火之精灵:火焰爆发",
            16001108 to "水之精灵:水弹",
            16001110 to "风之精灵:旋风",
            16001113 to "地之精灵:撞击",
            16001117 to "古代精灵:破坏",
            16340000 to "连续乱射",
            16130001 to "地之精灵:躯干撞击",
            16130005 to "地之精灵:强化躯干撞击",
            16330000 to "空间支配",
            16120002 to "风之精灵:落下之风",
            16120005 to "风之精灵:强化落下之风",
            16070000 to "灵魂尖叫",
            16300000 to "元素融合",
            16300001 to "元素融合-1阶段",
            16300002 to "元素融合-2阶段",
            16300003 to "元素融合-Max",
            16200000 to "冲击解除",
            16210000 to "绝望的诅咒",
            16710000 to "精灵打击",
            16720000 to "精灵保护",
            16730001 to "精灵降临",
            16740001 to "侵蚀",
            16750000 to "精灵重生",
            16760000 to "精神集中",
            16800001 to "连续逆流",
            16770000 to "精灵共鸣",
            16790000 to "重生契约",
            16780000 to "元素集结",
            16240000 to "协攻:毁灭攻势",
            16240001 to "协攻:毁灭攻势-1阶段",
            16240002 to "协攻:毁灭攻势-2阶段",
            16240003 to "协攻:毁灭攻势-Max",
            16001301 to "火之精灵:陨石召唤",
            16001305 to "水之精灵:冰河毒镖",
            16001309 to "风之精灵:风暴",
            16001313 to "地之精灵:巨大树干",
            16001317 to "古代精灵:自体风暴",
            16190000 to "强化:精灵护佑",
            16370000 to "烈火祝福",
            16250000 to "召唤:古代精灵",
            16250001 to "古代精灵:影子炮",
            16150000 to "协攻:腐蚀",
            16151100 to "火之精灵:愤怒爆发",
            16152100 to "水之精灵:冰链",
            16154100 to "风之精灵:突风",
            16153100 to "地之精灵:挑衅",
            16360000 to "凯西内尔的权能",
            16060000 to "吸入",
            16080000 to "恐惧尖叫",
            16220000 to "诅咒云",
            16230000 to "魔法夺取",
            16260000 to "魔法阻断",
            16700000 to "强袭恐惧",
            100055 to "基本攻击(古代精灵)",
            100051 to "基本攻击(古代精灵)",
            100014 to "基本攻击(火之精灵)",
            100024 to "基本攻击(水之精灵)",
            100032 to "基本攻击(风之精灵)",
            100045 to "基本攻击(地之精灵)",
            100041 to "基本攻击(地之精灵)",
            100018 to "基本攻击(火之精灵)",
            100028 to "基本攻击(水之精灵)",


            /*
        Chanter
         */
            18010000 to "击破碎",
            18020000 to "共鸣碎",
            18030000 to "霹雳碎",
            18370000 to "暴风碎",
            18040000 to "白热击",
            18050000 to "撕裂击",
            18090000 to "突进击破",
            18090001 to "突进击破-1阶段",
            18090002 to "突进击破-2阶段",
            18090003 to "突进击破-MAX",
            18060000 to "打击碎",
            18070000 to "破散击",
            18100000 to "暗击碎",
            18400000 to "贯通碎",
            18300000 to "疾风乱舞",
            18150000 to "热波击",
            18120000 to "快愈咒语",
            18210000 to "震动碎",
            18390000 to "激动碎",
            18080000 to "波动击",
            18080001 to "波动击",
            18290000 to "回旋击",
            18200000 to "冲击解除",
            18410000 to "强击碎",
            18220000 to "灭火",
            18190000 to "不败真言",
            18140000 to "集中防御",
            18160000 to "疾走真言",
            18130000 to "粉碎击",
            18330000 to "马尔库坦的愤怒",
            18240000 to "阻断权能",
            18230000 to "束缚烙印",
            18170000 to "快愈之手",
            18250000 to "疾风权能",
            18420000 to "守护祝福",
            18700000 to "强袭冲击",
            18710000 to "生命祝福",
            18720000 to "十字防御",
            18730000 to "保护阵",
            18740000 to "鼓起咒语",
            18750000 to "攻击准备",
            18760000 to "冲击命中",
            18770000 to "愤怒咒语",
            18780000 to "大地的承诺",
            18790000 to "生存意志",
            18800001 to "风之承诺",

            /*
        Ranger
         */
            14020000 to "狙击",
            14030000 to "连射",
            14040000 to "螺旋箭",
            14100000 to "疾风箭",
            14340000 to "速射",
            14130000 to "套索箭",
            14140000 to "锁足箭",
            14090000 to "标靶箭",
            14050000 to "刺针箭",
            14330000 to "箭矢乱射",
            14110000 to "狂风箭",
            14110001 to "狂风箭-1阶段",
            14110002 to "狂风箭-2阶段",
            14110003 to "狂风箭-Max",
            14170001 to "爆炸陷阱",
            14080000 to "撕裂箭",
            14070000 to "压制箭",
            14010000 to "瞄准箭",
            14010001 to "瞄准箭-1阶段",
            14010002 to "瞄准箭-2阶段",
            14010003 to "瞄准箭-Max",
            14260000 to "冲击解除",
            14370000 to "闪电箭",
            14720007 to "集中炮火",
            14770007 to "束缚之眼",
            14780008 to "近身射击",
            14800007 to "猎人之魂",
            14270000 to "箭矢风暴",
            14120000 to "偷袭踢",
            14180000 to "束缚陷阱",
            14150000 to "睡眠箭",
            14160000 to "封印箭",
            14350000 to "大自然的呼吸",
            14060000 to "狮鹫箭",
            14360000 to "爆炸箭",
            14700000 to "强袭强打",
            14200000 to "洞察之眼",

            /*
        Sorcerer
         */
            15210000 to "火焰箭",
            15030000 to "炸裂",
            15250000 to "烈火",
            15090000 to "冰链",
            15100000 to "寒气波动",
            15040000 to "火焰飞镖",
            15280002 to "严寒之风",
            15280003 to "严寒之风-首击",
            15050000 to "火焰爆发",
            15050007 to "火焰爆发(延迟伤害)",
            15010000 to "火焰乱射",
            15150000 to "冰结",
            15110000 to "寒冬束缚",
            15330000 to "寒冬幻影",
            15220000 to "冰结爆发",
            15310000 to "集中祈愿",
            15060000 to "地狱火焰",
            15060001 to "地狱火焰-1阶段",
            15060002 to "地狱火焰-2阶段",
            15060003 to "地狱火焰-Max",
            15060008 to "地狱火焰(火焰地带)",
            15240000 to "冲击解除",
            15340000 to "诅咒:枯木",
            15710008 to "火之标记",
            15720000 to "大地长袍",
            15730007 to "寒气召唤",
            15740000 to "火焰长袍",
            15760000 to "精气吸收",
            15770000 to "抵抗恩惠",
            15750000 to "寒气长袍",
            15780000 to "强化恩惠",
            15790000 to "重生契约",
            15800007 to "生气蒸发",
            15360000 to "神圣爆发",
            15160000 to "钢铁护盾",
            15400000 to "元素强化",
            15140000 to "诅咒:树木",
            15230000 to "冰雪护甲",
            15130000 to "灵魂冻结",
            15200000 to "寒气风暴",
            15390000 to "火焰屏障",
            15390002 to "火焰屏障-余火",
            15390008 to "火焰屏障-余火追加伤害",
            15300000 to "卢米尔的空间",
            15300001 to "深渊",
            15320007 to "延迟爆发",
            15120000 to "冰河强打",
            15700000 to "强袭轰炸",

            /*
        Templar
         */
            12010000 to "猛烈一击",
            12020000 to "会心一击",
            12030000 to "拼命一击",
            12440000 to "威胁的猛击",
            12040000 to "连续乱打",
            12060000 to "惩戒一击",
            12060005 to "惩戒一击(追加施放)",
            12130000 to "捕捉",
            12100000 to "盾牌强击",
            12240000 to "审判",
            12240009 to "审判(追加伤害)",
            12340000 to "闪光乱舞",
            12270000 to "衰弱的猛击",
            12350000 to "庇护一击",
            12430000 to "盾牌突击",
            12430001 to "盾牌突击-Max",
            12300000 to "歼灭",
            12090000 to "惩罚",
            12090001 to "惩罚-1阶段",
            12090002 to "惩罚-2阶段",
            12090003 to "惩罚-Max",
            12260000 to "冲击解除",
            12330000 to "生擒",
            12710000 to "体力强化",
            12720000 to "庇护之盾",
            12730001 to "惩戒护佑",
            12740000 to "铁壁防御",
            12750000 to "守护印",
            12760000 to "冲击命中",
            12770000 to "侮辱咆哮",
            12780000 to "激发",
            12790000 to "生存意志",
            12800000 to "痛苦阻断",
            12310000 to "主神的惩罚",
            12320000 to "内扎坎之盾",
            12110000 to "保护之盾",
            12120000 to "挑衅",
            12200000 to "平衡护甲",
            12190000 to "双重护甲",
            12070000 to "毁灭之盾",
            12230000 to "高洁护甲",
            12410000 to "处刑之剑",
            12250000 to "战友保护",
            12220000 to "逮捕",
            12420000 to "剑风",
            12700000 to "强袭猛击",

            /*
        Assassin
         */
            13010000 to "快速斩",
            13030000 to "绝魂斩",
            13040000 to "快捷斩",
            13100000 to "猛兽咆哮",
            13110000 to "猛兽后踢",
            13120000 to "猛兽横扫",
            13070000 to "暗袭",
            13060000 to "奇袭",
            13350000 to "心脏刺",
            13340000 to "风暴乱舞",
            13210000 to "旋风斩",
            13050000 to "闪光斩",
            13360000 to "渗透",
            13380000 to "暗击",
            13220000 to "阴影降落",
            13130000 to "纹样爆发",
            13260000 to "冲击解除",
            13330000 to "风暴斩",
            13710000 to "五感最大化",
            13720000 to "寻找破绽",
            13720005 to "分身攻击",
            13720006 to "分身攻击",
            13720007 to "分身攻击",
            13720008 to "分身攻击",
            13720009 to "分身攻击",
            13730000 to "涂毒",
            13730007 to "中毒",
            13740000 to "背后强击",
            13750000 to "强袭姿态",
            13760000 to "冲击命中",
            13770000 to "奇袭姿态",
            13780000 to "防御裂痕",
            13790000 to "重生契约",
            13800007 to "觉悟",
            13270000 to "猛兽尖牙",
            13390000 to "迅捷契约",
            13250000 to "烟雾弹",
            13080000 to "回避姿态",
            13280000 to "螺旋斩",
            13180000 to "阴影行走",
            13020000 to "暗剑投掷",
            13090000 to "暗剑追踪",
            13300000 to "特里尼埃尔的匕首",
            13230000 to "空中束缚",
            13240000 to "空中杀法",
            13310000 to "幻影分身",
            13370000 to "回避契约",
            13700000 to "强袭袭击"

        )

        val SKILL_CODES: IntArray =
            intArrayOf(
                100051,
                100055,
                17010000,
                17020000,
                17030000,
                17040000,
                17040007,
                17050000,
                17080000,
                17070000,
                17150002,
                17090000,
                17350000,
                17100000,
                17120000,
                17120001,
                17370000,
                17060000,
                17060001,
                17060002,
                17060003,
                17240000,
                17320000,
                17280000,
                17290000,
                17160000,
                17430000,
                17390000,
                17400000,
                17270000,
                17190000,
                17410000,
                17420000,
                17300000,
                17700000,
                17710000,
                17720000,
                17730001,
                17730002,
                17740000,
                17750000,
                17760000,
                17770000,
                17780000,
                17790000,
                17800000,
                11020000,
                11030000,
                11040000,
                11180000,
                11010000,
                11420000,
                11190000,
                11290000,
                11290001,
                11170000,
                11440000,
                11280000,
                11200000,
                11210000,
                11050000,
                11060000,
                11360000,
                11360001,
                11300000,
                11370000,
                11100000,
                11100001,
                11100002,
                11260000,
                11320000,
                11240000,
                11400000,
                11250000,
                11110000,
                11130000,
                11080000,
                11090000,
                11380000,
                11340000,
                11390000,
                11410000,
                11430000,
                11700000,
                11170037,
                11710000,
                11720000,
                11730000,
                11740000,
                11750000,
                11760000,
                11770000,
                11770007,
                11780000,
                11790000,
                11800008,
                16010000,
                16020000,
                16030000,
                16040000,
                16050000,
                16100004,
                16110004,
                16110005,
                16140000,
                16001104,
                16001108,
                16001110,
                16001113,
                16001117,
                16340000,
                16130001,
                16130005,
                16330000,
                16120002,
                16120005,
                16070000,
                16300000,
                16300001,
                16300002,
                16300003,
                16200000,
                16210000,
                16710000,
                16720000,
                16730001,
                16740001,
                16750000,
                16760000,
                16800001,
                16770000,
                16790000,
                16780000,
                16240000,
                16240001,
                16240002,
                16240003,
                16001301,
                16001305,
                16001309,
                16001313,
                16001317,
                16190000,
                16370000,
                16250000,
                16250001,
                16150000,
                16151100,
                16152100,
                16154100,
                16153100,
                16360000,
                16060000,
                16080000,
                16220000,
                16230000,
                16260000,
                16700000,
                18010000,
                18020000,
                18030000,
                18370000,
                18040000,
                18050000,
                18090000,
                18090001,
                18090002,
                18090003,
                18060000,
                18070000,
                18100000,
                18400000,
                18300000,
                18150000,
                18120000,
                18210000,
                18390000,
                18080000,
                18080001,
                18290000,
                18200000,
                18410000,
                18220000,
                18190000,
                18140000,
                18160000,
                18130000,
                18330000,
                18240000,
                18230000,
                18170000,
                18250000,
                18420000,
                18700000,
                18710000,
                18720000,
                18730000,
                18740000,
                18750000,
                18760000,
                18770000,
                18780000,
                18790000,
                18800001,
                14020000,
                14030000,
                14040000,
                14100000,
                14340000,
                14130000,
                14140000,
                14090000,
                14050000,
                14330000,
                14110000,
                14110001,
                14110002,
                14110003,
                14170001,
                14080000,
                14070000,
                14010000,
                14010001,
                14010002,
                14010003,
                14260000,
                14370000,
                14720007,
                14770007,
                14780008,
                14800007,
                14270000,
                14120000,
                14180000,
                14150000,
                14160000,
                14350000,
                14060000,
                14360000,
                14700000,
                14200000,
                15210000,
                15030000,
                15250000,
                15090000,
                15100000,
                15040000,
                15280002,
                15280003,
                15050000,
                15050007,
                15010000,
                15150000,
                15110000,
                15330000,
                15220000,
                15310000,
                15060000,
                15060001,
                15060002,
                15060003,
                15060008,
                15240000,
                15340000,
                15710008,
                15720000,
                15730007,
                15740000,
                15760000,
                15770000,
                15750000,
                15780000,
                15790000,
                15800007,
                15360000,
                15160000,
                15400000,
                15140000,
                15230000,
                15130000,
                15200000,
                15390000,
                15390002,
                15390008,
                15300000,
                15300001,
                15320007,
                15120000,
                15700000,
                12010000,
                12020000,
                12030000,
                12440000,
                12040000,
                12060000,
                12060005,
                12130000,
                12100000,
                12240000,
                12240009,
                12340000,
                12270000,
                12350000,
                12430000,
                12430001,
                12300000,
                12090000,
                12090001,
                12090002,
                12090003,
                12260000,
                12330000,
                12710000,
                12720000,
                12730001,
                12740000,
                12750000,
                12760000,
                12770000,
                12780000,
                12790000,
                12800000,
                12310000,
                12320000,
                12110000,
                12120000,
                12200000,
                12190000,
                12070000,
                12230000,
                12410000,
                12250000,
                12220000,
                12420000,
                12700000,
                13010000,
                13030000,
                13040000,
                13100000,
                13110000,
                13120000,
                13070000,
                13060000,
                13350000,
                13340000,
                13210000,
                13050000,
                13360000,
                13380000,
                13220000,
                13130000,
                13260000,
                13330000,
                13710000,
                13720000,
                13720005,
                13720006,
                13720007,
                13720008,
                13720009,
                13730000,
                13730007,
                13740000,
                13750000,
                13760000,
                13770000,
                13780000,
                13790000,
                13800007,
                13270000,
                13390000,
                13250000,
                13080000,
                13280000,
                13180000,
                13020000,
                13090000,
                13300000,
                13230000,
                13240000,
                13310000,
                13370000,
                13700000
            ).apply { sort() }
    }

    private val targetInfoMap = hashMapOf<Int, TargetInfo>()

    private var mode: Mode = Mode.BOSS_ONLY
    private var currentTarget: Int = 0
    @Volatile private var targetSelectionMode: TargetSelectionMode = TargetSelectionMode.MOST_DAMAGE

    fun setMode(mode: Mode) {
        this.mode = mode
        //모드 변경시 이전기록 초기화?
    }

    fun setTargetSelectionModeById(id: String?) {
        targetSelectionMode = TargetSelectionMode.fromId(id)
    }

    fun getDps(): DpsData {
        val pdpMap = dataStorage.getBossModeData()

        pdpMap.forEach { (target, data) ->
            var flag = false
            var targetInfo = targetInfoMap[target]
            if (!targetInfoMap.containsKey(target)) {
                flag = true
            }
            data.forEach { pdp ->
                if (flag) {
                    flag = false
                    targetInfo = TargetInfo(target, 0, pdp.getTimeStamp(), pdp.getTimeStamp())
                    targetInfoMap[target] = targetInfo!!
                }
                targetInfo!!.processPdp(pdp)
                //그냥 아래에서 재계산하는거 여기서 해놓고 아래에선 그냥 골라서 주는게 맞는거같은데 나중에 고민할필요있을듯
            }
        }
        val dpsData = DpsData()
        val targetDecision = decideTarget()
        dpsData.targetName = targetDecision.targetName
        dpsData.targetMode = targetDecision.mode.id

        currentTarget = targetDecision.trackingTargetId
        dataStorage.setCurrentTarget(currentTarget)

        val battleTime = when (targetDecision.mode) {
            TargetSelectionMode.ALL_TARGETS -> parseAllBattleTime(targetDecision.targetIds)
            else -> targetInfoMap[currentTarget]?.parseBattleTime() ?: 0
        }
        val nicknameData = dataStorage.getNickname()
        var totalDamage = 0.0
        if (battleTime == 0L) {
            return dpsData
        }
        val pdps = when (targetDecision.mode) {
            TargetSelectionMode.ALL_TARGETS -> collectAllPdp(pdpMap, targetDecision.targetIds)
            else -> pdpMap[currentTarget]?.toList() ?: return dpsData
        }
        pdps.forEach { pdp ->
            totalDamage += pdp.getDamage()
            val uid = dataStorage.getSummonData()[pdp.getActorId()] ?: pdp.getActorId()
            val nickname:String = nicknameData[uid]
                ?: nicknameData[dataStorage.getSummonData()[uid]?:uid]
                ?: uid.toString()
            if (!dpsData.map.containsKey(uid)) {
                dpsData.map[uid] = PersonalData(nickname = nickname)
            }
            pdp.setSkillCode(inferOriginalSkillCode(pdp.getSkillCode1()) ?: pdp.getSkillCode1())
            dpsData.map[uid]!!.processPdp(pdp)
            if (dpsData.map[uid]!!.job == "") {
                val origSkillCode = inferOriginalSkillCode(pdp.getSkillCode1()) ?: -1
                val job = JobClass.convertFromSkill(origSkillCode)
                if (job != null) {
                    dpsData.map[uid]!!.job = job.className
                }
            }
        }
        val iterator = dpsData.map.iterator()
        while (iterator.hasNext()) {
            val (_, data) = iterator.next()
            if (data.job == "") {
                iterator.remove()
            } else {
                data.dps = data.amount / battleTime * 1000
                data.damageContribution = data.amount / totalDamage * 100
            }
        }
        dpsData.battleTime = battleTime
        return dpsData
    }

    private fun decideTarget(): TargetDecision {
        if (targetInfoMap.isEmpty()) {
            return TargetDecision(emptySet(), "", targetSelectionMode, 0)
        }
        val mostDamageTarget = targetInfoMap.maxByOrNull { it.value.damagedAmount() }?.key ?: 0
        val mostRecentTarget = targetInfoMap.maxByOrNull { it.value.lastDamageTime() }?.key ?: 0

        return when (targetSelectionMode) {
            TargetSelectionMode.MOST_DAMAGE -> {
                TargetDecision(setOf(mostDamageTarget), resolveTargetName(mostDamageTarget), targetSelectionMode, mostDamageTarget)
            }
            TargetSelectionMode.MOST_RECENT -> {
                TargetDecision(setOf(mostRecentTarget), resolveTargetName(mostRecentTarget), targetSelectionMode, mostRecentTarget)
            }
            TargetSelectionMode.ALL_TARGETS -> {
                TargetDecision(targetInfoMap.keys.toSet(), "", targetSelectionMode, mostRecentTarget)
            }
        }
    }

    private fun resolveTargetName(target: Int): String {
        if (!dataStorage.getMobData().containsKey(target)) return ""
        val mobCode = dataStorage.getMobData()[target] ?: return ""
        return dataStorage.getMobCodeData()[mobCode] ?: ""
    }

    private fun parseAllBattleTime(targetIds: Set<Int>): Long {
        val targets = targetIds.mapNotNull { targetInfoMap[it] }
        if (targets.isEmpty()) return 0
        val start = targets.minOf { it.firstDamageTime() }
        val end = targets.maxOf { it.lastDamageTime() }
        return end - start
    }

    private fun collectAllPdp(
        pdpMap: Map<Int, Iterable<ParsedDamagePacket>>,
        targetIds: Set<Int>,
    ): List<ParsedDamagePacket> {
        val combined = mutableListOf<ParsedDamagePacket>()
        val seen = mutableSetOf<UUID>()
        targetIds.forEach { targetId ->
            pdpMap[targetId]?.forEach { pdp ->
                if (seen.add(pdp.getUuid())) {
                    combined.add(pdp)
                }
            }
        }
        return combined
    }

    private fun inferOriginalSkillCode(skillCode: Int): Int? {
        for (offset in POSSIBLE_OFFSETS) {
            val possibleOrigin = skillCode - offset
            if (SKILL_CODES.binarySearch(possibleOrigin) >= 0) {
                logger.debug("Inferred original skill code: {}", possibleOrigin)
                return possibleOrigin
            }
        }
        logger.debug("Failed to infer skill code")
        return null
    }

    fun resetDataStorage() {
        dataStorage.flushDamageStorage()
        targetInfoMap.clear()
        logger.info("Target damage accumulation reset")
    }

    fun analyzingData(uid: Int) {
        val dpsData = getDps()
        dpsData.map.forEach { (_, pData) ->
            logger.debug("-----------------------------------------")
            DebugLogWriter.debug(logger, "-----------------------------------------")
            logger.debug(
                "Nickname: {} job: {} total damage: {} contribution: {}",
                pData.nickname,
                pData.job,
                pData.amount,
                pData.damageContribution
            )
            DebugLogWriter.debug(
                logger,
                "Nickname: {} job: {} total damage: {} contribution: {}",
                pData.nickname,
                pData.job,
                pData.amount,
                pData.damageContribution
            )
            pData.analyzedData.forEach { (key, data) ->
                logger.debug(
                    "Skill (code): {} total damage: {}",
                    SKILL_MAP[key] ?: key,
                    data.damageAmount
                )
                DebugLogWriter.debug(
                    logger,
                    "Skill (code): {} total damage: {}",
                    SKILL_MAP[key] ?: key,
                    data.damageAmount
                )
                logger.debug(
                    "Uses: {} critical hits: {} critical hit rate: {}",
                    data.times,
                    data.critTimes,
                    data.critTimes / data.times * 100
                )
                DebugLogWriter.debug(
                    logger,
                    "Uses: {} critical hits: {} critical hit rate: {}",
                    data.times,
                    data.critTimes,
                    data.critTimes / data.times * 100
                )
                logger.debug(
                    "Skill damage share: {}%",
                    (data.damageAmount / pData.amount * 100).roundToInt()
                )
                DebugLogWriter.debug(
                    logger,
                    "Skill damage share: {}%",
                    (data.damageAmount / pData.amount * 100).roundToInt()
                )
            }
            logger.debug("-----------------------------------------")
            DebugLogWriter.debug(logger, "-----------------------------------------")
        }
    }

}
