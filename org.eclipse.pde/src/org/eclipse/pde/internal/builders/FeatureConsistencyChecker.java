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
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.osgi.framework.*;

public class FeatureConsistencyChecker extends IncrementalProjectBuilder {
	public static final String BUILDERS_VERIFYING = "Builders.verifying"; //$NON-NLS-1$
	public static final String BUILDERS_FEATURE_REFERENCE =
		"Builders.Feature.reference"; //$NON-NLS-1$
	public static final String BUILDERS_FEATURE_FREFERENCE =
		"Builders.Feature.freference"; //$NON-NLS-1$
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
					return (project.hasNature(PDE.FEATURE_NATURE));
				} catch (CoreException e) {
					PDE.logException(e);
					return false;
				}
			}
			if (resource instanceof IFile) {
				// see if this is it
				IFile candidate = (IFile) resource;
				if (isManifestFile(candidate)) {
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
	
	class ReferenceDeltaVisitor implements IResourceDeltaVisitor {
		private boolean interestingChange;
		public ReferenceDeltaVisitor() {
		}

		public boolean isInterestingChange() {
			return interestingChange;
		}

		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();

			if (resource instanceof IProject) {
				// Only check projects with plugin nature
				IProject project = (IProject) resource;
				return (PDE.hasFeatureNature(project));
			}
			if (resource instanceof IFile) {
				// see if this is it
				IFile candidate = (IFile) resource;
				if (isManifestFile(candidate)) {
					interestingChange = true;
					return false;
				}
			}
			return true;
		}
	}

	public FeatureConsistencyChecker() {
		super();
	}
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
		throws CoreException {
		if (PDECore.getDefault().getBundle().getState() != Bundle.ACTIVE || monitor.isCanceled())
			return new IProject[0];

		IResourceDelta delta = null;

		IProject project = getProject();
		if (kind != FULL_BUILD)
			delta = getDelta(project);

		if (delta == null || kind == FULL_BUILD) {
			// Full build
			checkProject(project, monitor);
		} else {
			delta.accept(new DeltaVisitor(monitor));
		}
		return new IProject[0];
	}

	private void checkProject(IProject project, IProgressMonitor monitor) {
		IFile file = project.getFile("feature.xml"); //$NON-NLS-1$
		if (file.exists()) {
			checkFile(file, monitor);
		}
	}
	
	private void checkFile(IFile file, IProgressMonitor monitor) {
		String message =
			PDE.getFormattedMessage(BUILDERS_VERIFYING, file.getFullPath().toString());
		monitor.subTask(message);
		PluginErrorReporter reporter = new PluginErrorReporter(file);
		ValidatingSAXParser.parse(file, reporter, true);
		if (reporter.getErrorCount() == 0) {
			validateFeature(file, reporter);
		}
		monitor.subTask(PDE.getResourceString(BUILDERS_UPDATING));
		monitor.done();
	}
	
	private boolean isManifestFile(IFile file) {
		return file.getParent().equals(file.getProject()) && file.getName().toLowerCase().equals("feature.xml"); //$NON-NLS-1$
	}
	
	private boolean isValidReference(IFeaturePlugin plugin) {
		String id = plugin.getId();
		if (id != null && id.trim().length() > 0) {
			ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(id);
			if (entry != null) {
				if (plugin.isFragment())
					return entry.getActiveModel() instanceof IFragmentModel;
				return entry.getActiveModel() instanceof IPluginModel;
			}
		}
		return false;
	}
	
	private boolean isValidReference(IFeatureChild child) {
		WorkspaceModelManager manager =
			PDECore.getDefault().getWorkspaceModelManager();
		IFeatureModel[] models = manager.getFeatureModels();

		for (int i = 0; i < models.length; i++) {
			IFeatureModel model = models[i];
			if (model.getFeature().getId().equals(child.getId())) {
				return true;
			}
		}
		return false;
	}

	private void validateFeature(IFile file, PluginErrorReporter reporter) {
		WorkspaceFeatureModel model = new WorkspaceFeatureModel(file);
		model.load();
		if (model.isLoaded()) {
			IFeature feature = model.getFeature();
			validateRequiredAttributes(feature, reporter);
			if (reporter.getErrorCount() > 0)
				return;
			testPluginReferences(feature, reporter);
			testFeatureReferences(feature, reporter);
		}
	}

	private void testPluginReferences(
		IFeature feature,
		PluginErrorReporter reporter) {
		IFeaturePlugin[] plugins = feature.getPlugins();
		int flag = CompilerFlags.getFlag(CompilerFlags.F_UNRESOLVED_PLUGINS);
		if (flag==CompilerFlags.IGNORE) return;

		for (int i = 0; i < plugins.length; i++) {
			IFeaturePlugin plugin = plugins[i];
			if (isValidReference(plugin) == false) {
				String message =
					PDE.getFormattedMessage(
						BUILDERS_FEATURE_REFERENCE,
						plugin.getLabel());
				reporter.report(
					message,
					getLine(plugin),
					flag);
			}
		}
	}
	
	private void testFeatureReferences(
		IFeature feature,
		PluginErrorReporter reporter) {
		IFeatureChild[] included = feature.getIncludedFeatures();
		int flag = CompilerFlags.getFlag(CompilerFlags.F_UNRESOLVED_FEATURES);
		if (flag==CompilerFlags.IGNORE) return;

		for (int i = 0; i < included.length; i++) {
			IFeatureChild child = included[i];
			if (isValidReference(child) == false) {
				String message =
					PDE.getFormattedMessage(
						BUILDERS_FEATURE_FREFERENCE,
						child.getId());
				reporter.report(
					message,
					getLine(child),
					flag);
			}
		}
	}

	private void validateRequiredAttributes(
		IFeature feature,
		PluginErrorReporter reporter) {
		assertNotNull(
			"id", //$NON-NLS-1$
			"feature", //$NON-NLS-1$
			getLine(feature),
			feature.getId(),
			reporter);
		assertNotNull(
			"version", //$NON-NLS-1$
			"feature", //$NON-NLS-1$
			getLine(feature),
			feature.getVersion(),
			reporter);

		IFeatureChild[] children = feature.getIncludedFeatures();
		for (int i = 0; i < children.length; i++) {
			IFeatureChild child = children[i];
			assertNotNull(
				"id", //$NON-NLS-1$
				"includes", //$NON-NLS-1$
				getLine(child),
				child.getId(),
				reporter);
			assertNotNull(
				"version", //$NON-NLS-1$
				"includes", //$NON-NLS-1$
				getLine(child),
				child.getVersion(),
				reporter);
		}
		IFeaturePlugin[] plugins = feature.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IFeaturePlugin plugin = plugins[i];
			assertNotNull(
				"id", //$NON-NLS-1$
				"plugin", //$NON-NLS-1$
				getLine(plugin),
				plugin.getId(),
				reporter);
			assertNotNull(
				"version", //$NON-NLS-1$
				"plugin", //$NON-NLS-1$
				getLine(plugin),
				plugin.getVersion(),
				reporter);
		}
		IFeatureData[] data = feature.getData();
		for (int i = 0; i < data.length; i++) {
			IFeatureData entry = data[i];
			assertNotNull(
				"id", //$NON-NLS-1$
				"data", //$NON-NLS-1$
				getLine(entry),
				entry.getId(),
				reporter);
		}
		IFeatureImport[] fimports = feature.getImports();
		for (int i = 0; i < fimports.length; i++) {
			IFeatureImport fimport = fimports[i];
			if (fimport.getType()==IFeatureImport.PLUGIN) {
				assertNotNull(
				"plugin", //$NON-NLS-1$
				"import", //$NON-NLS-1$
				getLine(fimport),
				fimport.getId(),
				reporter);
			}
		}
	}

	private static int getLine(IFeatureObject object) {
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
