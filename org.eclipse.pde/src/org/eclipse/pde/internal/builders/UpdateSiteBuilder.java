/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.*;

public class UpdateSiteBuilder extends IncrementalProjectBuilder {
	public static final String BUILDERS_VERIFYING = "Builders.verifying"; //$NON-NLS-1$
	public static final String BUILDERS_UPDATING = "Builders.updating"; //$NON-NLS-1$

	class DeltaVisitor implements IResourceDeltaVisitor {
		private IProgressMonitor monitor;
		public DeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();

			if (resource instanceof IProject) {
				// Only check projects with feature nature
				IProject project = (IProject) resource;
				try {
					return (project.hasNature(PDE.SITE_NATURE));
				} catch (CoreException e) {
					PDE.logException(e);
					return false;
				}
			}
			if (resource instanceof IFile) {
				// see if this is it
				IFile candidate = (IFile) resource;
				if (isSiteFile(candidate)) {
					// That's it, but only check it if it has been added or changed
					if (delta.getKind() != IResourceDelta.REMOVED) {
						checkFile(candidate, monitor);
						return true;
					}
				}
			}
			return true;
		}
	}

	public UpdateSiteBuilder() {
		super();
	}
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
		throws CoreException {

		IResourceDelta delta = null;
		if (kind != FULL_BUILD)
			delta = getDelta(getProject());

		if (delta == null || kind == FULL_BUILD) {
			// Full build
			IProject project = getProject();
			IFile file = project.getFile("site.xml"); //$NON-NLS-1$
			if (file.exists()) {
				checkFile(file, monitor);
			}
		} else {
			delta.accept(new DeltaVisitor(monitor));
		}
		return null;
	}
	
	private void checkFile(IFile file, IProgressMonitor monitor) {
		String message =
			PDE.getFormattedMessage(BUILDERS_VERIFYING, file.getFullPath().toString());
		monitor.subTask(message);
		PluginErrorReporter reporter = new PluginErrorReporter(file);
		ValidatingSAXParser.parse(file, reporter);
		if (reporter.getErrorCount() == 0) {
			validateFile(file, reporter);
		}
		monitor.subTask(PDE.getResourceString(BUILDERS_UPDATING));
		monitor.done();
	}
	
	private boolean isSiteFile(IFile file) {
		return file.getParent().equals(file.getProject())
			&& file.getName().toLowerCase().equals("site.xml"); //$NON-NLS-1$
	}

	private void validateFile(IFile file, PluginErrorReporter reporter) {
		WorkspaceSiteModel model = new WorkspaceSiteModel(file);
		model.load();
		if (model.isLoaded()) {
			ISite site = model.getSite();
			if (site != null) {
				validateRequiredAttributes(site, reporter);
			}
		}
	}
	private void validateRequiredAttributes(
		ISite site,
		PluginErrorReporter reporter) {
		ISiteFeature[] features = site.getFeatures();
		for (int i = 0; i < features.length; i++) {
			ISiteFeature feature = features[i];
			assertNotNull(
				"url", //$NON-NLS-1$
				"feature", //$NON-NLS-1$
				getLine(feature),
				feature.getURL(),
				reporter);
			ISiteCategory[] categories = feature.getCategories();
			for (int j = 0; j < categories.length; j++) {
				ISiteCategory category = categories[j];
				assertNotNull(
					"name", //$NON-NLS-1$
					"category", //$NON-NLS-1$
					getLine(category),
					category.getName(),
					reporter);
			}
		}
		ISiteArchive[] archives = site.getArchives();
		for (int i = 0; i < archives.length; i++) {
			ISiteArchive archive = archives[i];
			assertNotNull(
				"path", //$NON-NLS-1$
				"archive", //$NON-NLS-1$
				getLine(archive),
				archive.getPath(),
				reporter);
			assertNotNull(
				"url", //$NON-NLS-1$
				"archive", //$NON-NLS-1$
				getLine(archive),
				archive.getURL(),
				reporter);
		}
		ISiteCategoryDefinition[] defs = site.getCategoryDefinitions();
		for (int i = 0; i < defs.length; i++) {
			ISiteCategoryDefinition def = defs[i];
			assertNotNull(
				"name", //$NON-NLS-1$
				"category-def", //$NON-NLS-1$
				getLine(def),
				def.getName(),
				reporter);
			assertNotNull(
				"label", //$NON-NLS-1$
				"category-def", //$NON-NLS-1$
				getLine(def),
				def.getLabel(),
				reporter);
		}
	}

	private static int getLine(ISiteObject object) {
		int line = -1;
		if (object instanceof ISourceObject) {
			line = ((ISourceObject) object).getStartLine();
		}
		return line;
	}

	private static void assertNotNull(
		String att,
		String el,
		int line,
		String value,
		PluginErrorReporter reporter) {
		if (value == null) {
			String message =
				PDE.getFormattedMessage(
					"Builders.manifest.missingRequired", //$NON-NLS-1$
					new String[] { att, el });
			reporter.reportError(message, line);
		}
	}
}
