/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.IOException;

import org.apache.xerces.impl.XMLDocumentScannerImpl;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;


public class XEDocumentScanner extends XMLDocumentScannerImpl {

	protected boolean fNoAttributesYet;
	protected boolean fScanningStartElement;
	protected XMLAttributes fAttributes;
	protected String fRawname;

	/**
	 * Constructor for XEDocumentScanner.
	 */
	public XEDocumentScanner() {
		super();
	}

	/*
	 * @see org.apache.xerces.impl.XMLDocumentFragmentScannerImpl#scanStartElement()
	 */
	protected boolean scanStartElement() throws IOException, XNIException {
		fScanningStartElement= true;
		fNoAttributesYet= true;
		boolean result;
		try {
			result= super.scanStartElement();
		} finally {
			fScanningStartElement= false;
		}
		return result;
	}

	/*
	 * @see org.apache.xerces.impl.XMLDocumentFragmentScannerImpl#scanAttribute(org.apache.xerces.xni.XMLAttributes)
	 */
	protected void scanAttribute(XMLAttributes attributes) throws IOException, XNIException {
		fNoAttributesYet= false;
		fAttributes= attributes;
		try {
			super.scanAttribute(attributes);
		} catch (XEParseException ex) {
			if (ex.getKey().equals("EqRequiredInAttribute") && fEntityScanner instanceof XEEntityManager.XEEntityScanner) {
				XEEntityManager.XEEntityScanner scanner= (XEEntityManager.XEEntityScanner) fEntityScanner;
				scanner.insert(" ");
				return;
			}
			throw ex;
		}
	}

	/*
	 * @see org.apache.xerces.impl.XMLScanner#scanAttributeValue(org.apache.xerces.xni.XMLString, org.apache.xerces.xni.XMLString, java.lang.String, org.apache.xerces.xni.XMLAttributes, int, boolean)
	 */
	protected void scanAttributeValue(XMLString value, XMLString nonNormalizedValue, String atName, XMLAttributes attributes, int attrIndex, boolean checkEntities) throws IOException, XNIException {
		try {
			super.scanAttributeValue(value, nonNormalizedValue, atName, attributes, attrIndex, checkEntities);
		} catch (XEParseException ex) {
			if (ex.getKey().equals("OpenQuoteExpected")) {
				value.clear();
				nonNormalizedValue.clear();
				return;
			}
			if (ex.getKey().equals("LessthanInAttValue")) {
				value.clear();
				nonNormalizedValue.clear();
				XEEntityManager.XEEntityScanner scanner= (XEEntityManager.XEEntityScanner) fEntityScanner;
				scanner.insert("/>");
				return;
			}
			throw ex;
		}
	}

	/*
	 * @see org.apache.xerces.impl.XMLScanner#reportFatalError(java.lang.String, java.lang.Object)
	 */
	protected void reportFatalError(String msgId, Object[] args) throws XNIException {
		if (fScanningStartElement) { //try getting at current element name and attributes in case we can't recover from this error
			if (args.length > 0 && args[0] instanceof String) {
				fRawname= (String) args[0];
			} else {
				fRawname= null;
			}
			if (fNoAttributesYet) {
				fAttributes= null; //in this case, the current object contains old attributes
			}
		} else {
			fRawname= null;
			fAttributes= null;
		}
		super.reportFatalError(msgId, args);
	}

	/**
	 * Returns the fAttributes.
	 * @return XMLAttributes
	 */
	public XMLAttributes getAttributes() {
		return fAttributes;
	}

	/**
	 * Returns the fRawname.
	 * @return String
	 */
	public String getRawname() {
		return fRawname;
	}
}
