package org.eclipse.pde.internal.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.model.component.*;
import org.apache.xerces.parsers.*;
import org.eclipse.core.runtime.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;


public class ComponentConsistencyChecker extends IncrementalProjectBuilder {
	public static final String BUILDERS_VERIFYING = "Builders.verifying";
	public static final String BUILDERS_COMPONENT_REFERENCE = "Builders.Component.reference";
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
					return (project.hasNature(PDEPlugin.COMPONENT_NATURE));
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
			return true;
		}
	}

public ComponentConsistencyChecker() {
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
		IPath path = project.getFullPath().append("install.xml");
		IWorkspace workspace = project.getWorkspace();
		IFile file = workspace.getRoot().getFile(path);
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
		PDEPlugin.getFormattedMessage(
			BUILDERS_VERIFYING,
			file.getFullPath().toString());
	monitor.subTask(message);
	PluginErrorReporter reporter = new PluginErrorReporter(file);
	SAXParser parser = new SAXParser();
	parser.setErrorHandler(reporter);
	InputStream source = null;
	try {
		source = file.getContents();
		InputSource inputSource = new InputSource(source);
		parser.parse(inputSource);
		if (reporter.getErrorCount() == 0) {
			testPluginReferences(file, reporter);
		}
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	} catch (SAXException e) {
	} catch (IOException e) {
		PDEPlugin.logException(e);
	} finally {
		if (source != null) {
			try {
				source.close();
			} catch (IOException e) {
			}
		}
	}
	monitor.subTask(PDEPlugin.getResourceString(BUILDERS_UPDATING));
	reporter.reportErrors();
	monitor.done();
}
private boolean isManifestFile(IFile file) {
   return file.getName().toLowerCase().equals("install.xml");
}
private boolean isValidReference(IComponentPlugin plugin) {
	WorkspaceModelManager manager =
		PDEPlugin.getDefault().getWorkspaceModelManager();
	IPluginModel[] models = manager.getWorkspacePluginModels();
	for (int i = 0; i < models.length; i++) {
		IPluginModel model = models[i];
		if (model.getPlugin().getId().equals(plugin.getId())) {
			return true;
		}
	}
	return false;
}
protected void startupOnInitialize() {
	super.startupOnInitialize();
}
private void testPluginReferences(IFile file, PluginErrorReporter reporter) {
	WorkspaceComponentModel model = new WorkspaceComponentModel(file);
	model.load();
	if (model.isLoaded()) {
		IComponentPlugin[] plugins = model.getComponent().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IComponentPlugin plugin = plugins[i];
			if (isValidReference(plugin) == false) {
				String message =
					PDEPlugin.getFormattedMessage(
						BUILDERS_COMPONENT_REFERENCE,
						plugin.getLabel());
				reporter.reportError(message);
			}
		}
	}
}
}
