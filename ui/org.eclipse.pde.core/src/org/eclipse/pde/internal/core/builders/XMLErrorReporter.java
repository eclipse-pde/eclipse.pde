/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Fabio Mancinelli <fm@fabiomancinelli.org> - bug 201306
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.builders.IncrementalErrorReporter.VirtualMarker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class XMLErrorReporter extends DefaultHandler {

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

	private final IncrementalErrorReporter fErrorReporter;

	private org.w3c.dom.Document fXMLDocument;

	private IDocument fTextDocument;

	private Stack<Element> fElementStack;

	private Element fRootElement;

	private Locator fLocator;

	private int fHighestOffset;

	private HashMap<Element, ElementData> fOffsetTable;

	private FindReplaceDocumentAdapter fFindReplaceAdapter;

	private double fSchemaVersion = 2.1;

	public XMLErrorReporter(IFile file) {
		fErrorReporter = new IncrementalErrorReporter(file);

		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			fFile = file;
			fProject = file.getProject();
			manager.connect(file.getFullPath(), LocationKind.NORMALIZE, null);
			fTextDocument = manager.getTextFileBuffer(file.getFullPath(), LocationKind.NORMALIZE).getDocument();
			manager.disconnect(file.getFullPath(), LocationKind.NORMALIZE, null);
			fFindReplaceAdapter = new FindReplaceDocumentAdapter(fTextDocument);
			fOffsetTable = new HashMap<>();
			fElementStack = new Stack<>();
		} catch (CoreException e) {
			PDECore.log(e);
		}
	}

	public IFile getFile() {
		return fFile;
	}

	private VirtualMarker addMarker(String message, int lineNumber, int severity, int fixId, String category) {
		return fErrorReporter.addMarker(message, lineNumber, severity, fixId, category);
	}

	private void addMarker(SAXParseException e, int severity) {
		addMarker(e.getMessage(), e.getLineNumber(), severity, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		addMarker(exception, IMarker.SEVERITY_ERROR);
		generateErrorElementHierarchy();
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		addMarker(exception, IMarker.SEVERITY_ERROR);
		generateErrorElementHierarchy();
	}

	public int getErrorCount() {
		return fErrorReporter.getErrorCount();
	}

	public VirtualMarker report(String message, int line, int severity, int fixId, Element element, String attrName,
			String category) {
		VirtualMarker marker = report(message, line, severity, fixId, category);
		if (marker == null) {
			return null;
		}
		marker.setAttribute(PDEMarkerFactory.MPK_LOCATION_PATH, generateLocationPath(element, attrName));
		return marker;
	}

	private String generateLocationPath(Node node, String attrName) {
		if (node == null) {
			return ""; // //$NON-NLS-1$
		}

		int childIndex = 0;
		for (Node previousSibling = node.getPreviousSibling(); previousSibling != null; previousSibling = previousSibling.getPreviousSibling()) {
			childIndex += 1;
		}

		StringBuilder sb = new StringBuilder();
		Node parent = node.getParentNode();
		if (parent != null && !(parent instanceof Document)) {
			sb.append(generateLocationPath(parent, null));
			sb.append(F_CHILD_SEP);
		}
		composeNodeString(node, childIndex, attrName, sb);
		return sb.toString();
	}

	private String composeNodeString(Node node, int index, String attrName, StringBuilder sb) {
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

	public VirtualMarker report(String message, int line, int severity, int fixId, String category) {
		if (severity == CompilerFlags.ERROR) {
			return addMarker(message, line, IMarker.SEVERITY_ERROR, fixId, category);
		}
		if (severity == CompilerFlags.WARNING) {
			return addMarker(message, line, IMarker.SEVERITY_WARNING, fixId, category);
		}
		return null;
	}

	public VirtualMarker report(String message, int line, int severity, String category) {
		return report(message, line, severity, PDEMarkerFactory.M_ONLY_CONFIG_SEV, category);
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		addMarker(exception, IMarker.SEVERITY_WARNING);
	}

	@Override
	public void startDocument() throws SAXException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			fXMLDocument = factory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
		}
	}

	@Override
	public void endDocument() throws SAXException {
		fXMLDocument.appendChild(fRootElement);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		Element element = fXMLDocument.createElement(qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			element.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}

		if (fRootElement == null) {
			fRootElement = element;
		} else {
			fElementStack.peek().appendChild(element);
		}
		fElementStack.push(element);
		try {
			if (fTextDocument != null) {
				fOffsetTable.put(element, new ElementData(getStartOffset(qName)));
			}
		} catch (BadLocationException e) {
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		fElementStack.pop();
	}

	private void generateErrorElementHierarchy() {
		while (!fElementStack.isEmpty()) {
			ElementData data = fOffsetTable.get(fElementStack.pop());
			if (data != null) {
				data.fErrorNode = true;
			}
		}
	}

	@Override
	public void characters(char[] characters, int start, int length) throws SAXException {
		StringBuilder buff = new StringBuilder();
		for (int i = 0; i < length; i++) {
			buff.append(characters[start + i]);
		}
		Text text = fXMLDocument.createTextNode(buff.toString());
		if (fRootElement == null) {
			fXMLDocument.appendChild(text);
		} else {
			fElementStack.peek().appendChild(text);
		}
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}

	private int getStartOffset(String elementName) throws BadLocationException {
		int line = fLocator.getLineNumber();
		int col = fLocator.getColumnNumber();
		if (col < 0) {
			col = fTextDocument.getLineLength(line);
		}
		String text = fTextDocument.get(fHighestOffset + 1, fTextDocument.getLineOffset(line) - fHighestOffset - 1);

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

	private int getAttributeOffset(String name, String value, int offset) throws BadLocationException {
		IRegion nameRegion = fFindReplaceAdapter.find(offset, name + "=\"" + getWritableString(value), true, false, false, false); //$NON-NLS-1$
		if (nameRegion != null) {
			return nameRegion.getOffset();
		}
		return -1;
	}

	private String getWritableString(String source) {
		StringBuilder buf = new StringBuilder();
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

	/**
	 * Returns the text content of the xml element or <code>null</code> if there
	 * is a problem determining the content.  If the element has any children
	 * nodes, <code>null</code> will be returned.
	 *
	 * @param element the xml element to parse
	 * @return the text content of the xml node or <code>null</code>
	 */
	protected String getTextContent(Element element) {
		ElementData data = fOffsetTable.get(element);
		if (data == null) {
			return null;
		}
		try {
			if (element.hasChildNodes()) {
				return null;
			}
			IRegion openElement = fFindReplaceAdapter.find(data.offset, ">", true, true, false, false); //$NON-NLS-1$
			IRegion closeElement = fFindReplaceAdapter.find(data.offset, "</" + element.getNodeName() + ">", true, true, false, false); //$NON-NLS-1$ //$NON-NLS-2$
			if (openElement != null && closeElement != null) {
				int endOfOpenElement = openElement.getOffset() + openElement.getLength();
				return fTextDocument.get(endOfOpenElement, closeElement.getOffset() - endOfOpenElement).trim();
			}
		} catch (BadLocationException e) {
		}
		return null;
	}

	protected int getLine(Element element) {
		ElementData data = fOffsetTable.get(element);
		try {
			return (data == null) ? 1 : fTextDocument.getLineOfOffset(data.offset) + 1;
		} catch (Exception e) {
			return 1;
		}
	}

	protected int getLine(Element element, String attName) {
		ElementData data = fOffsetTable.get(element);
		if (data != null) {
			try {
				int offset = getAttributeOffset(attName, element.getAttribute(attName), data.offset);
				if (offset != -1) {
					return fTextDocument.getLineOfOffset(offset) + 1;
				}
			} catch (BadLocationException e) {
			}
		}
		return getLine(element);
	}

	public final void validateContent(IProgressMonitor monitor) {
		validate(monitor);
		fErrorReporter.applyMarkers();
	}

	protected abstract void validate(IProgressMonitor monitor);

	public Element getDocumentRoot() {
		if (fRootElement != null) {
			fRootElement.normalize();
		}
		return fRootElement;
	}

	@Override
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

	@Override
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
