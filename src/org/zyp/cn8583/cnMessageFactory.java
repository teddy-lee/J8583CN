package org.zyp.cn8583;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.zyp.cn8583.parse.cnFieldParseInfo;


/**
 * 这是一个中国版的8583格式标准的类，初始代码来源于MessageFactory类。
 * This class is used to create messages, either from scratch or from an existing String or byte
 * buffer. It can be configured to put default values on newly created messages, and also to know
 * what to expect when reading messages from an InputStream.
 * <P>
 * The factory can be configured to know what values to set for newly created messages, both from
 * a template (useful for fields that must be set with the same value for EVERY message created)
 * and individually (for trace [field 11] and message date [field 7]).
 * <P>
 * It can also be configured to know what fields to expect in incoming messages (all possible values
 * must be stated, indicating the date type for each). This way the messages can be parsed from
 * a byte buffer.
 * 
 * @author zyplanke
 *
 */
public class cnMessageFactory  {
	protected static final Log log = LogFactory.getLog(cnMessageFactory.class);

	/** This map stores the message template for each message msgtypeid. (msgtypeid, Message)*/
	private Map<String, cnMessage> typeTemplates = new HashMap<String, cnMessage>();
	/** Stores the information needed to parse messages sorted by type. (msgtypeid, (fieldID, fieldInfo))*/
	private Map<String, Map<Integer, cnFieldParseInfo>> parseMap = new HashMap<String, Map<Integer, cnFieldParseInfo>>();
	/** Stores the field numbers to be parsed, in order of appearance. (msgtypeid, fieldID)*/
	private Map<String, List<Integer>> parseOrder = new HashMap<String, List<Integer>>();

	private cnSystemTraceNumGenerator SystraceNumGen;
	/** The 8583 header to be included in each message msgtypeid. (msgtypeid, headerlength)*/
	private Map<String, Integer> msgheadersattr = new HashMap<String, Integer>();
	/** Indicates if the current date should be set on new messages (field 7). */
	private boolean usecurrentdata;
	/** Indicates if the factory should create binary messages and also parse binary messages. */
	private boolean useBinary;
	private int etx = -1;

	/** Tells the receiver to create and parse binary messages if the flag is true.
	 * Default is false, that is, create and parse ASCII messages. */
	public void setUseBinary(boolean flag) {
		useBinary = flag;
	}
	/** Returns true is the factory is set to create and parse binary messages,
	 * false if it uses ASCII messages. Default is false. */
	public boolean getUseBinary() {
		return useBinary;
	}

	/** Sets the ETX character to be sent at the end of the message. This is optional and the
	 * default is -1, which means nothing should be sent as terminator.
	 * @param value The ASCII value of the ETX character or -1 to indicate no terminator should be used. */
	public void setEtx(int value) {
		etx = value;
	}

	/** Creates a new message of the specified type id from message template. If the factory is set to use binary
	 * messages, then the returned message will be written using binary coding.
	 * @param msgtypeid The message type id, 应为4个字节字符*/
	public cnMessage newMessagefromTemplate(String msgtypeid) {
		cnMessage m = new cnMessage(msgtypeid, msgheadersattr.get(msgtypeid));
		m.setEtx(etx);
		m.setBinary(useBinary);

		//Copy the values from the template (通过报文模板来赋初值)
		cnMessage templ = typeTemplates.get(msgtypeid);
		if (templ != null) {
			for (int i = 2; i < 128; i++) {
				if (templ.hasField(i)) {
					m.setField(i, templ.getField(i).clone());
				}
			}
		}
		if (SystraceNumGen != null) {
			m.setValue(11, SystraceNumGen.nextTrace(), cnType.NUMERIC, 6);
		}
		if (usecurrentdata) {
			m.setValue(7, new Date(), cnType.DATE10, 10);
		}
		return m;
	}

	/** Creates a message to respond to a request. <P/>
	 * 根据请求报文生产回应报文 (回应报文的标示的第三位为：请求报文第三位加一) <P/>
	 * sets all fields from the template if there is one, and copies all values from the request,
	 * overwriting fields from the template if they overlap.
	 * @param request An 8583 message with a request type (ending in 00). */
	public cnMessage createResponse(cnMessage request) {
		String resptypeid = request.getMsgTypeID().substring(0, 2)
							+ Integer.toString(Integer.parseInt(request.getMsgTypeID().substring(2,3)) + 1)
							+ request.getMsgTypeID().substring(3, 4);
		cnMessage resp = new cnMessage(resptypeid, msgheadersattr.get(resptypeid));
	
		resp.setBinary(request.isBinary());
		resp.setEtx(etx);
		//Copy the values from the template
		cnMessage templ = typeTemplates.get(resp.getMsgTypeID());
		if (templ != null) {
			for (int i = 2; i < 128; i++) {
				if (templ.hasField(i)) {
					resp.setField(i, templ.getField(i).clone());
				}
			}
		}
		// copy the values from request message
		for (int i = 2; i < 128; i++) {
			if (request.hasField(i)) {
				resp.setField(i, request.getField(i).clone());
			}
		}
		return resp;
	}

	/**
	 * Creates a new message instance from the buffer, which must contain a
	 * valid 8583 message. If the factory is set to use binary messages then it
	 * will try to parse a binary message.
	 * 
	 * @param buf
	 *            The byte buffer containing the message. Must not include the
	 *            length header.
	 * @param msgheaderlength
	 *            The expected length of the 8583 header, after which the
	 *            message type id and the rest of the message must come.
	 */
	public cnMessage parseMessage(byte[] buf, int msgheaderlength)
			throws ParseException {
		cnMessage m = new cnMessage(new String(buf, msgheaderlength, 4),
				msgheaderlength);
		// TODO it only parses ASCII messages for now

		// 得到报文头
		m.setMessageHeaderData(0, Arrays.copyOfRange(buf, 0, msgheaderlength));

		// Parse the bitmap (primary first)
		BitSet bs = new BitSet(64);
		int pos = 0;
		for (int i = msgheaderlength + 4; i < msgheaderlength + 12; i++) {
			int bit = 128;
			for (int b = 0; b < 8; b++) {
				bs.set(pos++, (buf[i] & bit) != 0);
				bit >>= 1;
			}
		}

		// Check for secondary bitmap and parse if necessary
		if (bs.get(0)) {
			for (int i = msgheaderlength + 12; i < msgheaderlength + 20; i++) {
				int bit = 128;
				for (int b = 0; b < 8; b++) {
					bs.set(pos++, (buf[i] & bit) != 0);
					bit >>= 1;
				}
			}
			pos = 20 + msgheaderlength;
		} else {
			pos = 12 + msgheaderlength;
		}

		//Parse each field
		Map<Integer, cnFieldParseInfo> parseGuide = parseMap.get(m.getMsgTypeID());
		List<Integer> index = parseOrder.get(m.getMsgTypeID());	// 该类型报文应该存在的域ID集合
		for (Integer i : index) {
			cnFieldParseInfo fpi = parseGuide.get(i);
			if (bs.get(i - 1)) {
				cnValue val = useBinary ? fpi.parseBinary(buf, pos) : fpi.parse(buf, pos);
				m.setField(i, val);
				if (useBinary && !(val.getType() == cnType.ALPHA || val.getType() == cnType.LLVAR
						|| val.getType() == cnType.LLLVAR)) {
					pos += (val.getLength() / 2) + (val.getLength() % 2);
				} else {
					pos += val.getLength();
				}
				if (val.getType() == cnType.LLVAR) {
					pos += useBinary ? 1 : 2;
				} else if (val.getType() == cnType.LLLVAR) {
					pos += useBinary ? 2 : 3;
				}
			}
		}
		return m;
	}

	/** Sets whether the factory should set the current date on newly created messages,
	 * in field 7. Default is false. */
	public void setUseCurrentDate(boolean flag) {
		usecurrentdata = true;
	}
	/** Returns true if the factory is assigning the current date to newly created messages
	 * (field 7). Default is false. */
	public boolean getUseCurrentDate() {
		return usecurrentdata;
	}

	/** Sets the generator that this factory will get new trace numbers from. There is no
	 * default generator. */
	public void setSystemTraceNumberGenerator(cnSystemTraceNumGenerator value) {
		SystraceNumGen = value;
	}
	/** Returns the generator used to assign trace numbers to new messages. */
	public cnSystemTraceNumGenerator getSystemTraceNumberGenerator() {
		return SystraceNumGen;
	}

	/** Sets the 8583 header to be used in each message type.
	 * @param value A map where the keys are the message type id and the values are the message headers length.
	 */
	public void setHeaders(Map<String, Integer> value) {
		msgheadersattr.clear();
		msgheadersattr.putAll(value);
	}

	/** Sets the 8583 header attr for a specific message type.
	 * @param msgtypeid The message type( 4 bytes)
	 * @param headerlen The message header length */
	public void setHeaderLengthAttr(String msgtypeid, Integer headerlen) {
			msgheadersattr.put(msgtypeid, headerlen);
	}

	/** Returns the 8583 header length for the specified type. */
	public Integer getHeaderLengthAttr(String msgtypeid) {
		return msgheadersattr.get(msgtypeid);
	}

	/** Adds a message template to the factory. If there was a template for the same
	 * message type id as the new one, it is overwritten. */
	public void addMessageTemplate(cnMessage templ) {
		if (templ != null) {
			typeTemplates.put(templ.getMsgTypeID(), templ);
		}
	}

	/** Removes the message template for the specified message type id. */
	public void removeMessageTemplate(String msgtypeid) {
		typeTemplates.remove(msgtypeid);
	}

	/** Sets a message template for a specified message type. When new messages of that type
	 * are created, they will have the same values as the template.
	 * @param msgtypeid The message type id.
	 * @param templ The message from which fields should be copied, or NULL to remove the
	 * template for this message type.
	 * @deprecated Use addMessageTemplate(cnMessage) and removeMessageTemplate(String) instead of this. */
	public void setMessageTemplate(String msgtypeid, cnMessage templ) {
		if (templ == null) {
			typeTemplates.remove(msgtypeid);
		} else {
			typeTemplates.put(msgtypeid, templ);
		}
	}

	/** Sets a map with the fields that are to be expected when parsing a certain type of
	 * message.
	 * @param msgtypeid The message type id.
	 * @param map A map of FieldParseInfo instances, each of which define what type and length
	 * of field to expect. The keys will be the field numbers. */
	public void setParseMap(String msgtypeid, Map<Integer, cnFieldParseInfo> map) {
		parseMap.put(msgtypeid, map);
		ArrayList<Integer> index = new ArrayList<Integer>();
		index.addAll(map.keySet());
		Collections.sort(index);
		log.trace("Adding parse map for type: [" + msgtypeid + "] with fields " + index);
		parseOrder.put(msgtypeid, index);
	}

}
