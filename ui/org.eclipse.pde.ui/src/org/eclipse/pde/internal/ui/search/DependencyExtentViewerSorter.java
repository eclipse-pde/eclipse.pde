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
						return 2;
					return 1;
				}
			}
		} catch (JavaModelException e) {
		}
		return 0;
	}


}
