package org.eclipse.pde.internal.ui.model.plugin;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.ui.model.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * @author melhem
 *
 */
public class PluginDocumentHandler extends DefaultHandler {
	
	private PluginModelBase fModel;
	private PluginDocumentNodeFactory fFactory;
	private FindReplaceDocumentAdapter fFindReplaceAdapter;
	private Locator fLocator;
	private Stack fDocumentNodeStack = new Stack();
	
	
	public PluginDocumentHandler(PluginModelBase model) {
		fModel = model;
		fFactory = new PluginDocumentNodeFactory(model);
		fFindReplaceAdapter = new FindReplaceDocumentAdapter(fModel.getDocument());
	}
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		fDocumentNodeStack.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data)
			throws SAXException {
		try {
			if ("eclipse".equals(target)) {
				fModel.getPluginBase().setSchemaVersion("3.0");
			}
		} catch (CoreException e) {
		}
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		IDocumentNode parent = fDocumentNodeStack.isEmpty() ? null : (IDocumentNode)fDocumentNodeStack.peek();		
		IDocumentNode node = fFactory.createDocumentNode(qName, parent);
		try {
			node.setOffset(getStartOffset(qName));
		} catch (BadLocationException e) {
		}
		// create attributes
		for (int i = 0; i < attributes.getLength(); i++) {
			IDocumentAttribute attribute = fFactory.createAttribute(attributes.getQName(i), attributes.getValue(i), node);
			if (attribute != null)
				node.setXMLAttribute(attribute);
		}
		if (parent != null)
			parent.addChildNode(node);
		fDocumentNodeStack.push(node);
	}
	
	private int getStartOffset(String elementName) throws BadLocationException{
		int lineNumber = fLocator.getLineNumber();
		int colNumber = fLocator.getColumnNumber();
		if (colNumber < 0)
			colNumber = getLastCharColumn(lineNumber);
		int offset = fModel.getDocument().getLineOffset(lineNumber - 1) + colNumber - 1;
		IRegion region = fFindReplaceAdapter.search(offset, "<" + elementName, false, false, false, false);
		return region.getOffset();
	}
	
	private int getLastCharColumn(int line) throws BadLocationException {
		IDocument document = fModel.getDocument();
		String lineDelimiter = document.getLineDelimiter(line - 1);
		int lineDelimiterLength = lineDelimiter != null ? lineDelimiter.length() : 0;
		return document.getLineLength(line - 1) - lineDelimiterLength;
	}


	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		IDocumentNode node = (IDocumentNode)fDocumentNodeStack.pop();
		int line = fLocator.getLineNumber();
		try {
			node.setLength(fModel.getDocument().getLineOffset(line) - node.getOffset());
		} catch (BadLocationException e) {
		}
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
			((IDocumentNode)fDocumentNodeStack.pop()).setIsErrorNode(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
	 */
	public void error(SAXParseException e) throws SAXException {
		generateErrorElementHierarchy();
	}
	
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);
	}
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}
}
