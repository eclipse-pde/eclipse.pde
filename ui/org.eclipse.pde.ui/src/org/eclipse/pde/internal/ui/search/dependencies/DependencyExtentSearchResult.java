package org.eclipse.pde.internal.ui.search.dependencies;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.javaeditor.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.search.ui.*;
import org.eclipse.search.ui.text.*;
import org.eclipse.ui.*;


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
		IEditorInput editorInput= editor.getEditorInput();
		if (editorInput instanceof IFileEditorInput)  {
			IFileEditorInput fileEditorInput= (IFileEditorInput) editorInput;
			IFile file = fileEditorInput.getFile();
			if (JavaCore.create(file) != null)
				return computeContainedMatches(result, file);
		} else if (editorInput instanceof IClassFileEditorInput) {
			IClassFileEditorInput classFileEditorInput= (IClassFileEditorInput) editorInput;
			Set matches= new HashSet();
			collectMatches(matches, classFileEditorInput.getClassFile());
			return (Match[]) matches.toArray(new Match[matches.size()]);
		}
		return super.computeContainedMatches(result, editor);

	}
	
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
		IJavaElement javaElement= JavaCore.create(file);
		if (!(javaElement instanceof ICompilationUnit || javaElement instanceof IClassFile))
			return new Match[0];
		Set matches= new HashSet();
		collectMatches(matches, javaElement);
		return (Match[]) matches.toArray(new Match[matches.size()]);
	}

	
	private void collectMatches(Set matches, IJavaElement element) {
		Match[] m= getMatches(element);
		if (m.length != 0) {
			for (int i= 0; i < m.length; i++) {
				matches.add(m[i]);
			}
		}
		if (element instanceof IParent) {
			IParent parent= (IParent) element;
			try {
				IJavaElement[] children= parent.getChildren();
				for (int i= 0; i < children.length; i++) {
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
		return fQuery.getLabel() + " - " + count + (count == 1 ? " dependency" : " dependencies");
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
