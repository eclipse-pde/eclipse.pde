package org.eclipse.pde.internal.builders;

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


public class ManifestConsistencyChecker extends IncrementalProjectBuilder {
	public static final String BUILDERS_VERIFYING = "Builders.verifying";
	public static final String BUILDERS_FRAGMENT_BROKEN_LINK = "Builders.Fragment.brokenLink";
	public static final String BUILDERS_UPDATING = "Builders.updating";

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
		delta.accept(new DeltaVisitor(monitor));
	}
	return null;
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
		}
	}
	monitor.subTask(
		PDEPlugin.getResourceString(BUILDERS_UPDATING));
	reporter.reportErrors();
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
private void reportValidationError(Node errorNode, PluginErrorReporter reporter) {
	int type = errorNode.getNodeType();
}
protected void startupOnInitialize() {
	super.startupOnInitialize();
}
private void validateFragment(IFile file, PluginErrorReporter reporter) {
	WorkspaceFragmentModel model = new WorkspaceFragmentModel(file);
	model.load();
	if (model.isLoaded()) {
		// Test if plugin exists
		IFragment fragment = model.getFragment();
		String pluginId = fragment.getPluginId();
		String pluginVersion = fragment.getPluginVersion();
		IPlugin plugin = PDEPlugin.getDefault().findPlugin(pluginId, pluginVersion);
		if (plugin==null) {
			// broken fragment link
			String [] args = { pluginId, pluginVersion };
			String message = PDEPlugin.getFormattedMessage(BUILDERS_FRAGMENT_BROKEN_LINK, args);
			reporter.reportError(message);
		}
	}
	model.release();
}
}
