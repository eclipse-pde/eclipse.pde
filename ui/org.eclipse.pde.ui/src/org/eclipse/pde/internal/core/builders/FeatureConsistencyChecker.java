package org.eclipse.pde.internal.core.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.ifeature.*;
import org.eclipse.pde.internal.core.feature.*;
import org.apache.xerces.parsers.*;
import org.eclipse.core.runtime.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.ui.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;


public class FeatureConsistencyChecker extends IncrementalProjectBuilder {
	public static final String BUILDERS_VERIFYING = "Builders.verifying";
	public static final String BUILDERS_FEATURE_REFERENCE = "Builders.Component.reference";
	public static final String BUILDERS_UPDATING = "Builders.updating";
	public static final String BUILDERS_FEATURE_FOLDER_SYNC = "Builders.Component.folderSync";

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
					return (project.hasNature(PDEPlugin.FEATURE_NATURE));
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

public FeatureConsistencyChecker() {
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
	monitor.done();
}
private boolean isManifestFile(IFile file) {
   return file.getName().toLowerCase().equals("feature.xml");
}
private boolean isValidReference(IFeaturePlugin plugin) {
	WorkspaceModelManager manager =
		PDEPlugin.getDefault().getWorkspaceModelManager();
	IPluginModelBase [] models = plugin.isFragment() ? 
		(IPluginModelBase[])manager.getWorkspaceFragmentModels() :
		(IPluginModelBase[])manager.getWorkspacePluginModels();
	for (int i = 0; i < models.length; i++) {
		IPluginModelBase model = models[i];
		if (model.getPluginBase().getId().equals(plugin.getId())) {
			return true;
		}
	}
	return false;
}
protected void startupOnInitialize() {
	super.startupOnInitialize();
}

private void testPluginReferences(IFile file, PluginErrorReporter reporter) {
	WorkspaceFeatureModel model = new WorkspaceFeatureModel(file);
	model.load();
	if (model.isLoaded()) {
		IFeature feature = model.getFeature();
		IFeaturePlugin[] plugins = feature.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IFeaturePlugin plugin = plugins[i];
			if (isValidReference(plugin) == false) {
				String message =
					PDEPlugin.getFormattedMessage(
						BUILDERS_FEATURE_REFERENCE,
						plugin.getLabel());
				reporter.reportError(message);
			}
		}
		String version = feature.getVersion();
		String id = feature.getId();
		String expectedFolderName = id+"_"+version;
		IFolder folder = (IFolder)file.getParent();
		String realName = folder.getName();
		if (realName.equals(expectedFolderName)==false) {
			String message =
				PDEPlugin.getFormattedMessage(
					BUILDERS_FEATURE_FOLDER_SYNC,
					realName);
			reporter.reportError(message);
		}
	}
}
}
