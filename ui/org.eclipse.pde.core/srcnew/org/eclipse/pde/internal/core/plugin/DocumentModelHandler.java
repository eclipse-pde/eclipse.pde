package org.eclipse.pde.internal.core.plugin;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

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
public class DocumentModelHandler extends DefaultHandler implements LexicalHandler {
	
	private org.w3c.dom.Document fDocument;
	private IDocument fTextDocument;
	private FindReplaceDocumentAdapter fFindReplaceAdapter;
	private Locator fLocator;
	private Hashtable fLineTable;
	private Element fRootElement;
	private IDocumentNode fModelRoot;
	private String fSchemaVersion;
	
	private Stack fDocumentNodeStack = new Stack();
	
	
	public DocumentModelHandler(InputStream stream) {
		try {
			createTextDocument(stream);
		} catch (Exception e) {
		}
		fLineTable = new Hashtable();
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes)
		throws SAXException {
		Element element = createDOMElement(uri, localName, qName, attributes);
		IDocumentNode node = new PluginDocumentNode(element);
		
		if (fRootElement == null || fDocumentNodeStack.isEmpty()) {
			fRootElement = element;
			fModelRoot = node;
			fDocument.appendChild(fRootElement);
		} else {
			PluginDocumentNode parent = (PluginDocumentNode)fDocumentNodeStack.peek();
			parent.getDOMNode().appendChild(element);
			parent.addChild(node);
			node.setParent(parent);
		}
		fLineTable.put(node, createRange(qName));		
		fDocumentNodeStack.push(node);
	}
	
	private Element createDOMElement(String uri, String localName, String qName, Attributes attributes) {
		Element element = fDocument.createElement(qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			element.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}
		return element;
	}
	
	private ISourceRange createRange(String name) {
		SourceRange range = new SourceRange();
		try {
			int offset = getStartOffset(name);
			range.setOffset(offset);
			range.setStartLine(fTextDocument.getLineOfOffset(offset) + 1);
		} catch (BadLocationException e) {
		}
		return range;
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		ISourceRange range = (ISourceRange)fLineTable.get(fDocumentNodeStack.pop());
		int line = fLocator.getLineNumber();
		range.setEndLine(line);
		try {
			range.setLength(fTextDocument.getLineOffset(line) - range.getOffset());
		} catch (BadLocationException e) {
		}
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
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data) throws SAXException {
		ProcessingInstruction instruction = fDocument.createProcessingInstruction(target, data);
		if ("eclipse".equals(target)) {
			fSchemaVersion = "3.0";
		}
		fDocument.appendChild(instruction);
	}
	
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char[] characters, int start, int length) throws SAXException {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < length; i++) {
			buff.append(characters[start + i]);
		}
		if (buff.length() > 0) {
			Text text = fDocument.createTextNode(buff.toString());
			if (fRootElement == null)
				fDocument.appendChild(text);
			else {
				PluginDocumentNode node =
					((PluginDocumentNode) fDocumentNodeStack.peek());
				node.getDOMNode().appendChild(text);
			}
		}
	}
	
	public Node getDocumentElement() {
		if (fDocument != null && fDocument.getDocumentElement() != null) {
			fDocument.getDocumentElement().normalize();
			return fDocument.getDocumentElement();
		}
		return null;
	}
	
	public org.w3c.dom.Document getDocument() {
		return fDocument;
	}
	
	public Hashtable getLineTable() {
		return fLineTable;
	}
	
	private void createTextDocument(InputStream stream) {
		try {
			BufferedReader reader =
			new BufferedReader(new InputStreamReader(stream));
			StringBuffer buffer = new StringBuffer();
			while (reader.ready()) {
				buffer.append((char)reader.read());
			}
			fTextDocument = new Document(buffer.toString());
			fFindReplaceAdapter = new FindReplaceDocumentAdapter(fTextDocument);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} 				
	}
	
	private int getStartOffset(String elementName) throws BadLocationException{
		int lineNumber = fLocator.getLineNumber();
		int colNumber = fLocator.getColumnNumber();
		if (colNumber < 0)
			colNumber = getLastCharColumn(lineNumber);
		int offset = fTextDocument.getLineOffset(lineNumber - 1) + colNumber - 1;
		IRegion region = fFindReplaceAdapter.search(offset, "<" + elementName, false, false, false, false);
		return region.getOffset();
	}
	
	private int getLastCharColumn(int line) throws BadLocationException {
		String lineDelimiter = fTextDocument.getLineDelimiter(line - 1);
		int lineDelimiterLength = lineDelimiter != null ? lineDelimiter.length() : 0;
		return fTextDocument.getLineLength(line - 1) - lineDelimiterLength;
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
		if (fRootElement == null || fDocumentNodeStack.isEmpty())
			fDocument.appendChild(comment);
		else  {
			PluginDocumentNode node = ((PluginDocumentNode)fDocumentNodeStack.peek());
			node.getDOMNode().appendChild(comment);
		}
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
	
	public void reset(InputStream stream) {
		createTextDocument(stream);
		fRootElement = null;
		fModelRoot = null;
		fSchemaVersion = null;
		fDocumentNodeStack.clear();
		fLineTable.clear();
	}
	
	public String getSchemaVersion() {
		return fSchemaVersion;
	}
	
	public IDocumentNode getModelRoot() {
		return fModelRoot;
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
	 */
	public void fatalError(SAXParseException e) throws SAXException {
		generateErrorElementHierarchy();
	}
	
	/**
	 * 
	 */
	private void generateErrorElementHierarchy() {
		while (!fDocumentNodeStack.isEmpty()) {
			((PluginDocumentNode)fDocumentNodeStack.pop()).setIsErrorNode(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
	 */
	public void error(SAXParseException e) throws SAXException {
		generateErrorElementHierarchy();
	}
	
	public String getText() {
		return (fTextDocument == null) ? "" : fTextDocument.get();
	}
	
	
}
