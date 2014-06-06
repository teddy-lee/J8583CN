/*
this is a simple example for show how to use j8583CN .
Copyright (C) 2008 zyplanke

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
*/
//package j8583cn.example;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.util.Date;

import org.zyp.cn8583.cnMessage;
import org.zyp.cn8583.cnType;
import org.zyp.cn8583.cnMessageFactory;
import org.zyp.cn8583.impl.cnSimpleSystemTraceNumGen;
import org.zyp.cn8583.parse.cnConfigParser;

/** This little example program creates a message factory out of a XML config file,
 * creates a new message, and parses a couple of message from a text file.
 * 
 * @author zyplanke
 */
public class Example {

	public static void main(String[] args) throws Exception {
		// 配置
		cnMessageFactory mfact = cnConfigParser.createFromXMLConfigFile("config.xml");
		mfact.setUseCurrentDate(true);
		// 设置系统跟踪号的生成器（用于field 11）
		mfact.setSystemTraceNumberGenerator(new cnSimpleSystemTraceNumGen((int)(System.currentTimeMillis() % 100000)));
		
		//Create a new message
		cnMessage m = mfact.newMessagefromTemplate("0200");	// 根据模板创建并初始化一个报文对象
		m.setBinary(false);		// 对于域不使用二进制
		if(m.setMessageHeaderData(0, new String("0123456789").getBytes()) == false) {
			System.out.println("设置报文头出错。");
			System.exit(-1);
		}

		m.setValue(4, new BigDecimal("501.25"), cnType.AMOUNT, 0);
		m.setValue(12, new Date(), cnType.TIME, 0);
		m.setValue(15, new Date(), cnType.DATE4, 0);
		m.setValue(17, new Date(), cnType.DATE_EXP, 0);
		m.setValue(37, 12345678, cnType.NUMERIC, 12);
		m.setValue(41, "TEST-TERMINAL", cnType.ALPHA, 16);
		
		FileOutputStream fout = new FileOutputStream("messagedata.out");
		m.write(fout, 4, 10);	// 把报文写到文件，并在报文前，加上表示整个报文长度的四个数字字符(10进制表示)。
		fout.close();
		
		System.out.println("\n NEW MESSAGE:");
		print(m);
		
		// 下面解析一个报文串（该串存在文件中）	
		System.out.println("\n PARSE MESSAGE FROM FILE");
		byte[] buf = new byte[4];
		FileInputStream fin = new FileInputStream("messagedata.out");
		fin.read(buf);	// 读四个字节的数据（将报文长度信息读出）
		int len = buf[0] * 1000 + buf[1] * 100 + buf[2] * 10 + buf[3];
		buf = new byte[len];
		fin.read(buf);	// 从第五个字节读取len个自己的数据到buf中
		fin.close();
		
		mfact.setUseBinary(false);
		m = mfact.parseMessage(buf, mfact.getHeaderLengthAttr("0200"));	// 解析
		print(m);
		
	}

	// 输出一个报文内容
	private static void print(cnMessage m) {
		System.out.println("----------------------------------------------------- ");
		System.out.println("Message Header = [" + new String(m.getmsgHeader()) + "]");
		System.out.println("Message TypeID = [" +  m.getMsgTypeID() + "]");
		for (int i = 2; i < 128; i++) {
			if (m.hasField(i)) {
				System.out.println("FieldID: " + i 
									+ " <" + m.getField(i).getType() 
									+ ">\t[" + m.getObjectValue(i) 
									+ "]\t[" + m.getField(i).toString() + "]");
			}
		}
	}

}
