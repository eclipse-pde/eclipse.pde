package org.eclipse.pde.internal.core;

import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;

/**
 * @author melhem
 *
 */
public class XMLDefaultHandler extends DefaultHandler implements LexicalHandler {
	
	private org.w3c.dom.Document fDocument;
	private Locator fLocator;
	private Hashtable fLineTable;
	private Element fRootElement;
	
	private Stack fElementStack = new Stack();
	
	public XMLDefaultHandler() {
		fLineTable = new Hashtable();
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes)
		throws SAXException {
		Element element = fDocument.createElement(qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			element.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}
		
		Integer lineNumber = new Integer(fLocator.getLineNumber());
		Integer[] range = new Integer[] {lineNumber, new Integer(-1)};
		fLineTable.put(element, range);
		if (fRootElement == null)
			fRootElement = element;
		else 
			((Element)fElementStack.peek()).appendChild(element);
		fElementStack.push(element);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		Integer[] range = (Integer[])fLineTable.get(fElementStack.pop());
		range[1] = new Integer(fLocator.getLineNumber());
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			fDocument = factory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
		}
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		fDocument.appendChild(fRootElement);
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data) throws SAXException {
		fDocument.appendChild(fDocument.createProcessingInstruction(target, data));
	}
	
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char[] characters, int start, int length) throws SAXException {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < length; i++) {
			buff.append(characters[start + i]);
		}
		Text text = fDocument.createTextNode(buff.toString());
		if (fRootElement == null)
			fDocument.appendChild(text);
		else 
			((Element)fElementStack.peek()).appendChild(text);
	}
	
	public Node getDocumentElement() {
		fDocument.getDocumentElement().normalize();
		return fDocument.getDocumentElement();
	}
	
	public org.w3c.dom.Document getDocument() {
		fDocument.getDocumentElement().normalize();
		return fDocument;
	}
	
	public Hashtable getLineTable() {
		return fLineTable;
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.ext.LexicalHandler#endCDATA()
	 */
	public void endCDATA() throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ext.LexicalHandler#endDTD()
	 */
	public void endDTD() throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ext.LexicalHandler#startCDATA()
	 */
	public void startCDATA() throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ext.LexicalHandler#comment(char[], int, int)
	 */
	public void comment(char[] ch, int start, int length) throws SAXException {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < length; i++) {
			buffer.append(ch[start + i]);
		}
		Comment comment = fDocument.createComment(buffer.toString());
		if (fRootElement == null || fElementStack.isEmpty())
			fDocument.appendChild(comment);
		else 
			((Element)fElementStack.peek()).appendChild(comment);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ext.LexicalHandler#endEntity(java.lang.String)
	 */
	public void endEntity(String name) throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ext.LexicalHandler#startEntity(java.lang.String)
	 */
	public void startEntity(String name) throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ext.LexicalHandler#startDTD(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
	}
	
}
