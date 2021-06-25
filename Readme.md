# Memory Shell

JavaWeb MemoryShell Inject/Scan/Killer/Protect Research & Exploring

文章：[JavaWeb 内存马一周目通关攻略](https://su18.org/post/memory-shell)



## 项目介绍

本项目用来学习和研究 JavaWeb 内存马添加和防御模式，共包含以下几个模块。

### memshell-test

模块 memshell-test 中，针对各个常用中间件实现了至少一种 Servlet-API 类型的内存马。

目前包含 apusic/bes/glassfish/inforsuite/jboss/jetty/resin/tomcat/tongweb/weblogic/websphere 等常用中间件的内存马写入测试文件，部分文件来自各位师傅们的分享，经修改和调整后已经全部经过测试。开箱即用。

在对应文件中，标注了对应的测试中间件版本信息，请对照测试。

由于重点关注内存马的写入方式，因此上下文的获取、关键类的定位这里没有讨论。

###memshell-inject 

模拟冰蝎的写入内存马测试项目。

### memshell-spring

spring controller 内存马以及 interceptor 内存马动态添加测试项目。

### memshell-loader && memshell-scanner

suagent 项目，使用 JavaAgent 技术来检测和防御内存马。



## SuAgent

使用 JavaAgent 技术配合 ASM 字节码编织，获取系统中全部加载的 class，并判断其是否为内存马，如果匹配检测逻辑，将插入字节码绕过内存马逻辑，达到防御内存马的目的。

使用方法：

- build 项目后会在 suagent 文件夹生成 suagent-loader.jar 以及 suagent-scanner.jar 两个文件。
- 使用 java -jar suagent-loader.jar 可列举出当前系统上的 JVM PID 列表。
- 使用  java -jar suagent-loader.jar attach 100 对指定 PID 进行 attach 注入，suagent 会自动对系统内 servlet-api 类型的内存马进行扫描和字节注入，可以在控制台下看到日志输出。
- 使用  java -jar suagent-loader.jar detach 100 移除 agent。



**测试视频：**

[![Memory Shell Test](https://res.cloudinary.com/marcomontalbano/image/upload/v1624592145/video_to_markdown/images/youtube--tTFv15uCNjQ-c05b58ac6eb4c4700831b2b3070cd403.jpg)](https://youtu.be/tTFv15uCNjQ "Memory Shell Test")


