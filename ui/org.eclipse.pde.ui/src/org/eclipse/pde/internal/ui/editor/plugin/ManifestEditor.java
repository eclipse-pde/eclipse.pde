/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.build.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.ui.views.properties.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ManifestEditor extends MultiSourceEditor {
	protected void createResourceContexts(InputContextManager manager,
			IFileEditorInput input) {
		IFile file = input.getFile();
		IProject project = file.getProject();
		IFile manifestFile = null;
		IFile buildFile = null;
		IFile pluginFile = null;
		boolean fragment = false;
		String name = file.getName().toLowerCase();
		if (name.equals("manifest.mf")) {
			manifestFile = file;
			buildFile = project.getFile("build.properties");
			pluginFile = createPluginFile(project);
		} else if (name.equals("build.properties")) {
			buildFile = file;
			pluginFile = createPluginFile(project);
			manifestFile = project.getFile("META-INF/MANIFEST.MF");
		} else if (name.equals("plugin.xml") || name.equals("fragment.xml")) {
			pluginFile = file;
			fragment = name.equals("fragment.xml");
			buildFile = project.getFile("build.properties");
			manifestFile = project.getFile("META-INF/MANIFEST.MF");
		}
		if (manifestFile.exists()) {
			IEditorInput in = new FileEditorInput(manifestFile);
			manager.putContext(in, new BundleInputContext(this, in,
					file == manifestFile));
		}
		if (pluginFile.exists()) {
			FileEditorInput in = new FileEditorInput(pluginFile);
			manager.putContext(in, new PluginInputContext(this, in,
					file == pluginFile, fragment));
		}
		if (buildFile.exists()) {
			FileEditorInput in = new FileEditorInput(buildFile);
			manager.putContext(in, new BuildInputContext(this, in,
					file == buildFile));
		}
		manager.monitorFile(manifestFile);
		manager.monitorFile(pluginFile);
		manager.monitorFile(buildFile);
	}
	
	protected InputContextManager createInputContextManager() {
		PluginInputContextManager manager =  new PluginInputContextManager();
		manager.setUndoManager(new PluginUndoManager(this));
		return manager;
	}
	
	public void monitoredFileAdded(IFile file) {
		String name = file.getName();
		if (name.equalsIgnoreCase("MANIFEST.MF")) {
			IEditorInput in = new FileEditorInput(file);
			inputContextManager.putContext(in, new BundleInputContext(this, in, false));
		}
		else if (name.equalsIgnoreCase("plugin.xml")) {
			IEditorInput in = new FileEditorInput(file);
			inputContextManager.putContext(in, new PluginInputContext(this, in, false, false));						
		}
		else if (name.equalsIgnoreCase("fragment.xml")) {
			IEditorInput in = new FileEditorInput(file);
			inputContextManager.putContext(in, new PluginInputContext(this, in, false, true));
		}
		else if (name.equalsIgnoreCase("build.properties")) {
			IEditorInput in = new FileEditorInput(file);
			inputContextManager.putContext(in, new BuildInputContext(this, in, false));			
		}
	}

	public boolean monitoredFileRemoved(IFile file) {
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		return true;
	}
	public void contextAdded(InputContext context) {
		addSourcePage(context.getId());
	}
	public void contextRemoved(InputContext context) {
		IFormPage page = findPage(context.getId());
		if (page!=null)
			removePage(context.getId());
	}

	protected void createSystemFileContexts(InputContextManager manager,
			SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		File manifestFile = null;
		File buildFile = null;
		File pluginFile = null;
		String name = file.getName().toLowerCase();
		if (name.equals("manifest.mf")) {
			manifestFile = file;
			File dir = file.getParentFile().getParentFile();
			buildFile = new File(dir, "build.properties");
			pluginFile = createPluginFile(dir);
		} else if (name.equals("build.properties")) {
			buildFile = file;
			File dir = file.getParentFile();
			pluginFile = createPluginFile(dir);
			manifestFile = new File(dir, "META-INF/MANIFEST.MF");
		} else if (name.equals("plugin.xml") || name.equals("fragment.xml")) {
			pluginFile = file;
			File dir = file.getParentFile();
			buildFile = new File(dir, "build.properties");
			manifestFile = new File(dir, "META-INF/MANIFEST.MF");
		}
		if (manifestFile.exists()) {
			IEditorInput in = new SystemFileEditorInput(manifestFile);
			manager.putContext(in, new BundleInputContext(this, in,
					file == manifestFile));
		}
		if (pluginFile.exists()) {
			SystemFileEditorInput in = new SystemFileEditorInput(pluginFile);
			manager.putContext(in, new PluginInputContext(this, in,
					file == pluginFile, name.equals("fragment.xml")));
		}
		if (buildFile.exists()) {
			SystemFileEditorInput in = new SystemFileEditorInput(buildFile);
			manager.putContext(in, new BuildInputContext(this, in,
					file == buildFile));
		}
	}
	private File createPluginFile(File dir) {
		File pluginFile = new File(dir, "plugin.xml");
		if (!pluginFile.exists())
			pluginFile = new File(dir, "fragment.xml");
		return pluginFile;
	}
	private IFile createPluginFile(IProject project) {
		IFile pluginFile = project.getFile("plugin.xml");
		if (!pluginFile.exists())
			pluginFile = project.getFile("fragment.xml");
		return pluginFile;
	}
	protected void createStorageContexts(InputContextManager manager,
			IStorageEditorInput input) {
		String name = input.getName().toLowerCase();
		if (name.startsWith("manifest.mf")) {
			manager
					.putContext(input,
							new BundleInputContext(this, input, true));
		} else if (name.startsWith("build.properties")) {
			manager.putContext(input, new BuildInputContext(this, input, true));
		} else if (name.startsWith("plugin.xml")) {
			manager.putContext(input, new PluginInputContext(this, input, true,
					false));
		} else if (name.startsWith("fragment.xml")) {
			manager.putContext(input, new PluginInputContext(this, input, true,
					true));
		}
	}
	protected void contextMenuAboutToShow(IMenuManager manager) {
		super.contextMenuAboutToShow(manager);
	}

	protected void addPages() {
		try {
			addPage(new OverviewPage(this));
			addPage(new DependenciesPage(this));
			addPage(new RuntimePage(this));
			addPage(new ExtensionsPage(this));
			addPage(new ExtensionPointsPage(this));
			if (inputContextManager.hasContext(BuildInputContext.CONTEXT_ID))
				addPage(new BuildPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(BundleInputContext.CONTEXT_ID);
		addSourcePage(PluginInputContext.CONTEXT_ID);
		addSourcePage(BuildInputContext.CONTEXT_ID);
	}


	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			InputContext primary = inputContextManager.getPrimaryContext();
			boolean isBundle = inputContextManager
					.hasContext(BundleInputContext.CONTEXT_ID);
			if (primary.getId().equals(BuildInputContext.CONTEXT_ID))
				firstPageId = BuildPage.PAGE_ID;
			else if (primary.getId().equals(PluginInputContext.CONTEXT_ID)) {
				if (isBundle)
					firstPageId = ExtensionsPage.PAGE_ID;
				else
					firstPageId = OverviewPage.PAGE_ID;
			}
			if (firstPageId == null)
				firstPageId = OverviewPage.PAGE_ID;
		}
		return firstPageId;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor, java.lang.String, java.lang.String)
	 */
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		if (contextId.equals(PluginInputContext.CONTEXT_ID))
			return new ManifestSourcePage(editor, title, name);
		if (contextId.equals(BuildInputContext.CONTEXT_ID))
			return new BuildSourcePage(editor, title, name);
		if (contextId.equals(BundleInputContext.CONTEXT_ID))
			return new BundleSourcePage(editor, title, name);
		return super.createSourcePage(editor, title, name, contextId);
	}
	
	protected IContentOutlinePage createContentOutline() {
		return new ManifestOutlinePage(this);
	}
	public Object getAdapter(Class key) {
		//No property sheet needed - block super
		if (key.equals(IPropertySheetPage.class)) {
			return null;
		}
		return super.getAdapter(key);
	}
	public static void openPluginEditor(String pluginId) {
		openPluginEditor(pluginId, null);
	}

	public static void openPluginEditor(
		String pluginId,
		Object object) {
		IPlugin pluginToOpen = PDECore.getDefault().findPlugin(pluginId);
		if (pluginToOpen != null) {
			openPluginEditor(pluginToOpen, object);
		} else {
			Display.getCurrent().beep();
		}
	}

	public static void openPluginEditor(IPluginBase plugin) {
		openPluginEditor(plugin, null);
	}
	
	public static void openPluginEditor(
		IPluginBase plugin,
		Object object) {
		openPluginEditor(plugin, object, null);
	}

	public static void openPluginEditor(
		IPluginBase plugin,
		Object object,
		IMarker marker) {
		IEditorPart editor = null;
		ISharedPluginModel model = plugin.getModel();
		IResource underlyingResource = null;
		if (model instanceof IBundlePluginModelBase) {
			underlyingResource = ((IBundlePluginModelBase)model).getExtensionsModel().getUnderlyingResource();
		} else {
			underlyingResource = plugin.getModel().getUnderlyingResource();
		}
		if (underlyingResource == null) {
			editor = openExternalPlugin(plugin);
		} else {
			editor = openWorkspacePlugin((IFile) underlyingResource, plugin instanceof IFragment);
		}
		if (editor instanceof ManifestEditor && editor != null && object != null ) {
			((ManifestEditor)editor).openTo(object, marker);
		}
	}

	private static IEditorPart openWorkspacePlugin(IFile pluginFile, boolean fragment) {
		String editorId = PDEPlugin.MANIFEST_EDITOR_ID;
		try {
			FileEditorInput input = new FileEditorInput(pluginFile);
			return PDEPlugin.getActivePage().openEditor(
				input,
				editorId);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		return null;
	}
	private static ManifestEditor openExternalPlugin(IPluginBase pluginInfo) {
		boolean isFragment = pluginInfo.getPluginModel().isFragmentModel();
		String manifest =
			isFragment
				? "fragment.xml"
				: "plugin.xml";
		String fileName =
			pluginInfo.getModel().getInstallLocation()
				+ File.separator
				+ manifest;
		File file = new File(fileName);
		if (file.exists()) {
			try {
				SystemFileEditorInput input = new SystemFileEditorInput(file);
				return (ManifestEditor) PDEPlugin.getActivePage().openEditor(
					input,
					IPDEUIConstants.MANIFEST_EDITOR_ID);
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
			}
		}
		return null;
	}
}