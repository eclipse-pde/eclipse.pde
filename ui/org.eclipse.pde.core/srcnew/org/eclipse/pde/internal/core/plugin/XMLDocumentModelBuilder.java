/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.plugin;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.impl.XMLEntityScanner;
import org.apache.xerces.parsers.DOMBuilderImpl;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IDocument;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * 
 */
public class XMLDocumentModelBuilder extends DOMBuilderImpl {
	
	private Hashtable fLines= new Hashtable();
	private XMLLocator fLocator, fOldLocator;
	private int fLastLine, fLastColumn;
	private List fChildren= new ArrayList();
	private List fChildIndices= new ArrayList();
	private IDocumentModelFactory fModelFactory;
	private Map fRecoveredAttributes;
	private String fRecoveredRawname;
	private Set fIds= new TreeSet();

	
	/**
	 * Constructor for SourceDOMBuilder.
	 * @param config
	 */
	public XMLDocumentModelBuilder(XMLParserConfiguration config, boolean validate) {
		super(config);
		initialize(validate);
	}
	
	protected void initialize(boolean validate) {
		setFeature(DEFER_NODE_EXPANSION, false);
		setFeature(VALIDATION_FEATURE, validate);
		setFeature(VALIDATE_AGAINST_DTD, validate);
//		fConfiguration.setFeature(VALIDATE_IF_SCHEMA, true);
		setFeature(INCLUDE_IGNORABLE_WHITESPACE, true);
	}
	
//	public void setBasePathProvider(IBasePathProvider provider) {
//		setEntityResolver(new XEEntityResolver(provider));
//	}
	
	public void setProblemRequestor(IProblemRequestor requestor) {
		XEErrorHandler handler= null;
		if (requestor != null)
			handler= new XEErrorHandler(requestor);
		fConfiguration.setErrorHandler(handler);
	}
	
	public void setErrorHandler(XMLErrorHandler handler) {
		fConfiguration.setErrorHandler(handler);
	}
	
	public void setDocumentModelFactory(IDocumentModelFactory factory) {
		fModelFactory= factory;
	}
	
	public void parse(IDocument document) throws IOException {
		parse(new XMLInputSource(null, null, null, new StringReader(document.get()), null));
	}

	public Object getProperty(String propertyId) throws XMLConfigurationException {
		return fConfiguration.getProperty(propertyId);
	}

	private void startRange(Node node) {
		startRange(node, 0);
	}
	
	private void startRange(Node node, int columnOffset) {
		Assert.isTrue(!fInDTDExternalSubset);
		Assert.isTrue(node != null);
		if (!fLines.containsKey(node)) {
			SourceRange sourceRange= new SourceRange();
			sourceRange.setStartLine(fLastLine);
			sourceRange.setStartColumn(fLastColumn);
			fLines.put(node, sourceRange);
			startSubtree();
		} else {
			Assert.isTrue(true);
		}
		updateLastPosition(columnOffset);
	}
	
	private void endRange(Node node) {
		endRange(node, 0);
	}
	
	private void endRange(Node node, int columnOffset) {
		Assert.isTrue(!fInDTDExternalSubset);
		Assert.isTrue(node != null);
		ISourceRange sourceRange= (ISourceRange) fLines.get(node);
		Assert.isTrue(sourceRange != null);
		boolean firstPass = sourceRange.getEndLine() == -1;
		
		//we do this here, because character regions may be merged.
		sourceRange.setEndLine(fLocator.getLineNumber());
		sourceRange.setEndColumn(fLocator.getColumnNumber() + columnOffset);
		
		if (firstPass) {
			endSubtree(node);
		}
		updateLastPosition(columnOffset);
	}

	private void updateLastPosition() {
		updateLastPosition(0);
	}
	
	private void updateLastPosition(int columnOffset) {
		Assert.isTrue(!fInDTDExternalSubset);
		fLastLine= fLocator.getLineNumber();
		fLastColumn= fLocator.getColumnNumber() + columnOffset;
	}

	public Hashtable getLineTable() {
		return fLines;
	}
	
	public IDocumentNode getModelRoot() {
		if (fChildren.size() == 1) {
			Assert.isTrue(fChildIndices.size() == 0);
			return (IDocumentNode) fChildren.get(0);
		} else if (fChildren.size() > 1) {
			Assert.isTrue(false);
		}
		return null;
	}
	
	/*private String extract(XMLString text) {
		return new String(text.ch, text.offset, text.length);
	}*/

	private void recoverFromFatalError(Exception ex) {
		repairXMLTree();
		recoverTagAttributes(ex);
	}

	private void recoverTagAttributes(Exception ex) {
		if (ex instanceof XEParseException) {
			Object obj= fConfiguration.getProperty(XEParserConfiguration.DOCUMENT_SCANNER);
			if (obj instanceof XEDocumentScanner) {
				XEDocumentScanner documentScanner= (XEDocumentScanner) obj;
				fRecoveredRawname= documentScanner.getRawname();
				XMLAttributes attributes= documentScanner.getAttributes();
					
				if (attributes != null) {
					if (fRecoveredRawname != null) {
						String element= fRecoveredRawname;
						int index= element.indexOf(':');
						if (index > 0) {
							element= element.substring(index + 1);
						}
					}
					
					fRecoveredAttributes= new HashMap(attributes.getLength());
					for (int i= 0; i < attributes.getLength(); i++) {
						Attr attr= fDocument.createAttributeNS(attributes.getURI(i), attributes.getQName(i));
						attr.setValue(attributes.getValue(i));
						fRecoveredAttributes.put(attributes.getLocalName(i), attr);
					}
				}
			}
		}
	}

	private void repairXMLTree() {
		if (fChildIndices.size() > 0 && fCurrentNode != null) {
			Node node= fCurrentNode.getLastChild(); //REVISIT: do we surely get at the last inserted node? (may be introduce field 'lastNode'?) 
			if (node == null) {
				node= fCurrentNode;
			}
			ISourceRange sourceRange= (ISourceRange) fLines.get(node);
			Node parent;
			while (sourceRange.getEndLine() != -1) {
				parent= node.getParentNode();
				Assert.isTrue(node != parent && parent != null);
				node= parent;
				sourceRange= (ISourceRange) fLines.get(node);
			}
			parent= node.getParentNode();
			while (node != parent) {
				endRange(node);
				node= parent;
				parent= node == null ? null : node.getParentNode(); 
			}
		}
	}

	private void startSubtree() {
		fChildIndices.add(new Integer(fChildren.size())); //NOTE: this is done for empty elements too (see: super.element(...))
	}

	private void endSubtree(Node domNode) {
		//determine offset and length of children in fChildren 
		int firstChild= ((Integer)fChildIndices.remove(fChildIndices.size() - 1)).intValue();
		int nOfChildren= fChildren.size() - firstChild;
		
		//build array of children
		IDocumentNode[] children= null;
		if (nOfChildren > 0) {
			children= new IDocumentNode[nOfChildren];
			for (int i= nOfChildren - 1; i >= 0; i--) {
				children[i]= (IDocumentNode)fChildren.remove(firstChild + i);
			}
		}
		
		//create current node
		IDocumentNode currentNode= fModelFactory.createXMLNode(children, domNode);
		
		//register current node as parent of its children
		if (children != null) {
			for (int i= 0; i < children.length; i++) {
				children[i].setParent(currentNode);
			}
		}
		
		//add current node as child
		if (currentNode != null) {
			fChildren.add(currentNode);
			fLines.put(currentNode, fLines.get(domNode));
		}
	}

	/*
	 * @see org.apache.xerces.parsers.XMLParser#reset()
	 */
	public void reset() throws XNIException {
		super.reset();
		fChildIndices.clear();
		fChildren.clear();
		fLastColumn= 0;
		fLastLine= 0;
		fLines.clear();
		fLocator= null;
		fOldLocator= null;
		fRecoveredAttributes= null;
		fRecoveredRawname= null;
		fIds.clear();
	}

	/*
	 * @see org.apache.xerces.xni.XMLDocumentHandler#startDocument(org.apache.xerces.xni.XMLLocator, java.lang.String, org.apache.xerces.xni.Augmentations)
	 */
	public void startDocument(XMLLocator locator, String encoding, Augmentations augs) throws XNIException {
		super.startDocument(locator, encoding, augs);
		fLocator= locator;
		startRange(fCurrentNode);
	}

	/*
	 * @see org.apache.xerces.xni.XMLDocumentHandler#xmlDecl(java.lang.String, java.lang.String, java.lang.String, org.apache.xerces.xni.Augmentations)
	 */
	public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
		super.xmlDecl(version, encoding, standalone, augs);
		Element element= fDocument.createElement("XML");
		element.setAttribute("version", version);
		element.setAttribute("encoding", encoding);
		element.setAttribute("standalone", standalone);
		fCurrentNode.appendChild(element);
		startRange(element);
		endRange(element);
	}

	/*
	 * @see org.apache.xerces.xni.XMLDocumentHandler#doctypeDecl(java.lang.String, java.lang.String, java.lang.String, org.apache.xerces.xni.Augmentations)
	 */
	public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
		super.doctypeDecl(rootElement, publicId, systemId, augs);
		
		//work-around: scanner stops on '>' of the document type decl. and doesn't stop on '<' of the opening tag of the root element
		try {
			XMLEntityManager entityManager= (XMLEntityManager) fConfiguration.getProperty(XEParserConfiguration.ENTITY_MANAGER);
			XMLEntityScanner entityScanner= entityManager.getEntityScanner();
			if ((char)entityScanner.peekChar() == '>') {
				Node node= fCurrentNode.getLastChild();
				startRange(node, 1);
				endRange(node, 1);
//				while ((char)entityScanner.peekChar() != '<') {
//					entityScanner.scanChar();
//				}
				return;
			}
		} catch (IOException e) {
		}
		
		Node node= fCurrentNode.getLastChild();
		startRange(node);
		endRange(node);
	}

	/**
	 * @see org.apache.xerces.xni.XMLDocumentHandler#comment(org.apache.xerces.xni.XMLString, org.apache.xerces.xni.Augmentations)
	 * NOTE: This method is specified in XMLDocumentHandler <b>and</b>
	 * XMLDTDHandler.
	 */
	public void comment(XMLString text, Augmentations augmentations) throws XNIException {
		if (!fInDTDExternalSubset) {
			super.comment(text, augmentations);
			Node node= fCurrentNode.getLastChild();
			startRange(node);
			endRange(node);
		}
	}

	/**
	 * @see org.apache.xerces.xni.XMLDocumentHandler#processingInstruction(java.lang.String, org.apache.xerces.xni.XMLString, org.apache.xerces.xni.Augmentations)
	 * NOTE: This method is specified in XMLDocumentHandler <b>and</b>
	 * XMLDTDHandler. 	 
	 */
	public void processingInstruction(String target, XMLString data, Augmentations augmentations) throws XNIException {
		super.processingInstruction(target, data, augmentations);
		
		if (fInDTDExternalSubset) {
			Assert.isTrue(true);
		} else {
			Node node= fCurrentNode.getLastChild();
			startRange(node);
			endRange(node);
		}
	}

	/*
	 * @see org.apache.xerces.xni.XMLDocumentHandler#startElement(org.apache.xerces.xni.QName, org.apache.xerces.xni.XMLAttributes, org.apache.xerces.xni.Augmentations)
	 */
	public void startElement(QName element, XMLAttributes atts, Augmentations augs) throws XNIException {
		for (int i= 0; i < atts.getLength(); i++) {
			String value= atts.getValue(i);
			if (atts.getType(i).equals("ID") && value.length() > 0) {
				fIds.add(value);
			}
		}
		super.startElement(element, atts, augs);
		startRange(fCurrentNode);
	}

	/*
	 * @see org.apache.xerces.xni.XMLDocumentHandler#characters(org.apache.xerces.xni.XMLString, org.apache.xerces.xni.Augmentations)
	 */
	public void characters(XMLString text, Augmentations augs) throws XNIException {
		super.characters(text, augs);
		Node node= fCurrentNode.getLastChild(); //FIX ME: causes problems if super.characters(...) did appendData(...) instead of appendChild(...)
		startRange(node);
		endRange(node);
	}

	/*
	 * @see org.apache.xerces.xni.XMLDocumentHandler#ignorableWhitespace(org.apache.xerces.xni.XMLString, org.apache.xerces.xni.Augmentations)
	 */
	public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
		super.ignorableWhitespace(text, augs);
		Node node= fCurrentNode.getLastChild();
		startRange(node);
		endRange(node);
	}

	/*
	 * @see org.apache.xerces.xni.XMLDocumentHandler#endElement(org.apache.xerces.xni.QName, org.apache.xerces.xni.XMLAttributes, org.apache.xerces.xni. Augmentations)
	 */
	public void endElement(QName element, Augmentations augs) throws XNIException {
		super.endElement(element, augs);
		endRange(fCurrentNode.getLastChild());
	}

	/*
	 * @see org.apache.xerces.xni.XMLDocumentHandler#endDocument(org.apache.xerces.xni.Augmentations)
	 */
	public void endDocument(Augmentations augs) throws XNIException {
		if (fLocator.getLineNumber() > fLastLine || fLocator.getColumnNumber() > fLastColumn) {
			Text textNode= fDocument.createTextNode("#unknown text#");
			fCurrentNode.appendChild(textNode);
			startRange(textNode);
			endRange(textNode);
		}
		Node node= fCurrentNode;
		super.endDocument(augs);
		endRange(node);
	}
	
	/*
	 * @see org.apache.xerces.xni.XMLDTDHandler#startDTD(org.apache.xerces.xni.XMLLocator, org.apache.xerces.xni.Augmentations)
	 */
	public void startDTD(XMLLocator locator, Augmentations augmentations) throws XNIException {
		super.startDTD(locator, augmentations);
		fOldLocator= fLocator;
		fLocator= locator;
	}

	/*
	 * @see org.apache.xerces.xni.XMLDTDHandler#externalEntityDecl(java.lang.String, org.apache.xerces.xni.XMLResourceIdentifier, org.apache.xerces.xni.Augmentations)
	 */
	public void externalEntityDecl(String name, XMLResourceIdentifier identifier, Augmentations augmentations) throws XNIException {
		super.externalEntityDecl(name, identifier, augmentations);
		if (!fInDTDExternalSubset)
			updateLastPosition();
	}
	
	/*
	 * @see org.apache.xerces.xni.XMLDTDHandler#endDTD(org.apache.xerces.xni.Augmentations)
	 */
	public void endDTD(Augmentations augmentations) throws XNIException {
		super.endDTD(augmentations);
		fLocator= fOldLocator;
	}

	/*
	 * @see org.apache.xerces.dom3.ls.DOMBuilder#canSetFeature(java.lang.String, boolean)
	 */
	public boolean canSetFeature(String name, boolean state) {
		if (name.equals(VALIDATE_AGAINST_DTD))
			    return true;
		return super.canSetFeature(name, state);
	}

	/*
	 * @see org.apache.xerces.parsers.XMLParser#parse(org.apache.xerces.xni.parser.XMLInputSource)
	 */
	public void parse(XMLInputSource inputSource) throws XNIException, IOException {
		
		XEErrorHandler errorHandler= null;
		XMLErrorHandler handler= fConfiguration.getErrorHandler();
		if (handler instanceof XEErrorHandler)
			errorHandler= (XEErrorHandler) handler;
		
		if (errorHandler != null)
			errorHandler.beginReporting();
			
		try {
			super.parse(inputSource);
		} catch (XNIException ex) {
			recoverFromFatalError(ex);
			throw ex;
		} catch (IOException ex) {
			recoverFromFatalError(ex);
			throw ex;
		} finally {
			if (errorHandler != null)
				errorHandler.endReporting();
		}
	}
	
	/**
	 * Returns the fRecoveredRawname.
	 * 
	 * @return String
	 */
	public String getRecoveredRawname() {
		return fRecoveredRawname;
	}

	/**
	 * Returns the fRecoveredAttributes.
	 * 
	 * @return ArrayList
	 */
	public Map getRecoveredAttributes() {
		return fRecoveredAttributes;
	}

	/**
	 * Returns the fIds.
	 * 
	 * @return Set
	 */
	public Set getIds() {
		return fIds;
	}
}
