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
package org.eclipse.pde.internal.ui.search;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.search.ui.ISearchResultViewEntry;

/**
 * Insert the type's description here.
 * @see ViewerSorter
 */
public class DependencyExtentViewerSorter extends ViewerSorter {
	/**
	 * The constructor.
	 */
	public DependencyExtentViewerSorter() {
	}

	public int category(Object element) {
		try {
			if (element instanceof ISearchResultViewEntry) {
				Object object = ((ISearchResultViewEntry)element).getGroupByKey();
				if (object instanceof IType) {
					if (((IType)object).isClass())
						return 1;
					return 0;
				}
			}
		} catch (JavaModelException e) {
		}
		return 2;
	}


}
