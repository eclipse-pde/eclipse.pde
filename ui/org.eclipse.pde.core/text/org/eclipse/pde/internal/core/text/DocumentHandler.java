/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Stack;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class DocumentHandler extends DefaultHandler {

	protected Stack<IDocumentElementNode> fDocumentNodeStack = new Stack<>();
	protected int fHighestOffset = 0;
	private Locator fLocator;
	private IDocumentElementNode fLastError;
	private final boolean fReconciling;

	public DocumentHandler(boolean reconciling) {
		fReconciling = reconciling;
	}

	@Override
	public void startDocument() throws SAXException {
		fDocumentNodeStack.clear();
		fHighestOffset = 0;
		fLastError = null;
	}

	protected IDocumentElementNode getLastParsedDocumentNode() {
		if (fDocumentNodeStack.isEmpty()) {
			return null;
		}
		return fDocumentNodeStack.peek();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		IDocumentElementNode parent = getLastParsedDocumentNode();
		IDocumentElementNode node = getDocumentNode(qName, parent);
		try {
			int nodeOffset = getStartOffset(qName);
			node.setOffset(nodeOffset);
			IDocument doc = getDocument();
			int line = doc.getLineOfOffset(nodeOffset);
			node.setLineIndent(node.getOffset() - doc.getLineOffset(line));
			// create attributes
			for (int i = 0; i < attributes.getLength(); i++) {
				String attName = attributes.getQName(i);
				String attValue = attributes.getValue(i);
				IDocumentAttributeNode attribute = getDocumentAttribute(attName, attValue, node);
				if (attribute != null) {
					IRegion region = getAttributeRegion(attName, attValue, nodeOffset);
					if (region == null) {
						attValue = PDEXMLHelper.getWritableString(attValue);
						region = getAttributeRegion(attName, attValue, nodeOffset);
					}
					if (region != null) {
						attribute.setNameOffset(region.getOffset());
						attribute.setNameLength(attName.length());
						attribute.setValueOffset(region.getOffset() + region.getLength() - 1 - attValue.length());
						attribute.setValueLength(attValue.length());
					}
					node.setXMLAttribute(attribute);
				}
			}
			removeOrphanAttributes(node);
		} catch (BadLocationException e) {
		}
		if (parent != null && node != null && node.getParentNode() == null) {
			if (fReconciling) {
				// find right place for the child
				// this is necessary to save as much as possible from the model
				// we do not want an xml element with one tag to overwrite an element
				// with a different tag
				int position = 0;
				IDocumentElementNode[] children = parent.getChildNodes();
				for (; position < children.length; position++) {
					if (children[position].getOffset() == -1) {
						break;
					}
				}
				parent.addChildNode(node, position);
			} else {
				parent.addChildNode(node);
			}
		}
		fDocumentNodeStack.push(node);
	}

	protected abstract IDocumentElementNode getDocumentNode(String name, IDocumentElementNode parent);

	protected abstract IDocumentAttributeNode getDocumentAttribute(String name, String value, IDocumentElementNode parent);

	protected abstract IDocumentTextNode getDocumentTextNode(String content, IDocumentElementNode parent);

	private int getStartOffset(String elementName) throws BadLocationException {
		int line = fLocator.getLineNumber();
		int col = fLocator.getColumnNumber();
		IDocument doc = getDocument();
		if (col < 0) {
			col = doc.getLineLength(line);
		}

		int endOffset;
		if (line < doc.getNumberOfLines()) {
			endOffset = doc.getLineOffset(line);
		} else {
			line = doc.getNumberOfLines() - 1;
			IRegion lineInfo = doc.getLineInformation(line);
			endOffset = lineInfo.getOffset() + lineInfo.getLength();
		}
		String text = doc.get(fHighestOffset + 1, endOffset - fHighestOffset - 1);

		ArrayList<Position> commentPositions = new ArrayList<>();
		for (int idx = 0; idx < text.length();) {
			idx = text.indexOf("<!--", idx); //$NON-NLS-1$
			if (idx == -1) {
				break;
			}
			int end = text.indexOf("-->", idx); //$NON-NLS-1$
			if (end == -1) {
				break;
			}

			commentPositions.add(new Position(idx, end - idx));
			idx = end + 1;
		}

		int idx = 0;
		for (; idx < text.length(); idx += 1) {
			idx = text.indexOf("<" + elementName, idx); //$NON-NLS-1$
			if (idx == -1) {
				break;
			}
			boolean valid = true;
			for (int i = 0; i < commentPositions.size(); i++) {
				Position pos = commentPositions.get(i);
				if (pos.includes(idx)) {
					valid = false;
					break;
				}
			}
			if (valid) {
				break;
			}
		}
		if (idx > -1) {
			fHighestOffset += idx + 1;
		}
		return fHighestOffset;
	}

	private int getElementLength(IDocumentElementNode node, int line, int column) throws BadLocationException {
		int endIndex = node.getOffset();
		IDocument doc = getDocument();
		int start = Math.max(doc.getLineOffset(line), node.getOffset());
		column = doc.getLineLength(line);
		String lineText = doc.get(start, column - start + doc.getLineOffset(line));

		int index = lineText.indexOf("</" + node.getXMLTagName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		if (index == -1) {
			index = lineText.indexOf('>');
			if (index == -1) {
				endIndex = column;
			} else {
				endIndex = index + 1;
			}
		} else {
			endIndex = index + node.getXMLTagName().length() + 3;
		}
		return start + endIndex - node.getOffset();
	}

	private IRegion getAttributeRegion(String name, String value, int offset) throws BadLocationException {
		FindReplaceDocumentAdapter fFindReplaceAdapter = new FindReplaceDocumentAdapter(getDocument());
		IRegion nameRegion = fFindReplaceAdapter.find(offset, "\\s" + name + "\\s*=\\s*[\"\']", true, true, false, true); //$NON-NLS-1$ //$NON-NLS-2$
		if (nameRegion != null) {
			nameRegion = new Region(nameRegion.getOffset() + 1, nameRegion.getLength() - 1);
			if (getDocument().get(nameRegion.getOffset() + nameRegion.getLength(), value.length()).equals(value)) {
				return new Region(nameRegion.getOffset(), nameRegion.getLength() + value.length() + 1);
			}
		}
		return null;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (fDocumentNodeStack.isEmpty()) {
			return;
		}

		IDocumentElementNode node = fDocumentNodeStack.pop();
		try {
			node.setLength(getElementLength(node, fLocator.getLineNumber() - 1, fLocator.getColumnNumber()));
			setTextNodeOffset(node);
		} catch (BadLocationException e) {
		}
		removeOrphanElements(node);
	}

	protected void setTextNodeOffset(IDocumentElementNode node) throws BadLocationException {
		IDocumentTextNode textNode = node.getTextNode();
		if (textNode != null && textNode.getText() != null) {
			if (textNode.getText().trim().length() == 0) {
				node.removeTextNode();
				return;
			}
			IDocument doc = getDocument();
			String text = doc.get(node.getOffset(), node.getLength());
			// 1st char of text node
			int relativeStartOffset = text.indexOf('>') + 1;
			// last char of text node
			int relativeEndOffset = text.lastIndexOf('<') - 1;

			if ((relativeStartOffset < 0) || (relativeStartOffset >= text.length())) {
				return;
			} else if ((relativeEndOffset < 0) || (relativeEndOffset >= text.length())) {
				return;
			}

			// trim whitespace
			while (Character.isWhitespace(text.charAt(relativeStartOffset))) {
				relativeStartOffset += 1;
			}
			while (Character.isWhitespace(text.charAt(relativeEndOffset))) {
				relativeEndOffset -= 1;
			}

			textNode.setOffset(node.getOffset() + relativeStartOffset);
			textNode.setLength(relativeEndOffset - relativeStartOffset + 1);
			textNode.setText(textNode.getText().trim());
		}
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		generateErrorElementHierarchy();
	}

	/**
	 *
	 */
	private void generateErrorElementHierarchy() {
		while (!fDocumentNodeStack.isEmpty()) {
			IDocumentElementNode node = fDocumentNodeStack.pop();
			node.setIsErrorNode(true);
			removeOrphanAttributes(node);
			removeOrphanElements(node);
			if (fLastError == null) {
				fLastError = node;
			}
		}
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		generateErrorElementHierarchy();
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}

	protected abstract IDocument getDocument();

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		// Prevent the resolution of external entities in order to
		// prevent the parser from accessing the Internet
		// This will prevent huge workbench performance degradations and hangs
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

	public IDocumentElementNode getLastErrorNode() {
		return fLastError;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (!fReconciling || fDocumentNodeStack.isEmpty()) {
			return;
		}

		IDocumentElementNode parent = fDocumentNodeStack.peek();
		StringBuilder buffer = new StringBuilder();
		buffer.append(ch, start, length);
		getDocumentTextNode(buffer.toString(), parent);
	}

	private void removeOrphanAttributes(IDocumentElementNode node) {
		// when typing by hand, one element may overwrite a different existing one
		// remove all attributes from previous element, if any.
		if (fReconciling) {
			IDocumentAttributeNode[] attrs = node.getNodeAttributes();
			for (IDocumentAttributeNode attrNode : attrs) {
				if (attrNode.getNameOffset() == -1) {
					node.removeDocumentAttribute(attrNode);
				}
			}
		}
	}

	private void removeOrphanElements(IDocumentElementNode node) {
		// when typing by hand, one element may overwrite a different existing one
		// remove all excess children elements, if any.
		if (fReconciling) {
			IDocumentElementNode[] children = node.getChildNodes();
			for (IDocumentElementNode elementNode : children) {
				if (elementNode.getOffset() == -1) {
					node.removeChildNode(elementNode);
				}
			}
		}
	}

	protected boolean isReconciling() {
		return fReconciling;
	}

}
