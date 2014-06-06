
package org.zyp.cn8583.impl;

import org.zyp.cn8583.cnSystemTraceNumGenerator;

/** 
 * 这是一个中国版的8583格式标准的类，初始代码来源于类SimpleTraceGenerator。
 * 取系统跟踪号的方法已经同步。
 * Simple implementation of a cnSystemTraceNumGenerator with an internal
 * number that is increased in memory but is not stored anywhere.
 * 
 * @author zyplanke
 */
public class cnSimpleSystemTraceNumGen implements cnSystemTraceNumGenerator {

	private int value = 0;

	/** Creates a new instance that will use the specified initial value. This means
	 * the first nextTrace() call will return this number.
	 * @param initialValue a number between 1 and 999999.
	 * @throws IllegalArgumentException if the number is less than 1 or greater than 999999. */
	public cnSimpleSystemTraceNumGen(int initialValue) {
		if (initialValue < 1 || initialValue > 999999) {
			throw new IllegalArgumentException("Initial value must be between 1 and 999999");
		}
		value = initialValue - 1;
	}

	public synchronized int getLastTrace() {
		return value;
	}

	/** Returns the next number in the sequence. This method is synchronized, because the counter
	 * is incremented in memory only. */
	public synchronized int nextTrace() {
		value++;
		if (value > 999999) {
			value = 1;
		}
		return value;
	}

}
