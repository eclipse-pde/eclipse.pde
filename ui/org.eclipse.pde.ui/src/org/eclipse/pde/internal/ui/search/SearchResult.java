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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import java.io.File;
import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.*;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class SearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter {
	protected ISearchQuery fQuery;

	public SearchResult(ISearchQuery query) {
		fQuery = query;
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return this;
	}

	@Override
	public String getLabel() {
		int numMatches = getMatchCount();
		return fQuery.getLabel() + " - " + numMatches + " " + (numMatches == 1 ? PDEUIMessages.SearchResult_match : PDEUIMessages.SearchResult_matches); //$NON-NLS-1$ //$NON-NLS-2$
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

	@Override
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		Object element = match.getElement();
		if (element instanceof IPluginObject) {
			return isMatchContained(editor, (IPluginObject) element);
		}
		if (element instanceof IFeaturePlugin) {
			return isMatchContained(editor, (IFeaturePlugin) element);
		}
		return false;
	}

	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		ArrayList<Match> list = new ArrayList<>();
		Object[] objects = result.getElements();
		for (Object o : objects) {
			if (o instanceof IPluginObject) {
				IPluginObject object = (IPluginObject) o;
				if (isMatchContained(editor, object)) {
					Match[] matches = getMatches(object);
					for (Match matche : matches) {
						IDocument document = getDocument(editor, matche);
						if (document != null)
							list.add(ManifestEditorOpener.findExactMatch(document, matche, editor));
					}
				}
			}
			if (o instanceof IFeaturePlugin) {
				IFeaturePlugin object = (IFeaturePlugin) o;
				if (isMatchContained(editor, object)) {
					for (Match match : getMatches(object)) {
						list.add(match);
					}
				}
			}
		}
		return list.toArray(new Match[list.size()]);
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		return null;
	}

	protected boolean isMatchContained(final IEditorPart editor, final IPluginObject object) {
		return isMatchContained(editor, object.getModel().getUnderlyingResource(),
				object.getModel().getInstallLocation());
	}

	protected boolean isMatchContained(final IEditorPart editor, final IFeaturePlugin object) {
		return isMatchContained(editor, object.getModel().getUnderlyingResource(),
				object.getModel().getInstallLocation());
	}

	protected boolean isMatchContained(final IEditorPart editor, final IResource underlyingResource,
			final String installLocation) {
		IFile resource = editor.getEditorInput().getAdapter(IFile.class);
		if (resource != null) {
			IResource objectResource = underlyingResource;
			if (objectResource != null)
				return resource.getProject().equals(objectResource.getProject());
		}
		File file = editor.getEditorInput().getAdapter(File.class);
		if (file != null) {
			IPath path = new Path(installLocation);
			IPath filePath = null;
			if (ICoreConstants.MANIFEST_FILENAME.equals(file.getName()))
				filePath = new Path(file.getParentFile().getParent());
			else if (file.getName().endsWith("jar")) { //$NON-NLS-1$
				filePath = new Path(file.getPath());
			} else {
				filePath = new Path(file.getParent());
			}
			return path.equals(filePath);
		}
		return false;
	}

	protected IDocument getDocument(IEditorPart editor, Match match) {
		IDocument document = null;
		if (editor instanceof ISearchEditorAccess) {
			document = ((ISearchEditorAccess) editor).getDocument(match);
		} else if (editor instanceof ITextEditor) {
			document = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		}
		return document;
	}

}
