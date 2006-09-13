/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions;

import java.util.HashSet;

import org.eclipse.jface.action.Action;

/**
 * SimpleCSAbstractAdd
 *
 */
public abstract class SimpleCSAbstractAdd extends Action {

	/**
	 * 
	 */
	public SimpleCSAbstractAdd() {
	}

	/**
	 * @param result
	 * @param set
	 */
	protected void addNumberToBase(StringBuffer base, HashSet set) {
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
	protected void compareTitleWithBase(String base, HashSet set, String title) {
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

}
