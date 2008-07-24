/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 233997
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.ui.SharedImages;
import org.eclipse.pde.internal.ds.ui.editor.contentassist.TypeCompletionProposal;
import org.eclipse.swt.graphics.Point;

public class DSAttrCompletionProposal extends TypeCompletionProposal implements
		ICompletionProposal {

	public DSAttrCompletionProposal(String displayString,
			int startOffset, int length) {
		super(getReplacementString(displayString), SharedImages
				.getImage(SharedImages.DESC_ATTR), displayString,
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
	 * @param displayString
	 *            the name of attribute
	 * @return a String containing the replacementString
	 */
	private static String getReplacementString(String displayString) {
		String replacementString = null;
		if (displayString == null) {
			return null;
		}
		String string1 = "=\""; //$NON-NLS-1$
		String string2 = "\""; //$NON-NLS-1$
		
		if (displayString.equals(IDSConstants.ATTRIBUTE_COMPONENT_ENABLED)) {
			replacementString = displayString + string1
					+ IDSConstants.VALUE_TRUE
					+ string2;
		} else if (displayString.equals(IDSConstants.ATTRIBUTE_PROPERTY_TYPE)) {
			replacementString = displayString + string1
					+ IDSConstants.VALUE_PROPERTY_TYPE_STRING + string2;
		} else if (displayString.equals(IDSConstants.ATTRIBUTE_SERVICE_FACTORY)) {
			replacementString = displayString + string1
					+ IDSConstants.VALUE_FALSE
					+ string2;
		} else if (displayString
				.equals(IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY)) {
			replacementString = displayString + string1
					+ IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_ONE
					+ string2;
		} else if (displayString
				.equals(IDSConstants.ATTRIBUTE_REFERENCE_POLICY)) {
			replacementString = displayString + string1
					+ IDSConstants.VALUE_REFERENCE_POLICY_STATIC + string2;
		} else {
			replacementString = displayString + string1 + displayString
					+ string2;
		}
		return replacementString;
	}

}
