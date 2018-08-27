/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof AbstractTextSearchResult)
				return ((AbstractTextSearchResult) inputElement).getElements();
			return new Object[0];
		}


		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fTableViewer = (TableViewer) viewer;
			fSearchResult = (AbstractTextSearchResult) newInput;
		}

		public void clear() {
			fTableViewer.refresh();
		}

		public void elementsChanged(Object[] updatedElements) {
			for (Object updatedElement : updatedElements) {
				if (fSearchResult.getMatchCount(updatedElement) > 0) {
					if (fTableViewer.testFindItem(updatedElement) != null)
						fTableViewer.refresh(updatedElement);
					else
						fTableViewer.add(updatedElement);
				} else {
					fTableViewer.remove(updatedElement);
				}
			}
		}
	}

	private ContentProvider fContentProvider;

	public AbstractSearchResultPage() {
		super(AbstractTextSearchViewPage.FLAG_LAYOUT_FLAT);
	}

	@Override
	protected void elementsChanged(Object[] objects) {
		if (fContentProvider != null && fContentProvider.fSearchResult != null)
			fContentProvider.elementsChanged(objects);
	}

	@Override
	protected void clear() {
		if (fContentProvider != null)
			fContentProvider.clear();
	}

	@Override
	protected void configureTreeViewer(TreeViewer viewer) {
		throw new IllegalStateException("Doesn't support tree mode."); //$NON-NLS-1$
	}

	@Override
	protected void configureTableViewer(TableViewer viewer) {
		viewer.setComparator(createViewerComparator());
		viewer.setLabelProvider(createLabelProvider());
		fContentProvider = new ContentProvider();
		viewer.setContentProvider(fContentProvider);
	}

	protected abstract ILabelProvider createLabelProvider();

	protected abstract ViewerComparator createViewerComparator();

}
