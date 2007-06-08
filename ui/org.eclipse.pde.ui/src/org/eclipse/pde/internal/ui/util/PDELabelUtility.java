/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.util;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * PDELabelUtility
 *
 */
public class PDELabelUtility {

	/**
	 * Get the field label text from the label widget created before the
	 * control (assumption).  Use this method when the control's label is
	 * variable or if the label's text is set elsewhere.  Note:  Hyperlink 
	 * label text will not be detected.
	 * @param control
	 * @return
	 */
	public static String getFieldLabel(Control control) {
		// Note:  Does not handle hyperlink labels
		
		// Get the children of the control's parent
		// The control itself should be in there
		// Assuming the control's label is in there as well
		Control[] controls = control.getParent().getChildren();
		// Ensure has controls
		if (controls.length == 0) {
			return null;
		}
		// Me = control
		// Track the index of myself 
		int myIndex = -1;
		// Linearly search controls for myself
		for (int i = 0; i < controls.length; i++) {
			if (controls[i] == control) {
				// Found myself
				myIndex = i;
				break;
			}
		}
		// Ensure I was found and am not the first widget
		if (myIndex <= 0) {
			return null;
		}
		// Assume label is the control created before me
		int labelIndex = myIndex - 1;
		// Ensure the label index points to a label
		if ((controls[labelIndex] instanceof Label) == false) {
			return null;
		}
		// Get the label text
		String labelText = ((Label)controls[labelIndex]).getText(); 
		// Get rid of the trailing colon (if any)
		int colonIndex = labelText.indexOf(':');
		if (colonIndex >= 0) {
			labelText = labelText.replaceAll(":", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// Get rid of mnemonics (if any)
		int ampIndex = labelText.indexOf('&');
		if (ampIndex >= 0) {
			labelText = labelText.replaceAll("&", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		return labelText;
	}
	
	/**
	 * @param qualification
	 * @param message
	 * @return
	 */
	public static String qualifyMessage(String qualification, String message) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(qualification);
		buffer.append(':');
		buffer.append(' ');
		buffer.append(message);
		return buffer.toString();
	}	
	
}
