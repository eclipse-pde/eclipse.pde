package org.eclipse.pde.internal.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.model.*;
import org.apache.xerces.parsers.*;
import org.eclipse.core.runtime.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.ISourceObject;

public class ManifestConsistencyChecker extends IncrementalProjectBuilder {
	public static final String BUILDERS_VERIFYING = "Builders.verifying";
	public static final String BUILDERS_FRAGMENT_BROKEN_LINK =
		"Builders.Fragment.brokenLink";
	public static final String BUILDERS_UPDATING = "Builders.updating";
	public static final String BUILDERS_VERSION_FORMAT = "Builders.versionFormat";

	class DeltaVisitor implements IResourceDeltaVisitor {
		private IProgressMonitor monitor;
		public DeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();

			if (resource instanceof IProject) {
				// Only check projects with plugin nature
				IProject project = (IProject) resource;
				try {
					return (project.hasNature(PDEPlugin.PLUGIN_NATURE));
				} catch (CoreException e) {
					PDEPlugin.logException(e);
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
			return false;
		}
	}

	public ManifestConsistencyChecker() {
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
			IPath path = project.getFullPath().append("plugin.xml");
			IWorkspace workspace = project.getWorkspace();
			IFile file = workspace.getRoot().getFile(path);
			if (file.exists()) {
				checkFile(file, monitor);
			} else {
				path = project.getFullPath().append("fragment.xml");
				file = workspace.getRoot().getFile(path);
				if (file.exists()) {
					checkFile(file, monitor);
				}
			}
		} else {
			processDelta(delta, monitor);
		}
		return null;
	}

	private void processDelta(IResourceDelta delta, IProgressMonitor monitor)
		throws CoreException {
		delta.accept(new DeltaVisitor(monitor));
	}

	private void checkFile(IFile file, IProgressMonitor monitor) {
		String message =
			PDEPlugin.getFormattedMessage(
				BUILDERS_VERIFYING,
				file.getFullPath().toString());
		monitor.subTask(message);
		PluginErrorReporter reporter = new PluginErrorReporter(file);
		ManifestParser parser = new ManifestParser(reporter);
		parser.parse(file);
		if (reporter.getErrorCount() == 0) {
			if (isFragment(file)) {
				validateFragment(file, reporter);
			} else {
				validatePlugin(file, reporter);
			}
		}
		monitor.subTask(PDEPlugin.getResourceString(BUILDERS_UPDATING));
		monitor.done();
	}
	private boolean isFragment(IFile file) {
		String name = file.getName().toLowerCase();
		return name.equals("fragment.xml");
	}
	private boolean isManifestFile(IFile file) {
		String name = file.getName().toLowerCase();
		return name.equals("plugin.xml") || name.equals("fragment.xml");
	}
	private void reportValidationError(
		Node errorNode,
		PluginErrorReporter reporter) {
		int type = errorNode.getNodeType();
	}
	protected void startupOnInitialize() {
		super.startupOnInitialize();
	}

	private void validatePlugin(IFile file, PluginErrorReporter reporter) {
		WorkspacePluginModel model = new WorkspacePluginModel(file);
		model.load();
		if (model.isLoaded()) {
			// Test the version
			IPlugin plugin = model.getPlugin();
			validateVersion(plugin, reporter);
		}
		model.release();
	}

	private void validateFragment(IFile file, PluginErrorReporter reporter) {
		WorkspaceFragmentModel model = new WorkspaceFragmentModel(file);
		model.load();
		if (model.isLoaded()) {
			// Test the version
			// Test if plugin exists
			IFragment fragment = model.getFragment();
			validateVersion(fragment, reporter);
			String pluginId = fragment.getPluginId();
			String pluginVersion = fragment.getPluginVersion();
			int match = fragment.getRule();
			IPlugin plugin = PDEPlugin.getDefault().findPlugin(pluginId, pluginVersion, match);
			if (plugin == null) {
				// broken fragment link
				String[] args = { pluginId, pluginVersion };
				String message =
					PDEPlugin.getFormattedMessage(BUILDERS_FRAGMENT_BROKEN_LINK, args);
				int line = 1;
				if (fragment instanceof ISourceObject)
					line = ((ISourceObject) fragment).getStartLine();
				reporter.reportError(message, line);
			}
		}
		model.release();
	}

	private void validateVersion(
		IPluginBase pluginBase,
		PluginErrorReporter reporter) {
		String version = pluginBase.getVersion();
		if (version == null)
			version = "";
		try {
			PluginVersionIdentifier pvi = new PluginVersionIdentifier(version);
			pvi.toString();
		} catch (Throwable e) {
			String message =
				PDEPlugin.getFormattedMessage(BUILDERS_VERSION_FORMAT, version);
			int line = 1;
			if (pluginBase instanceof ISourceObject)
				line = ((ISourceObject) pluginBase).getStartLine();
			reporter.reportError(message, line);
		}
	}
}