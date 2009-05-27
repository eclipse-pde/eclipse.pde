/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Fabio Mancinelli <fm@fabiomancinelli.org> - bug 201306
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.StringReader;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.core.*;
import org.w3c.dom.*;
import org.w3c.dom.Document;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class XMLErrorReporter extends DefaultHandler {

	public static final char F_ATT_PREFIX = '@';
	public static final char F_ATT_VALUE_PREFIX = '!';
	public static final char F_CHILD_SEP = '>';

	class ElementData {
		int offset;
		boolean fErrorNode;

		public ElementData(int offset) {
			this.offset = offset;
		}
	}

	protected IFile fFile;

	protected IProject fProject;

	private int fErrorCount;

	private PDEMarkerFactory fMarkerFactory;

	private org.w3c.dom.Document fXMLDocument;

	private IDocument fTextDocument;

	private Stack fElementStack;

	private Element fRootElement;

	private Locator fLocator;

	private int fHighestOffset;

	private HashMap fOffsetTable;

	private FindReplaceDocumentAdapter fFindReplaceAdapter;

	private double fSchemaVersion = 2.1;

	public XMLErrorReporter(IFile file) {
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			fFile = file;
			fProject = file.getProject();
			manager.connect(file.getFullPath(), LocationKind.NORMALIZE, null);
			fTextDocument = manager.getTextFileBuffer(file.getFullPath(), LocationKind.NORMALIZE).getDocument();
			manager.disconnect(file.getFullPath(), LocationKind.NORMALIZE, null);
			fFindReplaceAdapter = new FindReplaceDocumentAdapter(fTextDocument);
			fOffsetTable = new HashMap();
			fElementStack = new Stack();
			removeFileMarkers();
		} catch (CoreException e) {
			PDECore.log(e);
		}
	}

	public IFile getFile() {
		return fFile;
	}

	private IMarker addMarker(String message, int lineNumber, int severity, int fixId, String category) {
		try {
			IMarker marker = getMarkerFactory().createMarker(fFile, fixId, category);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1)
				lineNumber = 1;
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			if (severity == IMarker.SEVERITY_ERROR)
				fErrorCount += 1;
			return marker;
		} catch (CoreException e) {
			PDECore.logException(e);
		}
		return null;
	}

	private PDEMarkerFactory getMarkerFactory() {
		if (fMarkerFactory == null)
			fMarkerFactory = new PDEMarkerFactory();
		return fMarkerFactory;
	}

	private void addMarker(SAXParseException e, int severity) {
		addMarker(e.getMessage(), e.getLineNumber(), severity, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
	}

	public void error(SAXParseException exception) throws SAXException {
		addMarker(exception, IMarker.SEVERITY_ERROR);
		generateErrorElementHierarchy();
	}

	public void fatalError(SAXParseException exception) throws SAXException {
		addMarker(exception, IMarker.SEVERITY_ERROR);
		generateErrorElementHierarchy();
	}

	public int getErrorCount() {
		return fErrorCount;
	}

	private void removeFileMarkers() {
		try {
			fFile.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
			fFile.deleteMarkers(PDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			PDECore.logException(e);
		}
	}

	public void report(String message, int line, int severity, int fixId, Element element, String attrName, String category) {
		IMarker marker = report(message, line, severity, fixId, category);
		if (marker == null)
			return;
		try {
			marker.setAttribute(PDEMarkerFactory.MPK_LOCATION_PATH, generateLocationPath(element, attrName));
		} catch (CoreException e) {
		}
	}

	private String generateLocationPath(Node node, String attrName) {
		if (node == null)
			return new String();

		int childIndex = 0;
		for (Node previousSibling = node.getPreviousSibling(); previousSibling != null; previousSibling = previousSibling.getPreviousSibling())
			childIndex += 1;

		StringBuffer sb = new StringBuffer();
		Node parent = node.getParentNode();
		if (parent != null && !(parent instanceof Document)) {
			sb.append(generateLocationPath(parent, null));
			sb.append(F_CHILD_SEP);
		}
		composeNodeString(node, childIndex, attrName, sb);
		return sb.toString();
	}

	private String composeNodeString(Node node, int index, String attrName, StringBuffer sb) {
		sb.append('(');
		sb.append(index);
		sb.append(')');
		sb.append(node.getNodeName());
		if (attrName != null) {
			sb.append(F_ATT_PREFIX);
			sb.append(attrName);
		}
		return sb.toString();
	}

	public IMarker report(String message, int line, int severity, int fixId, String category) {
		if (severity == CompilerFlags.ERROR)
			return addMarker(message, line, IMarker.SEVERITY_ERROR, fixId, category);
		if (severity == CompilerFlags.WARNING)
			return addMarker(message, line, IMarker.SEVERITY_WARNING, fixId, category);
		return null;
	}

	public IMarker report(String message, int line, int severity, String category) {
		return report(message, line, severity, PDEMarkerFactory.NO_RESOLUTION, category);
	}

	public void warning(SAXParseException exception) throws SAXException {
		addMarker(exception, IMarker.SEVERITY_WARNING);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			fXMLDocument = factory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		fXMLDocument.appendChild(fRootElement);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		Element element = fXMLDocument.createElement(qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			element.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}

		if (fRootElement == null)
			fRootElement = element;
		else
			((Element) fElementStack.peek()).appendChild(element);
		fElementStack.push(element);
		try {
			if (fTextDocument != null)
				fOffsetTable.put(element, new ElementData(getStartOffset(qName)));
		} catch (BadLocationException e) {
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName) throws SAXException {
		fElementStack.pop();
	}

	private void generateErrorElementHierarchy() {
		while (!fElementStack.isEmpty()) {
			ElementData data = (ElementData) fOffsetTable.get(fElementStack.pop());
			if (data != null)
				data.fErrorNode = true;
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char[] characters, int start, int length) throws SAXException {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < length; i++) {
			buff.append(characters[start + i]);
		}
		Text text = fXMLDocument.createTextNode(buff.toString());
		if (fRootElement == null)
			fXMLDocument.appendChild(text);
		else
			((Element) fElementStack.peek()).appendChild(text);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}

	private int getStartOffset(String elementName) throws BadLocationException {
		int line = fLocator.getLineNumber();
		int col = fLocator.getColumnNumber();
		if (col < 0)
			col = fTextDocument.getLineLength(line);
		String text = fTextDocument.get(fHighestOffset + 1, fTextDocument.getLineOffset(line) - fHighestOffset - 1);

		ArrayList commentPositions = new ArrayList();
		for (int idx = 0; idx < text.length();) {
			idx = text.indexOf("<!--", idx); //$NON-NLS-1$
			if (idx == -1)
				break;
			int end = text.indexOf("-->", idx); //$NON-NLS-1$
			if (end == -1)
				break;

			commentPositions.add(new Position(idx, end - idx));
			idx = end + 1;
		}

		int idx = 0;
		for (; idx < text.length(); idx += 1) {
			idx = text.indexOf("<" + elementName, idx); //$NON-NLS-1$
			if (idx == -1)
				break;
			boolean valid = true;
			for (int i = 0; i < commentPositions.size(); i++) {
				Position pos = (Position) commentPositions.get(i);
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

	private int getAttributeOffset(String name, String value, int offset) throws BadLocationException {
		IRegion nameRegion = fFindReplaceAdapter.find(offset, name + "=\"" + getWritableString(value), true, false, false, false); //$NON-NLS-1$
		if (nameRegion != null) {
			return nameRegion.getOffset();
		}
		return -1;
	}

	private String getWritableString(String source) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}

	protected String getTextContent(Element element) {
		ElementData data = (ElementData) fOffsetTable.get(element);
		try {
			IRegion nameRegion = fFindReplaceAdapter.find(data.offset, "</" + element.getNodeName() + ">", true, true, false, false); //$NON-NLS-1$ //$NON-NLS-2$
			int offset = data.offset + element.getNodeName().length() + 2;
			if (nameRegion != null)
				return fTextDocument.get(offset, nameRegion.getOffset() - offset).trim();
		} catch (BadLocationException e) {
		}
		return null;
	}

	protected int getLine(Element element) {
		ElementData data = (ElementData) fOffsetTable.get(element);
		try {
			return (data == null) ? 1 : fTextDocument.getLineOfOffset(data.offset) + 1;
		} catch (Exception e) {
			return 1;
		}
	}

	protected int getLine(Element element, String attName) {
		ElementData data = (ElementData) fOffsetTable.get(element);
		try {
			int offset = getAttributeOffset(attName, element.getAttribute(attName), data.offset);
			if (offset != -1)
				return fTextDocument.getLineOfOffset(offset) + 1;
		} catch (BadLocationException e) {
		}
		return getLine(element);
	}

	public void validateContent(IProgressMonitor monitor) {

	}

	public Element getDocumentRoot() {
		if (fRootElement != null)
			fRootElement.normalize();
		return fRootElement;
	}

	public void processingInstruction(String target, String data) throws SAXException {
		if ("eclipse".equals(target)) { //$NON-NLS-1$
			// Data should be of the form: version="<version>"
			if (data.length() > 10 && data.substring(0, 9).equals("version=\"") && data.charAt(data.length() - 1) == '\"') { //$NON-NLS-1$
				fSchemaVersion = Double.parseDouble(TargetPlatformHelper.getSchemaVersionForTargetVersion(data.substring(9, data.length() - 1)));
			} else {
				fSchemaVersion = Double.parseDouble(TargetPlatformHelper.getSchemaVersion());
			}
		}
	}

	protected double getSchemaVersion() {
		return fSchemaVersion;
	}

	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		int x = fTextDocument.get().indexOf("!DOCTYPE"); //$NON-NLS-1$
		if (x > 0) {
			try {
				int line = fTextDocument.getLineOfOffset(x) + 1;
				report(PDECoreMessages.XMLErrorReporter_ExternalEntityResolution, line, CompilerFlags.WARNING, PDEMarkerFactory.CAT_OTHER);
			} catch (BadLocationException e) {
			}
		}
		// Prevent the resolution of external entities in order to
		// prevent the parser from accessing the Internet
		// This will prevent huge workbench performance degradations and hangs
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

}
