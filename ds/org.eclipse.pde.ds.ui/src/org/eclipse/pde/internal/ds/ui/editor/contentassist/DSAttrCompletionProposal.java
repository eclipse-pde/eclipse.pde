/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.contentassist;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.ui.SharedImages;
import org.eclipse.swt.graphics.Point;

public class DSAttrCompletionProposal extends TypeCompletionProposal implements
		ICompletionProposal {

	public DSAttrCompletionProposal(String string,
			int startOffset, int length) {
		super(getReplacementString(string), SharedImages
				.getImage(SharedImages.DESC_ATTR), string,
				startOffset, length);
	}

	public Point getSelection(IDocument document) {
		return new Point(this.fBeginInsertPoint + fReplacementString.length()
				- 1, 0);
	}

	/**
	 * Returns a String to be replaced by the Content Assist with default values
	 * or with a repeated String if there is no default value.
	 * 
	 * Example: enabled="true" and entry="entry"
	 * 
	 * @param attribute
	 *            the name of attribute
	 * @return a String containing the replacementString
	 */
	private static String getReplacementString(String attribute) {
		String replacementString = null;
		if (attribute == null) {
			return null;
		}
		String string1 = "=\""; //$NON-NLS-1$
		String string2 = "\""; //$NON-NLS-1$

		if (attribute.equals(IDSConstants.ATTRIBUTE_COMPONENT_ENABLED)) {
			replacementString = attribute + string1
 + IDSConstants.VALUE_TRUE
					+ string2;
		} else if (attribute
				.equals(IDSConstants.ATTRIBUTE_COMPONENT_IMMEDIATE)) {
			replacementString = attribute + string1
 + IDSConstants.VALUE_FALSE
					+ string2;
		} else if (attribute.equals(IDSConstants.ATTRIBUTE_PROPERTY_TYPE)) {
			replacementString = attribute + string1
					+ IDSConstants.VALUE_PROPERTY_TYPE_STRING + string2;
		} else if (attribute.equals(IDSConstants.ATTRIBUTE_SERVICE_FACTORY)) {
			replacementString = attribute + string1
 + IDSConstants.VALUE_FALSE
					+ string2;
		} else if (attribute
				.equals(IDSConstants.ATTRIBUTE_REFERENCE_TARGET)) {
			replacementString = attribute + string1
					+ IDSConstants.VALUE_DEFAULT_TARGET + string2;
		} else if (attribute
				.equals(IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY)) {
			replacementString = attribute + string1
					+ IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_ONE
					+ string2;
		} else if (attribute
				.equals(IDSConstants.ATTRIBUTE_REFERENCE_POLICY)) {
			replacementString = attribute + string1
					+ IDSConstants.VALUE_REFERENCE_POLICY_STATIC + string2;
		} else if (attribute
				.equals(IDSConstants.ATTRIBUTE_COMPONENT_CONFIGURATION_POLICY)) {
			replacementString = attribute + string1
					+ IDSConstants.VALUE_CONFIGURATION_POLICY_OPTIONAL
					+ string2;
		} else {
			replacementString = attribute + string1 + attribute
 + string2;
		}
		return replacementString;
	}

}
