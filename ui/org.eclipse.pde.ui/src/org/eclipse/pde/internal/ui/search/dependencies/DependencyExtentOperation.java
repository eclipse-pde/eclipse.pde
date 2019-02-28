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
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
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

		@Override
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

		@Override
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
		SubMonitor subMonitor = SubMonitor.convert(monitor,
				PDEUIMessages.DependencyExtentOperation_searching + " " + fImportID + "...", 10); //$NON-NLS-1$//$NON-NLS-2$
		checkForJavaDependencies(plugins, subMonitor.split(9));
		subMonitor.setWorkRemaining(plugins.length);
		for (IPluginModelBase plugin : plugins) {
			checkForExtensionPointsUsed(plugin);
			subMonitor.worked(1);
		}
	}

	private void checkForExtensionPointsUsed(IPluginModelBase model) {
		IPluginExtensionPoint[] extPoints = model.getPluginBase().getExtensionPoints();
		for (IPluginExtensionPoint extPoint : extPoints) {
			findMatches(extPoint);
		}
	}

	private void findMatches(IPluginExtensionPoint point) {
		String fullID = point.getFullId();
		if (fullID == null)
			return;

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		for (IPluginExtension extension : extensions) {
			if (fullID.equals(extension.getPoint())) {
				int line = ((ISourceObject) extension).getStartLine() - 1;
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
			for (IPackageFragment pkgFragment : packageFragments) {
				if (monitor.isCanceled())
					break;
				monitor.subTask(PDEUIMessages.DependencyExtentOperation_inspecting + " " + pkgFragment.getElementName()); //$NON-NLS-1$
				if (pkgFragment.hasChildren()) {
					IJavaElement[] children = pkgFragment.getChildren();
					for (IJavaElement child : children) {
						if (monitor.isCanceled())
							break;
						IType[] types = new IType[0];
						if (child instanceof IOrdinaryClassFile) {
							types = new IType[] {((IOrdinaryClassFile) child).getType()};
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
		}
	}

	private void searchForTypesUsed(SearchEngine engine, IJavaElement parent, IType[] types, IJavaSearchScope scope) throws CoreException {
		for (IType type : types) {
			if (type.isAnonymous())
				continue;
			TypeReferenceSearchRequestor requestor = new TypeReferenceSearchRequestor();
			engine.search(SearchPattern.createPattern(type, IJavaSearchConstants.REFERENCES), new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, scope, requestor, null);
			if (requestor.containMatches()) {
				TypeDeclarationSearchRequestor decRequestor = new TypeDeclarationSearchRequestor();
				engine.search(SearchPattern.createPattern(type, IJavaSearchConstants.DECLARATIONS), new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, SearchEngine.createJavaSearchScope(new IJavaElement[] {parent}), decRequestor, null);
				Match match = decRequestor.getMatch();
				if (match != null)
					fSearchResult.addMatch(match);
			}
		}

	}

}
