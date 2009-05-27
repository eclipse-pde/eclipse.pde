/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.viewers.*;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;

public abstract class AbstractSearchResultPage extends AbstractTextSearchViewPage {

	class ContentProvider implements IStructuredContentProvider {

		private TableViewer fTableViewer;
		private AbstractTextSearchResult fSearchResult;

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof AbstractTextSearchResult)
				return ((AbstractTextSearchResult) inputElement).getElements();
			return new Object[0];
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fTableViewer = (TableViewer) viewer;
			fSearchResult = (AbstractTextSearchResult) newInput;
		}

		public void clear() {
			fTableViewer.refresh();
		}

		public void elementsChanged(Object[] updatedElements) {
			for (int i = 0; i < updatedElements.length; i++) {
				if (fSearchResult.getMatchCount(updatedElements[i]) > 0) {
					if (fTableViewer.testFindItem(updatedElements[i]) != null)
						fTableViewer.refresh(updatedElements[i]);
					else
						fTableViewer.add(updatedElements[i]);
				} else {
					fTableViewer.remove(updatedElements[i]);
				}
			}
		}
	}

	private ContentProvider fContentProvider;

	public AbstractSearchResultPage() {
		super(AbstractTextSearchViewPage.FLAG_LAYOUT_FLAT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#elementsChanged(java.lang.Object[])
	 */
	protected void elementsChanged(Object[] objects) {
		if (fContentProvider != null && fContentProvider.fSearchResult != null)
			fContentProvider.elementsChanged(objects);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#clear()
	 */
	protected void clear() {
		if (fContentProvider != null)
			fContentProvider.clear();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTreeViewer(org.eclipse.jface.viewers.TreeViewer)
	 */
	protected void configureTreeViewer(TreeViewer viewer) {
		throw new IllegalStateException("Doesn't support tree mode."); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTableViewer(org.eclipse.jface.viewers.TableViewer)
	 */
	protected void configureTableViewer(TableViewer viewer) {
		viewer.setComparator(createViewerComparator());
		viewer.setLabelProvider(createLabelProvider());
		fContentProvider = new ContentProvider();
		viewer.setContentProvider(fContentProvider);
	}

	protected abstract ILabelProvider createLabelProvider();

	protected abstract ViewerComparator createViewerComparator();

}
