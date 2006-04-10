/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.xhtml;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.lucene.demo.html.HTMLParserConstants;
import org.apache.lucene.demo.html.Token;

public class XHTMLTag {
	
	/*
	 * Empty XHTML Strict 1.0 elements
	 * derived from http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd
	 */
	private static final ArrayList F_XHTML_S1_EE = new ArrayList(10); 
	static {
		F_XHTML_S1_EE.add("base"); //$NON-NLS-1$
		F_XHTML_S1_EE.add("meta"); //$NON-NLS-1$
		F_XHTML_S1_EE.add("link"); //$NON-NLS-1$
		F_XHTML_S1_EE.add("hr"); //$NON-NLS-1$
		F_XHTML_S1_EE.add("br"); //$NON-NLS-1$
		F_XHTML_S1_EE.add("param"); //$NON-NLS-1$
		F_XHTML_S1_EE.add("img"); //$NON-NLS-1$
		F_XHTML_S1_EE.add("area"); //$NON-NLS-1$
		F_XHTML_S1_EE.add("input"); //$NON-NLS-1$
		F_XHTML_S1_EE.add("col"); //$NON-NLS-1$
	}
	/*
	 * Empty XHTML Transitional 1.0 elements
	 * derived from http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd
	 */
	private static final ArrayList F_XHTML_T1_EE = new ArrayList(12);
	static {
		F_XHTML_T1_EE.add("base"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("meta"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("link"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("hr"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("br"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("basefont"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("param"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("img"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("area"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("input"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("isindex"); //$NON-NLS-1$
		F_XHTML_T1_EE.add("col"); //$NON-NLS-1$
	}
	/*
	 * Empty XHTML Frameset 1.0 elements
	 * derived from http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd
	 */
	private static final ArrayList F_XHTML_F1_EE = new ArrayList(13);
	static {
		F_XHTML_F1_EE.add("base"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("meta"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("link"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("frame"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("hr"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("br"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("basefont"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("param"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("img"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("area"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("input"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("isindex"); //$NON-NLS-1$
		F_XHTML_F1_EE.add("col"); //$NON-NLS-1$
	}
	private static final char F_TAG_OPEN = '<';
	private static final char F_TAG_CLOSE = '>';
	private static final char F_TAG_SLASH = '/';
	private static final char F_TAG_SPACE = ' ';
	private static final char F_TAG_DQUOTE = '"';
	
	
	private String fTagName;
	private String fCurrAttName;
	private ArrayList fAttribNames;
	private ArrayList fAttribValues;
	private boolean fIsClosingTag;
	private boolean fIsEmptyTag;
	private int fDTDType;
	
	public XHTMLTag(String tagName, int type) {
		fTagName = extractTagName(tagName);
		fDTDType = type;
		fAttribNames = new ArrayList();
		fAttribValues = new ArrayList();
	}

	private String extractTagName(String tagName) {
		if (tagName == null || tagName.length() == 0)
			return null;
		if (tagName.charAt(0) == F_TAG_OPEN) {
			if (tagName.length() == 1)
				return null;
			tagName = tagName.substring(1);
		}
		if (tagName.charAt(0) == F_TAG_SLASH) {
			if (tagName.length() == 1)
				return null;
			tagName = tagName.substring(1);
			fIsClosingTag = true;
		}
		return tagName.toLowerCase();
	}
	
	protected void eatToken(Token token) {
		switch (token.kind) {
		case HTMLParserConstants.ArgName:
			expandLeftoverAttribute();
			// set the attribute name or mark tag as empty
			if (token.image.length() == 1
					&& token.image.charAt(0) == F_TAG_SLASH) {
				fCurrAttName = null;
				fIsEmptyTag = true;
			} else
				fCurrAttName = token.image;
			break;
		case HTMLParserConstants.ArgValue:
		case HTMLParserConstants.Quote1Text:
		case HTMLParserConstants.Quote2Text:
			// found the value: add the attribute
			addAttribute(fCurrAttName, token.image);
			break;
		}
	}
	
	public void addAttribute(String attName, String attValue) {
		if (attName == null)
			return;
		String name = attName.toLowerCase();
		// remove old attribute - using list to maintain attribute index order
		int index = fAttribNames.size();
		if (fAttribNames.contains(name)) {
			index = fAttribNames.indexOf(name);
			fAttribNames.remove(index);
			fAttribValues.remove(index);
		}
		fAttribNames.add(index, attName.toLowerCase());
		fAttribValues.add(index, attValue != null ? attValue : attName);
		// reset name
		fCurrAttName = null;
	}
	
	public void expandLeftoverAttribute() {
		if (fCurrAttName != null)
			addAttribute(fCurrAttName, null);
	}

	public void writeClosed(PrintWriter pw) {
		if (fTagName == null)
			return;
		pw.write(F_TAG_OPEN);
		pw.write(F_TAG_SLASH);
		pw.write(fTagName);
		pw.write(F_TAG_CLOSE);
	}
	
	public void write(PrintWriter pw) {
		String tag = toString();
		if (tag != null)
			pw.write(tag);
	}
	
	public String toString() {
		// don't write tags which weren't initialized properly
		if (fTagName == null)
			return null;
		StringBuffer sb = new StringBuffer();
		sb.append(F_TAG_OPEN);
		if (fIsClosingTag)
			sb.append(F_TAG_SLASH);
		sb.append(fTagName);
		if (!fIsClosingTag) {
			for (int i = 0; i < fAttribNames.size(); i++) {
				sb.append(F_TAG_SPACE);
				sb.append(fAttribNames.get(i).toString());
				sb.append('=');
				sb.append(F_TAG_DQUOTE);
				sb.append(fAttribValues.get(i).toString());
				sb.append(F_TAG_DQUOTE);
			}
			if (isEmptyTag()) {
				sb.append(F_TAG_SPACE);
				sb.append(F_TAG_SLASH);
			}
		}
		
		sb.append(F_TAG_CLOSE);
		return sb.toString();
	}
	
	public boolean isClosingTag() {
		return fIsClosingTag;
	}
	
	public boolean isEmptyTag() {
		if (fIsEmptyTag)
			return true;
		switch(fDTDType) {
		case XHTMLConverter.XHTML_STRICT:
			return F_XHTML_S1_EE.contains(fTagName);
		case XHTMLConverter.XHTML_TRANSITIONAL:
			return F_XHTML_T1_EE.contains(fTagName);
		case XHTMLConverter.XHTML_FRAMESET:
			return F_XHTML_F1_EE.contains(fTagName);
		}
		return false;
	}

}
