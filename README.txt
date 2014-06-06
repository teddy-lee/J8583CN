这是J8583CN的说明：
	(C) 2008  zyplanke 
	
J8583CN,是8583报文格式在中国的事实标准的JAVA实现。(很多地方参考了中国银联2.0和某些商行的标准,本程序在某商行各系统的测试项目中使用通过)
由于原来J8583和中国的实际标准在某些地方不完全一致，所以在J8583 1.2的基础上进行了改进。为了尽可能的保留利用J8583原来的代码，这此的原始代码来源于J8583，类结构基本不变，为区分，新类名以cn开头。

主要变化如下:
 1、   所有的初始代码均来自于J8583 1.2版，对类名名字和部分方法名进行了改变。
 2、   为方便使用，将Jakarta Commons Logging 1.1.1 一起打包，使用时不必再次下载。所有的代码用JavaSE6进行编译，因此要求至少JVM5或以上环境运行(IDE is Eclipse 3.4.0 and subversion 1.5.1)。
 3、   将cnMessageFactory类中createFromClasspathConfig改为createFromXMLConfigFile，并要求传入参数为XML文件的绝对路径（或运行时的相对路径）。
 4、   原来的报文类型标识符以数字表示，先改成字符串（按规范应为4字节串，但不强制，以方便做异常测试）
 5、   修改了XML配置文件（DTD文件也相应进行了修改）(新的配置用法见 《XML配置说明.rtf》)。
 6、   将报文头做了改进，由于报文头是可以自己定义的，所以报文头现在改成字节串，根据实际对报文头的要求按进行灵活设置，见相关API文档
 7、   请求/回应报文的报文标识按8583规范：回应报文为请求报文标识第三位加一（比如请求为0200，回应为0210）。原j8583把报文标示按16进制处理（相差16），先改成字符串，所以相应的也做了变更。
 8、   把位图部分做了改进，现在位图只能为8个或16个字节（二进制64bit或128bit，不存在非二进制的问题）
 9、   保留了原J8583中对报文是否二进制拼串的属性和方法，但现在这个属性的值，只控制各个报文域的是否以二进制，而报文头、报文类型标识、位图等不受影响。
 10、在cnMessage类增加了估算整个报文长度的方法，以方便特殊地方需要（如在银联2.0标准中报文头的第三域）
 
由上述变化而导致其他地方相应的改变，再此不一一说明。
对于使用者，可以不用了解原来的J8583,而直接用本J8583CN即可。示例参见example中的源代码。	

感谢j8583的原作者 Enrique Zamudio。 本人E-Mail：zypmail8@163.com


*************************************************************
follow is original README.txt of J8583 package
---------------------------------------------------------
j8583

(C)2007 Enrique Zamudio

The purpose of this library is to simplify the creation, transmission, and parsing of ISO8583 messages.

This software is LGPL. The license is included in LGPL.txt or can be viewed at http://www.fsf.org/licensing/licenses/lgpl.html

This software was developed using Eclipse 3.2.2. An ant build file is included to build the framework JAR and the example JAR.

REQUIREMENTS
This project requires Java 5 or higher. Set the compiler preferences in Eclipse to have 1.5 compliance.

DEPENDENCIES
Jakarta Commons Logging 1.1 is used for logging. A log4j.properties file is included in the source, for easy Log4J configuration and use to run the examples. These libraries are not included here but can be downloaded from http://jakarta.apache.org/commons/ and http://logging.apache.org/log4j/

Enrique Zamudio
chochos@sourceforge.net
