package org.eclipse.pde.internal.ui.model.plugin;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.plugin.*;
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
	private String fSchemaVersion;
	
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
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		IPluginBase pluginBase = fModel.getPluginBase();
		try {
			if (pluginBase != null)
				pluginBase.setSchemaVersion(fSchemaVersion);
		} catch (CoreException e) {
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data)
			throws SAXException {
		if ("eclipse".equals(target)) {
			fSchemaVersion = "3.0";
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
			IDocument doc = fModel.getDocument();
			int line = doc.getLineOfOffset(nodeOffset);
			node.setLineIndent(node.getOffset() - doc.getLineOffset(line));
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

		ArrayList commentPositions = new ArrayList();
		for (int idx = 0; idx < text.length();) {
			idx = text.indexOf("<!--", idx);
			if (idx == -1)
				break;
			int end = text.indexOf("-->", idx);
			if (end == -1) 
				break;
			
			commentPositions.add(new Position(idx, end - idx));
			idx = end + 1;
		}

		int idx = 0;
		for (; idx < text.length(); idx += 1) {
			idx = text.indexOf("<" + elementName, idx);
			if (idx == -1)
				break;
			boolean valid = true;
			for (int i = 0; i < commentPositions.size(); i++) {
				Position pos = (Position)commentPositions.get(i);
				if (pos.includes(idx)) {
					valid = false;
					break;
				}
			}
			if (valid)
				break;
		}
		if (idx > -1)
			fHighestOffset += idx + 1;
		return fHighestOffset;
	}
	
	private int getElementLength(IDocumentNode node, int line, int column) throws BadLocationException {
		int endIndex = node.getOffset();
		IDocument doc = fModel.getDocument();
		int start = Math.max(doc.getLineOffset(line), node.getOffset());
		column = doc.getLineLength(line);
		String lineText= doc.get(start, column - start + doc.getLineOffset(line));
		
		int index = lineText.indexOf("</" + node.getXMLTagName() + ">");
		if (index == -1) {
			index= lineText.indexOf("/>"); //$NON-NLS-1$
			if (index == -1 ) {
				endIndex = column;
			} else {
				endIndex = index + 2;
			}
		} else{
			endIndex = index + node.getXMLTagName().length() + 3;
		}
		return start + endIndex - node.getOffset();
	}
	
	private IRegion getAttributeRegion(String name, String value, int offset) throws BadLocationException{
		return fFindReplaceAdapter.find(offset, name+"\\s*=\\s*\""+value+"\"", true, false, false, true);
	}

	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		IDocumentNode node = (IDocumentNode)fDocumentNodeStack.pop();
		try {
			node.setLength(getElementLength(node, fLocator.getLineNumber() - 1, fLocator.getColumnNumber()));
			IDocumentTextNode textNode = node.getTextNode();
			if (textNode != null) {
				IDocument doc = fModel.getDocument();
				String text = doc.get(node.getOffset(), node.getLength());
				textNode.setOffset(node.getOffset() + text.indexOf(textNode.getText()));
				text = doc.get(textNode.getOffset(), node.getLength() - textNode.getOffset() + node.getOffset());
				int index = text.indexOf('<');
                for (index -= 1; index >= 0; index--) {
                	if (!Character.isWhitespace(text.charAt(index))) {
                		index += 1;
                		break;
                	}
                }
                textNode.setLength(index);
                textNode.setText(doc.get(textNode.getOffset(), index));
			}
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
	public void characters(char[] ch, int start, int length) throws SAXException {		
		IDocumentNode parent = (IDocumentNode)fDocumentNodeStack.peek();
		if (parent == null)
			return;
		
		StringBuffer buffer = new StringBuffer();
		buffer.append(ch, start, length);
		IDocumentTextNode textNode = parent.getTextNode();
		if (textNode == null) {
			if (buffer.toString().trim().length() > 0) {
				textNode = new DocumentTextNode();
				textNode.setEnclosingElement(parent);
				parent.addTextNode(textNode);
				textNode.setText(buffer.toString().trim());
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}
}
