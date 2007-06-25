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

import java.util.HashSet;

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
	
	/**
	 * @param base
	 * @param set
	 */
	private static void addNumberToBase(StringBuffer base, HashSet set) {
		if (set.size() > 0) {
			// Limit on the number of auto-generated item numbers to check for
			int limit = 100;
			// Check the set for the numbers encountered and generate the 
			// lowest number accordingly
			if (set.contains(new Integer(0)) == false) {
				// Use base
			} else {
				for (int x = 1; x < limit; x++) {
					// Check if the number was already used to auto-generate an
					// existing item
					if (set.contains(new Integer(x)) == false) {
						base.append(" ("); //$NON-NLS-1$
						base.append(x);
						base.append(")"); //$NON-NLS-1$
						break;
					}
				}
			}
		}
	}
	
	/**
	 * @param base
	 * @param set
	 * @param title
	 */
	private static void compareTitleWithBase(String base, HashSet set, String title) {
		// Check to see it the name starts with the base
		if (title.startsWith(base)) {
			// space, (, number, )
			int minSizeNumAddOn = 4;				
			// We found a possible auto-generated name
			// Determine number
			if (title.length() >= (base.length() + minSizeNumAddOn)) {
				// We skipped the space
				String numPart = title.substring(base.length() + 1);
				// We found an auto-generated name
				if (numPart.charAt(0) == '(') {
					StringBuffer buffer = new StringBuffer();
					// Parse the number between the brackets
					for (int j = 1; j < numPart.length(); j++) {
						char current = numPart.charAt(j);
						// Make sure its a digit
						if (Character.isDigit(current)) {
							buffer.append(current);
						} else {
							// Break on non digits including ')'
							break;
						}
					}
					// Convert the number we found into an actual number
					if (buffer.length() > 0) {
						set.add(new Integer(buffer.toString()));
					}
				}
				
			} else {
				// No number to parse
				// Assume it is just base
				set.add(new Integer(0));
			}
		}
	}

	/**
	 * @return
	 */
	public static String generateName(String[] names, String base) {
		StringBuffer result = new StringBuffer(base);
		// Used to track auto-generated numbers used
		HashSet set = new HashSet();

		// Linear search O(n).  
		// Performance hit unnoticeable because number of items per cheatsheet
		// should be minimal.
		for (int i = 0; i < names.length; i++) {
			PDELabelUtility.compareTitleWithBase(base, set, names[i]);
		}
		// Add an auto-generated number
		PDELabelUtility.addNumberToBase(result, set);
		
		return result.toString();
	}
}
