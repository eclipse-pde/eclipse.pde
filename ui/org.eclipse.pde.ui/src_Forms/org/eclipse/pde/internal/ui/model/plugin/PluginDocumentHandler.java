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
	private int fHighestOffset = 0;
	
	
	public PluginDocumentHandler(PluginModelBase model) {
		fModel = model;
		fFactory = (PluginDocumentNodeFactory)fModel.getPluginFactory();
		fFindReplaceAdapter = new FindReplaceDocumentAdapter(fModel.getDocument());
	}
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		fDocumentNodeStack.clear();
		fHighestOffset = 0;
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
		node.setXMLTagName(qName);
		try {
			int nodeOffset = getStartOffset(qName);
			node.setOffset(nodeOffset);
			// create attributes
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = attributes.getValue(i);
				IDocumentAttribute attribute = fFactory.createAttribute(attName, attValue, node);
				if (attribute != null) {
					IRegion region = getAttributeRegion(attName, attValue, nodeOffset);
					if (region != null) {
						attribute.setNameOffset(region.getOffset());
						attribute.setNameLength(attName.length());
						attribute.setValueOffset(region.getOffset() + region.getLength() - 1 - attValue.length());
						attribute.setValueLength(attValue.length());
					}
					node.setXMLAttribute(attribute);
				}
			}
		} catch (BadLocationException e) {
		}
		if (parent != null)
			parent.addChildNode(node);
		fDocumentNodeStack.push(node);
	}
	
	private int getStartOffset(String elementName) throws BadLocationException {
		int line = fLocator.getLineNumber();
		int col = fLocator.getColumnNumber();
		IDocument doc = fModel.getDocument();
		if (col < 0)
			col = doc.getLineLength(line);
		String text = doc.get(fHighestOffset + 1, doc.getLineOffset(line) - fHighestOffset - 1);
		int index = text.indexOf("<" + elementName);
		if (index > -1)
			fHighestOffset += index + 1;
		return fHighestOffset;
	}
	
	private int getElementLength(IDocumentNode node, int line, int column) throws BadLocationException {
		int endIndex = node.getOffset();
		IDocument doc = fModel.getDocument();
		if (column <= 0) {
			column = doc.getLineLength(line);
			int start = Math.max(doc.getLineOffset(line), node.getOffset());
			String lineText= doc.get(start, column - start + doc.getLineOffset(line));
			
			int index= lineText.indexOf("<" + node.getXMLTagName() + "/>");
			if (index == -1) {
				index= lineText.indexOf("/>"); //$NON-NLS-1$
				if (index == -1 ) {
					endIndex = column;
				} else {
					endIndex = index + 2;
				}
				endIndex += start - doc.getLineOffset(line);
			} else {
				endIndex = index + node.getXMLTagName().length() + 2;
			}
		}
		return doc.getLineOffset(line) + endIndex - node.getOffset();
	}
	
	private IRegion getAttributeRegion(String name, String value, int offset) throws BadLocationException{
		return fFindReplaceAdapter.search(offset, name+"\\s*=\\s*\""+value+"\"", true, false, false, true);
	}

	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		IDocumentNode node = (IDocumentNode)fDocumentNodeStack.pop();
		try {
			node.setLength(getElementLength(node, fLocator.getLineNumber() - 1, fLocator.getColumnNumber()));
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
