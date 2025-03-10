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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.Match;

public class DependencyExtentOperation {

	static class TypeReferenceSearchRequestor extends SearchRequestor {
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

	static class TypeDeclarationSearchRequestor extends SearchRequestor {

		private Match fMatch;

		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			if (!match.isInsideDocComment()) {
				fMatch = new Match(match.getElement(), Match.UNIT_CHARACTER, match.getOffset(), match.getLength());
			}
		}

		public Match getMatch() {
			return fMatch;
		}
	}

	private final DependencyExtentSearchResult fSearchResult;
	private final String fImportID;
	private final IPluginModelBase fModel;
	private final IProject fProject;

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
		if (fullID == null) {
			return;
		}

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
			if (!fProject.hasNature(JavaCore.NATURE_ID)) {
				return;
			}

			IJavaProject jProject = JavaCore.create(fProject);
			IPackageFragment[] packageFragments = PluginJavaSearchUtil.collectPackageFragments(models, jProject, true);
			monitor.beginTask("", packageFragments.length); //$NON-NLS-1$
			SearchEngine engine = new SearchEngine();
			for (IPackageFragment pkgFragment : packageFragments) {
				if (monitor.isCanceled()) {
					break;
				}
				monitor.subTask(PDEUIMessages.DependencyExtentOperation_inspecting + " " + pkgFragment.getElementName()); //$NON-NLS-1$
				if (pkgFragment.hasChildren()) {
					IJavaElement[] children = pkgFragment.getChildren();
					for (IJavaElement child : children) {
						if (monitor.isCanceled()) {
							break;
						}
						IType[] types = new IType[0];
						if (child instanceof IOrdinaryClassFile) {
							types = new IType[] {((IOrdinaryClassFile) child).getType()};
						} else if (child instanceof ICompilationUnit) {
							types = ((ICompilationUnit) child).getTypes();
						}
						if (types.length > 0) {
							searchForTypesUsed(engine, child, types, PluginJavaSearchUtil.createSeachScope(jProject));
						}
					}
				}
				monitor.worked(1);
			}
		} catch (CoreException e) {
		}
	}

	private void searchForTypesUsed(SearchEngine engine, IJavaElement parent, IType[] types, IJavaSearchScope scope) throws CoreException {
		for (IType type : types) {
			if (type.isAnonymous()) {
				continue;
			}
			TypeReferenceSearchRequestor requestor = new TypeReferenceSearchRequestor();
			SearchPattern pattern = SearchPattern.createPattern(type, IJavaSearchConstants.REFERENCES);
			if (pattern == null) {
				continue;
			}
			engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, scope,
					requestor, null);
			if (requestor.containMatches()) {
				TypeDeclarationSearchRequestor decRequestor = new TypeDeclarationSearchRequestor();
				pattern = SearchPattern.createPattern(type, IJavaSearchConstants.DECLARATIONS);
				if (pattern == null) {
					continue;
				}
				engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
						SearchEngine.createJavaSearchScope(new IJavaElement[] { parent }), decRequestor, null);
				Match match = decRequestor.getMatch();
				if (match != null) {
					fSearchResult.addMatch(match);
				}
			}
		}

	}

}
