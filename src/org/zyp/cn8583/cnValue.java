package org.zyp.cn8583;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 这是一个中国版的8583格式标准的类，初始源代码来自于IsoValue。
 * Represents a value that is stored in a field inside a china 8583 message.
 * It can format the value when the message is generated.
 * Some values have a fixed length, other values require a length to be specified
 * so that the value can be padded to the specified length. LLVAR and LLLVAR
 * values do not need a length specification because the length is calculated
 * from the stored value.
 * 
 * @author zyplanke
 */
public class cnValue<T> {
	private cnType datatype;
	private T value;
	private int length;

	/** Creates a new instance that stores the specified value as the specified type.
	 * Useful for storing LLVAR or LLLVAR types, as well as fixed-length value types
	 * like DATE10, DATE4, AMOUNT, etc.
	 * @param t the cnType.
	 * @param value The value to be stored. */
	public cnValue(cnType t, T value) {
		if (t.needsLength()) {
			throw new IllegalArgumentException("Fixed-value types must use constructor that specifies length");
		}
		datatype = t;
		this.value = value;
		if (datatype == cnType.LLVAR || datatype == cnType.LLLVAR) {
			length = value.toString().length();
			if (t == cnType.LLVAR && length > 99) {
				throw new IllegalArgumentException("LLVAR can only hold values up to 99 chars");
			} else if (t == cnType.LLLVAR && length > 999) {
				throw new IllegalArgumentException("LLLVAR can only hold values up to 999 chars");
			}
		} else {
			length = datatype.getLength();
		}
	}

	/** Creates a new instance that stores the specified value as the specified type.
	 * Useful for storing fixed-length value types. */
	public cnValue(cnType t, T val, int len) {
		datatype = t;
		value = val;
		length = len;
		if (length == 0 && t.needsLength()) {
			throw new IllegalArgumentException("Length must be greater than zero");
		} else if (t == cnType.LLVAR || t == cnType.LLLVAR) {
			length = val.toString().length();
			if (t == cnType.LLVAR && length > 99) {
				throw new IllegalArgumentException("LLVAR can only hold values up to 99 chars");
			} else if (t == cnType.LLLVAR && length > 999) {
				throw new IllegalArgumentException("LLLVAR can only hold values up to 999 chars");
			}
		}
	}

	/** Returns the cnType to which the value must be formatted. */
	public cnType getType() {
		return datatype;
	}

	/** Returns the length of the stored value, of the length of the formatted value
	 * in case of NUMERIC or ALPHA. It doesn't include the field length header in case
	 * of LLVAR or LLLVAR. */
	public int getLength() {
		return length;
	}

	/** Returns the stored value without any conversion or formatting. */
	public T getValue() {
		return value;
	}

	/** Returns the formatted value as a String. The formatting depends on the type of the
	 * receiver. */
	public String toString() {
		if (value == null) {
			return "FieldValue<null>";
		}
		if (datatype == cnType.NUMERIC || datatype == cnType.AMOUNT) {
			if (datatype == cnType.AMOUNT) {
				return datatype.format((BigDecimal)value, 12);
			} else if (value instanceof Number) {
				return datatype.format(((Number)value).longValue(), length);
			} else {
				return datatype.format(value.toString(), length);
			}
		} else if (datatype == cnType.ALPHA) {
			return datatype.format(value.toString(), length);
		} else if (datatype == cnType.LLLVAR || datatype == cnType.LLLVAR) {
			return value.toString();
		} else if (value instanceof Date) {
			return datatype.format((Date)value);
		}
		return value.toString();
	}

	/** Returns a copy of the receiver that references the same value object. */
	@SuppressWarnings("unchecked")
	public cnValue<T> clone() {
		return (cnValue<T>)(new cnValue(this.datatype, this.value, this.length));

	}

	/** Returns true of the other object is also an cnValue and has the same type and length,
	 * and if other.getValue().equals(getValue()) returns true. */
	public boolean equals(Object other) {
		if (other == null || !(other instanceof cnValue)) {
			return false;
		}
		cnValue comp = (cnValue)other;
		return (comp.getType() == getType() && comp.getValue().equals(getValue()) && comp.getLength() == getLength());
	}

	/** Writes the formatted value to a stream, with the length header
	 * if it's a variable length type. */
	public void write(OutputStream outs, boolean binary) throws IOException {
		if (datatype == cnType.LLLVAR || datatype == cnType.LLVAR) {
			if (binary) {
				if (datatype == cnType.LLLVAR) {
					outs.write(length / 100); //00 to 09 automatically in BCD
				}
				//BCD encode the rest of the length
				outs.write((((length % 100) / 10) << 4) | (length % 10));
			} else {
				//write the length in ASCII
				if (datatype == cnType.LLLVAR) {
					outs.write((length / 100) + 48);
				}
				if (length >= 10) {
					outs.write(((length % 100) / 10) + 48);
				} else {
					outs.write(48);
				}
				outs.write((length % 10) + 48);
			}
		} else if (binary) {
			//numeric types in binary are coded like this
			byte[] buf = null;
			if (datatype == cnType.NUMERIC) {
				buf = new byte[(length / 2) + (length % 2)];
			} else if (datatype == cnType.AMOUNT) {
				buf = new byte[6];
			} else if (datatype == cnType.DATE10 || datatype == cnType.DATE4 || datatype == cnType.DATE_EXP || datatype == cnType.TIME) {
				buf = new byte[length / 2];
			}
			//Encode in BCD if it's one of these types
			if (buf != null) {
				toBcd(toString(), buf);
				outs.write(buf);
				return;
			}
		}
		//Just write the value as text
		outs.write(toString().getBytes());
	}

	/** Encode the value as BCD and put it in the buffer. The buffer must be big enough
	 * to store the digits in the original value (half the length of the string). */
	private void toBcd(String value, byte[] buf) {
		int charpos = 0; //char where we start
		int bufpos = 0;
		if (value.length() % 2 == 1) {
			//for odd lengths we encode just the first digit in the first byte
			buf[0] = (byte)(value.charAt(0) - 48);
			charpos = 1;
			bufpos = 1;
		}
		//encode the rest of the string
		while (charpos < value.length()) {
			buf[bufpos] = (byte)(((value.charAt(charpos) - 48) << 4)
					| (value.charAt(charpos + 1) - 48));
			charpos += 2;
			bufpos++;
		}
	}

}
