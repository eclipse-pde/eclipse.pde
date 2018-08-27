/*******************************************************************************
 *  Copyright (c) 2005, 2016 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
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

public class DependencyExtentSearchResult extends SearchResult {

	/**
	 * @param query
	 */
	public DependencyExtentSearchResult(ISearchQuery query) {
		super(query);
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return this;
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		return null;
	}

	@Override
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		return true;
	}

	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		IEditorInput editorInput = editor.getEditorInput();
		IJavaElement element = editorInput.getAdapter(IJavaElement.class);
		if (element != null) {
			Set<Match> matches = new HashSet<>();
			collectMatches(matches, element);
			return matches.toArray(new Match[matches.size()]);
		}
		return super.computeContainedMatches(result, editor);

	}

	private void collectMatches(Set<Match> matches, IJavaElement element) {
		Match[] m = getMatches(element);
		if (m.length != 0) {
			for (Match elementMatch : m) {
				matches.add(elementMatch);
			}
		}
		if (element instanceof IParent) {
			IParent parent = (IParent) element;
			try {
				IJavaElement[] children = parent.getChildren();
				for (IJavaElement child : children) {
					collectMatches(matches, child);
				}
			} catch (JavaModelException e) {
				// we will not be tracking these results
			}
		}
	}

	@Override
	public String getLabel() {
		int count = getMatchCount();
		return fQuery.getLabel() + " - " + count + " " + (count == 1 ? PDEUIMessages.DependencyExtentSearchResult_dependency : PDEUIMessages.DependencyExtentSearchResult_dependencies); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public String getTooltip() {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return PDEPluginImages.DESC_PSEARCH_OBJ;
	}

	@Override
	public ISearchQuery getQuery() {
		return fQuery;
	}

}
