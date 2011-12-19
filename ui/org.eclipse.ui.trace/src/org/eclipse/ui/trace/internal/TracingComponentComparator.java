/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.ui.trace.internal.datamodel.TracingNode;

/**
 * A Comparator for ordering the list of traceable bundles visible in a viewer in alphabetical order
 */
public class TracingComponentComparator extends ViewerComparator {

	/**
	 * Either the first or second object are null - so compare them to ensure they are put in the right order.
	 * 
	 * @param object1
	 *            The first object to compare
	 * @param object2
	 *            The second object to compare
	 * @return Return 0 if both objects are null; Returns -1 if object1 is null and object2 is not null; Returns 1 if
	 *         object1 is not null and object2 is null.
	 */
	private int compareNullCases(final Object object1, final Object object2) {

		int result = -1;
		// take care of the null cases
		if ((object1 == null) && (object2 == null)) {
			result = 0;
		} else if ((object1 == null) && (object2 != null)) {
			result = -1;
		} else if ((object1 != null) && (object2 == null)) {
			result = 1;
		}
		return result;
	}

	@Override
	public int compare(final Viewer viewer, final Object object1, final Object object2) {

		int result = -1;
		if ((object1 == null) || (object2 == null)) {
			// take care of the null cases
			result = this.compareNullCases(object1, object2);
		} else {
			// neither object are null
			if ((object1 instanceof TracingNode) && (object2 instanceof TracingNode)) {
				String name1 = ((TracingNode) object1).getLabel();
				String name2 = ((TracingNode) object2).getLabel();
				if ((name1 == null) || (name2 == null)) {
					// take care of the null cases
					result = this.compareNullCases(name1, name2);
				} else {
					result = name1.compareTo(name2);
				}
			} else if (object1 instanceof String && object2 instanceof String) {
				String label1 = (String) object1;
				String label2 = (String) object2;
				result = label1.compareTo(label2);
			}
		}
		return result;
	}
}