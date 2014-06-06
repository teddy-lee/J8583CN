package org.zyp.cn8583.parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.zyp.cn8583.cnMessage;
import org.zyp.cn8583.cnType;
import org.zyp.cn8583.cnMessageFactory;

/**
 * 这是一个中国版的8583格式标准的类，初始代码来源于类ConfigParser  <P/>
 * This class is used to parse a XML configuration file and configure
 * a MessageFactory with the values from it.
 * 
 * @author zyplanke
 */
public class cnConfigParser {

	private final static Log log = LogFactory.getLog(cnConfigParser.class);

	/** Creates a message factory configured from the default file, which is j8583.xml
	 * located in the root of the classpath. 
	 * @deprecated use createFromXMLConfigFile(String) instead
	 */
	public static cnMessageFactory createDefault() throws IOException {
		if (cnMessageFactory.class.getClassLoader().getResource("j8583.xml") == null) {
			log.warn("j8583.xml not found, returning empty message factory");
			return new cnMessageFactory();
		} else {
			return createFromXMLConfigFile("j8583.xml");
		}
	}

	/** Creates a message factory from the specified path( full path and filename). */
	public static cnMessageFactory createFromXMLConfigFile(String filepath) throws IOException {
		// InputStream ins = cnMessageFactory.class.getClassLoader().getResourceAsStream(path);
		InputStream ins = new FileInputStream(filepath);
		cnMessageFactory mfact = new cnMessageFactory();
		if (ins != null) {
			if (log.isDebugEnabled()) {
				log.debug("Parsing config from xml file: [" + filepath + "]");
			}
			try {
				parse(mfact, ins);
			} finally {
				ins.close();
			}
		} else {
			log.warn("File not found in classpath: " + filepath);
		}
		return mfact;
	}

	/** Creates a message factory from the file located at the specified URL. */
	public static cnMessageFactory createFromUrl(URL url) throws IOException {
		cnMessageFactory mfact = new cnMessageFactory();
		InputStream stream = url.openStream();
		try {
			parse(mfact, stream);
		} finally {
			stream.close();
		}
		return mfact;
	}

	/** Reads the XML from the stream and configures the message factory with its values.
	 * @param mfact The message factory to be configured with the values read from the XML.
	 * @param stream The InputStream containing the XML configuration. */
	protected static void parse(cnMessageFactory mfact, InputStream stream) throws IOException {
		final DocumentBuilderFactory docfact = DocumentBuilderFactory.newInstance();
		DocumentBuilder docb = null;
		Document doc = null;
		try {
			docb = docfact.newDocumentBuilder();
			doc = docb.parse(stream);
		} catch (ParserConfigurationException ex) {
			log.error("Cannot parse XML configuration", ex);
			return;
		} catch (SAXException ex) {
			log.error("Parsing XML configuration", ex);
			return;
		}
		final Element root = doc.getDocumentElement();

		//Read the 8583 message configure headers
		NodeList nodes = root.getElementsByTagName("header");
		Element elem = null;
		for (int i = 0; i < nodes.getLength(); i++) {
			elem = (Element)nodes.item(i);
			int headerlen = Integer.parseInt(elem.getAttribute("length"));
			if (elem.getChildNodes() == null || elem.getChildNodes().getLength() == 0) {
				throw new IOException("Invalid header element");
			}
			String msgtypeid = elem.getChildNodes().item(0).getNodeValue();
			
			if (msgtypeid.length() != 4) {
				throw new IOException("Invalid msgtypeid for header: " + elem.getAttribute("msgtypeid"));
			}
			mfact.setHeaderLengthAttr(msgtypeid, headerlen);
			if (log.isTraceEnabled()) {
				log.trace("Adding 8583 header for msgtypeid: " + msgtypeid + "  length: " + headerlen);
			}		
		}

		//Read the message templates
		nodes = root.getElementsByTagName("template");
		for (int i = 0; i < nodes.getLength(); i++) {
			elem = (Element)nodes.item(i);
			String msgtypeid = elem.getAttribute("msgtypeid");
			if (msgtypeid.length() != 4) {
				throw new IOException("Invalid type for template: " + msgtypeid);
			}
			NodeList fields = elem.getElementsByTagName("field");
			cnMessage m = new cnMessage();
			m.setMsgTypeID(msgtypeid);
			for (int j = 0; j < fields.getLength(); j++) {
				Element f = (Element)fields.item(j);
				int fieldid = Integer.parseInt(f.getAttribute("id"));
				cnType datatype = cnType.valueOf(f.getAttribute("datatype"));
				int length = 0;
				if (f.getAttribute("length").length() > 0) {
					length = Integer.parseInt(f.getAttribute("length"));
				}
				String init_filed_data = f.getChildNodes().item(0).getNodeValue();
				m.setValue(fieldid, init_filed_data, datatype, length);
			}
			mfact.addMessageTemplate(m);
		}

		//Read the parsing guides
		nodes = root.getElementsByTagName("parseinfo");
		for (int i = 0; i < nodes.getLength(); i++) {
			elem = (Element)nodes.item(i);
			String msgtypeid = elem.getAttribute("msgtypeid");
			if (msgtypeid.length()!= 4) {
				throw new IOException("Invalid type for parse guide: " + msgtypeid);
			}
			NodeList fields = elem.getElementsByTagName("field");
			HashMap<Integer, cnFieldParseInfo> parseMap = new HashMap<Integer, cnFieldParseInfo>();
			for (int j = 0; j < fields.getLength(); j++) {
				Element f = (Element)fields.item(j);
				int fieldid = Integer.parseInt(f.getAttribute("id"));
				cnType datatype = cnType.valueOf(f.getAttribute("datatype"));
				int length = 0;
				if (f.getAttribute("length").length() > 0) {
					length = Integer.parseInt(f.getAttribute("length"));
				}
				parseMap.put(fieldid, new cnFieldParseInfo(datatype, length));
			}
			mfact.setParseMap(msgtypeid, parseMap);
		}

	}

}
