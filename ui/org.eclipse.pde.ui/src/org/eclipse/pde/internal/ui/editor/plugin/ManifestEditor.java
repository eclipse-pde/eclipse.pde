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
package org.eclipse.pde.internal.ui.editor.plugin;
import java.io.*;
import java.util.zip.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.build.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.properties.*;

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
		if (name.equals("manifest.mf")) { //$NON-NLS-1$
			manifestFile = file;
			buildFile = project.getFile("build.properties"); //$NON-NLS-1$
			pluginFile = createPluginFile(project);
		} else if (name.equals("build.properties")) { //$NON-NLS-1$
			buildFile = file;
			pluginFile = createPluginFile(project);
			manifestFile = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		} else if (name.equals("plugin.xml") || name.equals("fragment.xml")) { //$NON-NLS-1$ //$NON-NLS-2$
			pluginFile = file;
			fragment = name.equals("fragment.xml"); //$NON-NLS-1$
			buildFile = project.getFile("build.properties"); //$NON-NLS-1$
			manifestFile = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
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
		PluginInputContextManager manager =  new PluginInputContextManager(this);
		manager.setUndoManager(new PluginUndoManager(this));
		return manager;
	}
	
	public void monitoredFileAdded(IFile file) {
		String name = file.getName();
		if (name.equalsIgnoreCase("MANIFEST.MF")) { //$NON-NLS-1$
			if (!inputContextManager.hasContext(BundleInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				inputContextManager.putContext(in, new BundleInputContext(this, in, false));
			}
		}
		else if (name.equalsIgnoreCase("plugin.xml")) { //$NON-NLS-1$
			if (!inputContextManager.hasContext(PluginInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				inputContextManager.putContext(in, new PluginInputContext(this, in, false, false));
			}
		}
		else if (name.equalsIgnoreCase("fragment.xml")) { //$NON-NLS-1$
			if (!inputContextManager.hasContext(PluginInputContext.CONTEXT_ID)) {			
				IEditorInput in = new FileEditorInput(file);
				inputContextManager.putContext(in, new PluginInputContext(this, in, false, true));
			}
		}
		else if (name.equalsIgnoreCase("build.properties")) { //$NON-NLS-1$
			if (!inputContextManager.hasContext(BundleInputContext.CONTEXT_ID)) {			
				IEditorInput in = new FileEditorInput(file);
				inputContextManager.putContext(in, new BuildInputContext(this, in, false));
			}
		}
	}
	
	public void ensurePluginContextPresence() {
		if (inputContextManager.hasContext(PluginInputContext.CONTEXT_ID))
			return;
		IProject project = inputContextManager.getCommonProject();
		IFile file = project.getFile("plugin.xml"); //$NON-NLS-1$
		WorkspacePluginModel model = new WorkspacePluginModel(file);
		IPluginBase pluginBase = model.getPluginBase(true);
		try {
			pluginBase.setSchemaVersion("3.0"); //$NON-NLS-1$
		}
		catch (CoreException e) {
		}
		model.save();
		IEditorInput in = new FileEditorInput(file);
		inputContextManager.putContext(in, new PluginInputContext(this, in, false, false));
	}

	public boolean monitoredFileRemoved(IFile file) {
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		return true;
	}
	public void contextAdded(InputContext context) {
		addSourcePage(context.getId());
		try {
			if (context.getId().equals(BuildPage.PAGE_ID))
				addPage(new BuildPage(this));
		}
		catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
	public void contextRemoved(InputContext context) {
		if (context.isPrimary()) {
			close(true);
			return;
		}
		IFormPage page = findPage(context.getId());
		if (page!=null) {
			removePage(context.getId());
			if (context.getId().equals(BuildInputContext.CONTEXT_ID))
				removePage(BuildPage.PAGE_ID);
		}
	}

	protected void createSystemFileContexts(InputContextManager manager,
			SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		File manifestFile = null;
		File buildFile = null;
		File pluginFile = null;
		String name = file.getName().toLowerCase();
		if (name.equals("manifest.mf")) { //$NON-NLS-1$
			manifestFile = file;
			File dir = file.getParentFile().getParentFile();
			buildFile = new File(dir, "build.properties"); //$NON-NLS-1$
			pluginFile = createPluginFile(dir);
		} else if (name.equals("build.properties")) { //$NON-NLS-1$
			buildFile = file;
			File dir = file.getParentFile();
			pluginFile = createPluginFile(dir);
			manifestFile = new File(dir, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
		} else if (name.equals("plugin.xml") || name.equals("fragment.xml")) { //$NON-NLS-1$ //$NON-NLS-2$
			pluginFile = file;
			File dir = file.getParentFile();
			buildFile = new File(dir, "build.properties"); //$NON-NLS-1$
			manifestFile = new File(dir, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
		}
		if (manifestFile.exists()) {
			IEditorInput in = new SystemFileEditorInput(manifestFile);
			manager.putContext(in, new BundleInputContext(this, in,
					file == manifestFile));
		}
		if (pluginFile.exists()) {
			SystemFileEditorInput in = new SystemFileEditorInput(pluginFile);
			manager.putContext(in, new PluginInputContext(this, in,
					file == pluginFile, name.equals("fragment.xml"))); //$NON-NLS-1$
		}
		if (buildFile.exists()) {
			SystemFileEditorInput in = new SystemFileEditorInput(buildFile);
			manager.putContext(in, new BuildInputContext(this, in,
					file == buildFile));
		}
	}
	private File createPluginFile(File dir) {
		File pluginFile = new File(dir, "plugin.xml"); //$NON-NLS-1$
		if (!pluginFile.exists())
			pluginFile = new File(dir, "fragment.xml"); //$NON-NLS-1$
		return pluginFile;
	}
	
	private IFile createPluginFile(IProject project) {
		IFile pluginFile = project.getFile("plugin.xml"); //$NON-NLS-1$
		if (!pluginFile.exists())
			pluginFile = project.getFile("fragment.xml"); //$NON-NLS-1$
		return pluginFile;
	}
	
	protected void createStorageContexts(InputContextManager manager,
			IStorageEditorInput input) {
		if (input instanceof JarEntryEditorInput) {
			createJarEntryContexts(manager, (JarEntryEditorInput)input);
			return;
		}
		
		String name = input.getName().toLowerCase();
		if (name.startsWith("manifest.mf")) { //$NON-NLS-1$
			manager
					.putContext(input,
							new BundleInputContext(this, input, true));
		} else if (name.startsWith("build.properties")) { //$NON-NLS-1$
			manager.putContext(input, new BuildInputContext(this, input, true));
		} else if (name.startsWith("plugin.xml")) { //$NON-NLS-1$
			manager.putContext(input, new PluginInputContext(this, input, true,
					false));
		} else if (name.startsWith("fragment.xml")) { //$NON-NLS-1$
			manager.putContext(input, new PluginInputContext(this, input, true,
					true));
		}
	}
	
	protected void createJarEntryContexts(InputContextManager manager,
			JarEntryEditorInput input) {
		IStorage storage = input.getStorage();
		ZipFile zip = (ZipFile)storage.getAdapter(ZipFile.class);
		if (zip == null)
			return;
		
		if (zip.getEntry("META-INF/MANIFEST.MF") != null) { //$NON-NLS-1$
			input = new JarEntryEditorInput(new JarEntryFile(zip, "META-INF/MANIFEST.MF")); //$NON-NLS-1$
			manager.putContext(input, new BundleInputContext(this, input, storage.getName().equals("MANIFEST.MF"))); //$NON-NLS-1$
		}
		
		if (zip.getEntry("plugin.xml") != null) { //$NON-NLS-1$
			input = new JarEntryEditorInput(new JarEntryFile(zip, "plugin.xml")); //$NON-NLS-1$
			manager.putContext(input, new PluginInputContext(this, input, storage.getName().equals("plugin.xml"), false)); //$NON-NLS-1$
		} else if (zip.getEntry("fragment.xml") != null) { //$NON-NLS-1$
			input = new JarEntryEditorInput(new JarEntryFile(zip, "fragment.xml")); //$NON-NLS-1$
			manager.putContext(input, new PluginInputContext(this, input, storage.getName().equals("fragment.xml"), true)); //$NON-NLS-1$
		}
		
		if (zip.getEntry("build.properties") != null) { //$NON-NLS-1$
			input = new JarEntryEditorInput(new JarEntryFile(zip, "build.properties")); //$NON-NLS-1$
			manager.putContext(input, new BuildInputContext(this, input, storage.getName().equals("build.properties"))); //$NON-NLS-1$
		}
	}
	
	public boolean canCopy(ISelection selection) {
		return true;
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
	
	protected ISortableContentOutlinePage createContentOutline() {
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

	public static IEditorPart openPluginEditor(IPluginBase plugin) {
		return openPluginEditor(plugin, null);
	}
	
	public static IEditorPart openPluginEditor(
		IPluginBase plugin,
		Object object) {
		IEditorPart editor = null;
		IResource underlyingResource = plugin.getModel().getUnderlyingResource();
		if (underlyingResource == null) {
			editor = openExternalPlugin(plugin);
		} else {
			editor = openWorkspacePlugin((IFile) underlyingResource, plugin instanceof IFragment);
		}
		return editor;
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
		String manifest = null;
		
		IStorageEditorInput input = null;
		File pluginLocation = new File(pluginInfo.getModel().getInstallLocation());
		if (pluginLocation.isFile() && pluginLocation.getName().endsWith(".jar")) { //$NON-NLS-1$
			try {
				ZipFile zipFile = new ZipFile(pluginLocation);
				if (zipFile.getEntry("META-INF/MANIFEST.MF") != null) //$NON-NLS-1$
					manifest = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
				else 
					manifest = isFragment ? "fragment.xml" : "plugin.xml"; //$NON-NLS-1$ //$NON-NLS-2$
				input = new JarEntryEditorInput(new JarEntryFile(zipFile, manifest));
			} catch (Exception e) {
			}			
		}
		
		if (input == null) {
			File file = new File(pluginLocation, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
			if (!file.exists())
				file = new File(pluginLocation, isFragment ? "fragment.xml" : "plugin.xml"); //$NON-NLS-1$ //$NON-NLS-2$
			if (file.exists())
				input = new SystemFileEditorInput(file);
		}
		try {
			if (input != null)
				return (ManifestEditor) PDEPlugin.getActivePage().openEditor(
					input,
					IPDEUIConstants.MANIFEST_EDITOR_ID);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		return null;
	}

	public String getTitle() {
		IPluginModelBase model = (IPluginModelBase)getAggregateModel();
		if (model==null || !model.isValid())
			return super.getTitle();
		String text = getTitleText(model.getPluginBase());
		if (text == null)
			return super.getTitle();
		return model.getResourceString(text);
	}
	
	public String getTitleProperty() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		String pref = store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
		if (pref!=null && pref.equals(IPreferenceConstants.VALUE_USE_NAMES))
			return IPluginBase.P_NAME;
		return IPluginBase.P_ID;
	}
	
	private String getTitleText(IPluginBase pluginBase) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		String pref = store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
		if (pref!=null && pref.equals(IPreferenceConstants.VALUE_USE_NAMES))
			return pluginBase.getName();
		return pluginBase.getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof IBuildObject) {
			context = inputContextManager.findContext(BuildInputContext.CONTEXT_ID);
		} else if (object instanceof IPluginExtensionPoint || object instanceof IPluginExtension) {
			context = inputContextManager.findContext(PluginInputContext.CONTEXT_ID);
		} else {
			context = inputContextManager.findContext(BundleInputContext.CONTEXT_ID);
			if (context == null)
				context = inputContextManager.findContext(PluginInputContext.CONTEXT_ID);
		}		
		return context;
	}

}
