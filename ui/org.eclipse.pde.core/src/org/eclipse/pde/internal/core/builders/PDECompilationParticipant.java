/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.pde.internal.core.bnd.PdeProjectAnalyzer;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Packages;

public class PDECompilationParticipant extends CompilationParticipant {

	private ThreadLocal<IJavaProject> project = new ThreadLocal<>();
	private ThreadLocal<Analyzer> analyzer = new ThreadLocal<>();

	@Override
	public int aboutToBuild(IJavaProject project) {
		this.project.set(project);
		System.out.println(
				String.format("---- PDECompilationParticipant.aboutToBuild(%s)", project.getProject().getName())); //$NON-NLS-1$
		return READY_FOR_BUILD;
	}

	@Override
	public void buildFinished(IJavaProject project) {
		System.out.println(
				String.format("---- PDECompilationParticipant.buildFinished(%s)", project.getProject().getName())); //$NON-NLS-1$
		this.project.set(null);
		try (Analyzer analyzer = this.analyzer.get()) {
			if (analyzer != null) {
				analyzer.setImportPackage("*"); //$NON-NLS-1$
				analyzer.calcManifest();
				Packages imports = analyzer.getImports();
				if (imports == null) {
					System.out.println("No packages computed!");
				} else {
					checkImportedPackages(imports, project);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.analyzer.set(null);
	}

	protected void checkImportedPackages(Packages imports, IJavaProject javaProject) throws CoreException {
		SearchEngine engine = new SearchEngine();
		IJavaSearchScope searchScope = PluginJavaSearchUtil.createSeachScope(javaProject);
		Set<String> computedPackages = imports.keySet().stream().map(PackageRef::getFQN)
				.collect(Collectors.toSet());
		ReferencesSearch referencesSearch = new ReferencesSearch(engine, searchScope);
		for (String pkg : computedPackages) {
			// TODO here we now want to check if the package is already properly
			// imported by the manifest
			System.out.println("  - " + pkg);
			// if not we need to create an error marker on places where this is
			// used is the given compilation units
			Set<String> search = referencesSearch.search(pkg);
			if (search.isEmpty()) {
				System.out.println("    -> Nothing found in source?!?");
			}
			for (String used : search) {
				System.out.println("    -> " + used);
			}
		}
	}

	@Override
	public void buildStarting(BuildContext[] files, boolean isBatch) {
		IJavaProject javaProject = project.get();
		System.out.println(
				String.format("PDECompilationParticipant.buildStarting(%s)", javaProject.getProject().getName())); //$NON-NLS-1$
		try {
			this.analyzer.set(new PdeProjectAnalyzer(javaProject.getProject(), true));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void cleanStarting(IJavaProject project) {
		System.out.println(
				String.format("---- PDECompilationParticipant.cleanStarting(%s)", project.getProject().getName())); //$NON-NLS-1$
	}

	@Override
	public boolean isActive(IJavaProject project) {
		System.out.println(String.format("PDECompilationParticipant.isActive(%s)", project.getProject().getName())); //$NON-NLS-1$
		return PluginProject.isPluginProject(project.getProject());
	}

	@Override
	public boolean isPostProcessor() {
		return true;
	}

	@Override
	public Optional<byte[]> postProcess(BuildContext file, ByteArrayInputStream bytes) {
		IJavaProject javaProject = project.get();
		Analyzer projectAnalyzer = analyzer.get();
		IPath projectRelativePath = file.getFile().getProjectRelativePath();
		String relativePath = projectRelativePath.toString();
		String base = relativePath.substring(javaProject.getProject().getProjectRelativePath().toString().length());
		System.out.println(String.format("PDECompilationParticipant.postProcess(%s)", file.getFile())); //$NON-NLS-1$
		if (base.startsWith("src/")) {
			// TODO build context should supply the binary name
			base = base.substring("src/".length());
		}
		String name = base.replace(".java", ".class");
		System.out.println(base);
		projectAnalyzer.getJar().putResource(name,
				new EmbeddedResource(bytes.readAllBytes(), System.currentTimeMillis()));
		return super.postProcess(file, bytes);
	}

	private static class ReferencesSearch extends SearchRequestor {
		private final SearchEngine engine;
		private final IJavaSearchScope searchScope;
		private final Set<String> found = new HashSet<>();

		public ReferencesSearch(SearchEngine engine, IJavaSearchScope searchScope) {
			this.engine = engine;
			this.searchScope = searchScope;
		}

		public Set<String> search(String packageName) throws CoreException {
			found.clear();
			SearchPattern pattern = SearchPattern.createPattern(packageName, IJavaSearchConstants.PACKAGE,
					IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH);
			engine.search(pattern, new SearchParticipant[] {
					 SearchEngine.getDefaultSearchParticipant() },
					//TODO it seems there is no way to cancel a compilation participant!
					 searchScope, this, new NullProgressMonitor());
			return Set.copyOf(found);
		}

		@Override
		public void acceptSearchMatch(SearchMatch match) {
			// TODO we must gather more information than a string, is it safe to
			// store the match?
			found.add(String.valueOf(match.getElement()));
		}

	}

}
