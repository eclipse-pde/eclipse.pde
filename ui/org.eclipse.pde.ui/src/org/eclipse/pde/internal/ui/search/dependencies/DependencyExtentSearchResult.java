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
package org.eclipse.pde.internal.ui.search.dependencies;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.search.SearchResult;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

public class DependencyExtentSearchResult extends SearchResult implements IEditorMatchAdapter {

	/**
	 * @param query
	 */
	public DependencyExtentSearchResult(ISearchQuery query) {
		super(query);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getEditorMatchAdapter()
	 */
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getFileMatchAdapter()
	 */
	public IFileMatchAdapter getFileMatchAdapter() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.IEditorMatchAdapter#isShownInEditor(org.eclipse.search.ui.text.Match, org.eclipse.ui.IEditorPart)
	 */
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.IEditorMatchAdapter#computeContainedMatches(org.eclipse.search.ui.text.AbstractTextSearchResult, org.eclipse.ui.IEditorPart)
	 */
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		IEditorInput editorInput = editor.getEditorInput();
		IJavaElement element = (IJavaElement) editorInput.getAdapter(IJavaElement.class);
		if (element != null) {
			Set matches = new HashSet();
			collectMatches(matches, element);
			return (Match[]) matches.toArray(new Match[matches.size()]);
		}
		return super.computeContainedMatches(result, editor);

	}

	private void collectMatches(Set matches, IJavaElement element) {
		Match[] m = getMatches(element);
		if (m.length != 0) {
			for (int i = 0; i < m.length; i++) {
				matches.add(m[i]);
			}
		}
		if (element instanceof IParent) {
			IParent parent = (IParent) element;
			try {
				IJavaElement[] children = parent.getChildren();
				for (int i = 0; i < children.length; i++) {
					collectMatches(matches, children[i]);
				}
			} catch (JavaModelException e) {
				// we will not be tracking these results
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getLabel()
	 */
	public String getLabel() {
		int count = getMatchCount();
		return fQuery.getLabel() + " - " + count + " " + (count == 1 ? PDEUIMessages.DependencyExtentSearchResult_dependency : PDEUIMessages.DependencyExtentSearchResult_dependencies); //$NON-NLS-1$ //$NON-NLS-2$  
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getTooltip()
	 */
	public String getTooltip() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return PDEPluginImages.DESC_PSEARCH_OBJ;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getQuery()
	 */
	public ISearchQuery getQuery() {
		return fQuery;
	}

}
