# AION2Meter4J

用于Aion2战斗分析的DPS计量工具项目

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![GitHub Issues](https://img.shields.io/github/issues/TK-open-public/Aion2-Dps-Meter)](https://github.com/TK-open-public/Aion2-Dps-Meter/issues)
[![GitHub Pull Requests](https://img.shields.io/github/issues-pr/TK-open-public/Aion2-Dps-Meter)](https://github.com/TK-open-public/Aion2-Dps-Meter/pulls)

如果该项目收到运营方请求、数据包加密措施或官方宣布停止使用，则会停止并转为私有状态。

## 目录

- [构建](#构建)
- [使用方法](#使用方法)
- [UI说明](#ui-说明)
- [常见问题](#常见问题)
- [下载](#下载)

## 构建

```bash
# 复制仓库
git clone https://github.com/TK-open-public/Aion2-Dps-Meter.git

# 进入目录
cd Aion2-Dps-Meter

# 构建msi安装包
./gradlew packageDistributionForCurrentOS
```

## 使用方法

### 上述构建方法与普通用户无关。

1. 安装npcap (https://npcap.com/#download)。（必须勾选"Install Npcap in WinPcap API-compatible Mode"）
2. [前往](https://github.com/TK-open-public/Aion2-Dps-Meter/releases) 该链接下载aion2meter4j-x.x.x.msi并安装。
3. **如果Aion已启动，请先返回到角色选择界面**。
4. 在程序安装位置（默认路径为C:\Program Files\aion2meter4j）找到aion2meter4j.exe，**必须以管理员权限运行**。

![image](./readme-asset/firstUI.png)

5. 如果显示上述UI则表示成功运行。
6. 如果DPS计未显示，可以尝试移动到凯布利斯克或基地进行测试（包括在地下城内移动到凯布利斯克），或重复步骤3和4。
7. 如果之前正常工作但某时刻突然停止工作，按照第6步同样方法通过移动到凯布利斯克或进入地下城等方式，大概率会重新接收数据。如果仍然无法工作，请从第3步重新开始。

## UI说明

<br />

![image](./readme-asset/uiDesc.png)

<br />

蓝色框 - 显示怪物名称的位置（计划中）

棕色框 - 初始化当前记录。每个BOSS结束后，在进入下一场BOSS战斗前需要初始化。

粉色框 - 展开或折叠DPS。不想看DPS计时很有用。

红色框 - 当成功推断出该玩家的职业时，显示职业图标的位置。

橙色框 - 显示玩家昵称的位置。点击时会打开战斗详情窗口。

天蓝色框 - 基于当前计算中的怪物输出DPS的位置。

紫色框 - 基于当前计算中的怪物输出百分比贡献度的位置。

绿色框 - 显示战斗时间的地方。
战斗中时显示为绿色。
<br />
没有检测到伤害时变为黄色，并且计时器停止。
<br />
黄灯持续一定时间后判断为战斗结束，结束时显示为灰色并且计时器停止。

<br />

![image](./readme-asset/battleAnalyze_2.png)


<br />

点击DPS计可以看到各个用户的详细信息。
<br />
攻击次数不是施放次数，而是命中次数。
如果一个技能施放一次能攻击三次，则命中次数会显示为3次。

## 常见问题

- UI显示出来了，但是自己或其他人的伤害完全不显示。
  - 请确认npcap是否正确安装
  - 完全退出DPS计，在角色选择界面确认运行后再进入世界

- 打木桩，但除了我的DPS只显示我旁边的人的DPS
  - 根据当前收集的伤害中受到最多伤害的怪物来显示DPS。请打同一个木桩，或使用军团木桩或凶猛暗窟入口前的木桩

<!-- - 同样的木桩，但打的人的名字没全部显示
  - DPS计未能成功收集角色名。请在角色选择界面重新运行DPS计
  - 如果昵称是单个英文字母可能不会显示。 -->

- DPS计上只显示我一个人，但贡献度不是100%
  - 可能存在DPS计未能成功收集到角色名而未显示的贡献者。

- 有命令功能吗？
  - 目前不支持命令功能。将来可能会支持，但开发优先级较低。

- 战斗详情中命中次数比我使用的技能次数多
  - 命中次数就是字面意思的命中次数。如果一个技能施放一次攻击三次，即使只施放一次技能，命中次数也会显示为3次

- 技能名称显示为奇怪的数字
  - 通常这是神石。如果不是神石而是看起来像技能的数据以数字形式显示技能名称，请在issue中留言

## 下载

### [前往](https://github.com/TK-open-public/Aion2-Dps-Meter/releases)

即使有玩家输出不佳也请不要抱怨，请抱着“可能就是这样”的心态看待

使用该程序所产生的责任由用户本人承担。
