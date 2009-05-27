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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.Match;

public class DependencyExtentOperation {

	class TypeReferenceSearchRequestor extends SearchRequestor {
		boolean fUsed = false;

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org.eclipse.jdt.core.search.SearchMatch)
		 */
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
				fUsed = true;
			}
		}

		public boolean containMatches() {
			return fUsed;
		}
	}

	class TypeDeclarationSearchRequestor extends SearchRequestor {

		private Match fMatch;

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org.eclipse.jdt.core.search.SearchMatch)
		 */
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			if (!match.isInsideDocComment())
				fMatch = new Match(match.getElement(), Match.UNIT_CHARACTER, match.getOffset(), match.getLength());
		}

		public Match getMatch() {
			return fMatch;
		}
	}

	private DependencyExtentSearchResult fSearchResult;
	private String fImportID;
	private IPluginModelBase fModel;
	private IProject fProject;

	public DependencyExtentOperation(IProject project, String importID, ISearchResult searchResult) {
		fSearchResult = (DependencyExtentSearchResult) searchResult;
		fProject = project;
		fImportID = importID;
		fModel = PluginRegistry.findModel(project);
	}

	public void execute(IProgressMonitor monitor) {
		IPluginModelBase[] plugins = PluginJavaSearchUtil.getPluginImports(fImportID);
		monitor.beginTask(PDEUIMessages.DependencyExtentOperation_searching + " " + fImportID + "...", 10); //$NON-NLS-1$//$NON-NLS-2$ 
		checkForJavaDependencies(plugins, new SubProgressMonitor(monitor, 9));
		for (int i = 0; i < plugins.length; i++) {
			checkForExtensionPointsUsed(plugins[i]);
		}
		monitor.done();
	}

	private void checkForExtensionPointsUsed(IPluginModelBase model) {
		IPluginExtensionPoint[] extPoints = model.getPluginBase().getExtensionPoints();
		for (int i = 0; i < extPoints.length; i++) {
			findMatches(extPoints[i]);
		}
	}

	private void findMatches(IPluginExtensionPoint point) {
		String fullID = point.getFullId();
		if (fullID == null)
			return;

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			if (fullID.equals(extensions[i].getPoint())) {
				int line = ((ISourceObject) extensions[i]).getStartLine() - 1;
				if (line >= 0) {
					fSearchResult.addMatch(new Match(point, Match.UNIT_LINE, line, 1));
					break;
				}
			}
		}
	}

	private void checkForJavaDependencies(IPluginModelBase[] models, IProgressMonitor monitor) {
		try {
			if (!fProject.hasNature(JavaCore.NATURE_ID))
				return;

			IJavaProject jProject = JavaCore.create(fProject);
			IPackageFragment[] packageFragments = PluginJavaSearchUtil.collectPackageFragments(models, jProject, true);
			monitor.beginTask("", packageFragments.length); //$NON-NLS-1$
			SearchEngine engine = new SearchEngine();
			for (int i = 0; i < packageFragments.length; i++) {
				if (monitor.isCanceled())
					break;
				IPackageFragment pkgFragment = packageFragments[i];
				monitor.subTask(PDEUIMessages.DependencyExtentOperation_inspecting + " " + pkgFragment.getElementName()); //$NON-NLS-1$ 
				if (pkgFragment.hasChildren()) {
					IJavaElement[] children = pkgFragment.getChildren();
					for (int j = 0; j < children.length; j++) {
						if (monitor.isCanceled())
							break;
						IJavaElement child = children[j];
						IType[] types = new IType[0];
						if (child instanceof IClassFile) {
							types = new IType[] {((IClassFile) child).getType()};
						} else if (child instanceof ICompilationUnit) {
							types = ((ICompilationUnit) child).getTypes();
						}
						if (types.length > 0)
							searchForTypesUsed(engine, child, types, PluginJavaSearchUtil.createSeachScope(jProject));
					}
				}
				monitor.worked(1);
			}
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
	}

	private void searchForTypesUsed(SearchEngine engine, IJavaElement parent, IType[] types, IJavaSearchScope scope) throws CoreException {
		for (int i = 0; i < types.length; i++) {
			if (types[i].isAnonymous())
				continue;
			TypeReferenceSearchRequestor requestor = new TypeReferenceSearchRequestor();
			engine.search(SearchPattern.createPattern(types[i], IJavaSearchConstants.REFERENCES), new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, scope, requestor, null);
			if (requestor.containMatches()) {
				TypeDeclarationSearchRequestor decRequestor = new TypeDeclarationSearchRequestor();
				engine.search(SearchPattern.createPattern(types[i], IJavaSearchConstants.DECLARATIONS), new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, SearchEngine.createJavaSearchScope(new IJavaElement[] {parent}), decRequestor, null);
				Match match = decRequestor.getMatch();
				if (match != null)
					fSearchResult.addMatch(match);
			}
		}

	}

}
