/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.IBuildObject;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelProvider;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.JarEntryFile;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildPage;
import org.eclipse.pde.internal.ui.editor.build.BuildSourcePage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.search.javaparticipant.SearchHit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.osgi.service.prefs.BackingStoreException;

public class ManifestEditor extends MultiSourceEditor implements IShowEditorInput {
    
    private static int BUILD_INDEX = 5;
	private static boolean SHOW_SOURCE;
    private boolean fEquinox = true;
    private boolean fShowExtensions = true;
    private IEclipsePreferences fPrefs;
    
	public static IEditorPart openPluginEditor(String id) {
		return openPluginEditor(PDECore.getDefault().getModelManager().findModel(id));
	}
	
	public static IEditorPart openPluginEditor(IPluginModelBase model) {
		return openPluginEditor(model, false);
	}
	
	public static IEditorPart openPluginEditor(IPluginModelBase model, boolean source) {
		return open(model.getPluginBase(), source);
	}
	
	public static IEditorPart open(Object object, boolean source) {
		SHOW_SOURCE = source;
		if (object instanceof IPluginObject) {
			ISharedPluginModel model = ((IPluginObject)object).getModel();
			if (model instanceof IBundlePluginModelProvider) 
				model = ((IBundlePluginModelProvider)model).getBundlePluginModel();
			if (model instanceof IPluginModelBase) {
				String filename = ((IPluginModelBase)model).isFragmentModel() ? "fragment.xml" : "plugin.xml"; //$NON-NLS-1$ //$NON-NLS-2$
				if (!(object instanceof IPluginExtension) && !(object instanceof IPluginExtensionPoint)) {
					File file = new File(model.getInstallLocation());
					if (file.isFile()) {
						if (CoreUtility.jarContainsResource(file, "META-INF/MANIFEST.MF", false)) { //$NON-NLS-1$
							filename = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
						} 
					} else if (new File(file, "META-INF/MANIFEST.MF").exists()) { //$NON-NLS-1$
						filename = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
					}
				}
				IResource resource = model.getUnderlyingResource();
				if (resource == null) 
					return openExternalPlugin(new File(model.getInstallLocation()), filename);
				return openWorkspacePlugin(resource.getProject().getFile(filename));
			}
		}
		return null;
	}
		
	
	private static IEditorPart openWorkspacePlugin(IFile pluginFile) {
		return openEditor(new FileEditorInput(pluginFile));
	}
	
	private static IEditorPart openExternalPlugin(File location, String filename) {
		IEditorInput input = null;
		if (location.isFile()) {
			try {
				ZipFile zipFile = new ZipFile(location);
				if (zipFile.getEntry(filename) != null) 
					input = new JarEntryEditorInput(new JarEntryFile(zipFile, filename));
			} catch (IOException e) {
			}			
		} else {
			File file = new File(location, filename);
			if (file.exists())
				input = new SystemFileEditorInput(file);
		}
		return openEditor(input);
	}
	
	public static IEditorPart openEditor(IEditorInput input) {
		if (input != null) {
			try {
				return PDEPlugin.getActivePage().openEditor(
						input,
						IPDEUIConstants.MANIFEST_EDITOR_ID);
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
			}
		}
		return null;		
	}

	protected void createResourceContexts(InputContextManager manager,
			IFileEditorInput input) {
		IFile file = input.getFile();
		IProject project = file.getProject();
		IFile manifestFile = null;
		IFile buildFile = null;
		IFile pluginFile = null;
		boolean fragment = false;
		String name = file.getName().toLowerCase(Locale.ENGLISH);
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
		manager.monitorFile(project.getFile("plugin.xml")); //$NON-NLS-1$
		manager.monitorFile(project.getFile("fragment.xml")); //$NON-NLS-1$
		manager.monitorFile(buildFile);
		
		fPrefs = new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
		if (fPrefs != null) {
			fShowExtensions = fPrefs.getBoolean(ICoreConstants.EXTENSIONS_PROPERTY, true);
			fEquinox = fPrefs.getBoolean(ICoreConstants.EQUINOX_PROPERTY, true);
		}
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
			if (!inputContextManager.hasContext(BuildInputContext.CONTEXT_ID)) {			
				IEditorInput in = new FileEditorInput(file);
				inputContextManager.putContext(in, new BuildInputContext(this, in, false));
			}
		}
	}
	
	public void ensurePluginContextPresence() {
		if (inputContextManager.hasContext(PluginInputContext.CONTEXT_ID))
			return;
		IProject project = inputContextManager.getCommonProject();
		String name = (inputContextManager.getAggregateModel() instanceof IFragmentModel)
						? "fragment.xml" : "plugin.xml"; //$NON-NLS-1$ //$NON-NLS-2$
		IFile file = project.getFile(name); 
		WorkspacePluginModelBase model;
		if (name.equals("fragment.xml"))  //$NON-NLS-1$
			model = new WorkspaceFragmentModel(file, false);
		else
			model = new WorkspacePluginModel(file, false);
		
		IPluginBase pluginBase = model.getPluginBase(true);
		try {
			pluginBase.setSchemaVersion("3.0"); //$NON-NLS-1$
		}
		catch (CoreException e) {
		}
		model.save();
		IEditorInput in = new FileEditorInput(file);
		inputContextManager.putContext(in, new PluginInputContext(this, in, false, false));

		updateBuildProperties(name);
	}
	
    private void updateBuildProperties(String filename) {
        try {
         InputContext context = inputContextManager.findContext(BuildInputContext.CONTEXT_ID);
         if (context != null) {
                IBuildModel buildModel = (IBuildModel)context.getModel();
                IBuild build = buildModel.getBuild();
                IBuildEntry entry = build.getEntry("bin.includes"); //$NON-NLS-1$
                if (entry == null) {
                    entry = buildModel.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
                    build.add(entry);
                } 
                if (!entry.contains(filename))
                    entry.addToken(filename); 
            }
         } catch (CoreException e) {
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
		try {
			if (context.getId().equals(BuildInputContext.CONTEXT_ID))
				addPage(BUILD_INDEX, new BuildPage(this));
            else {
				updateFirstThreePages();
            }
		}
		catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
	public void contextRemoved(InputContext context) {		
		close(true);
	}
	
	private void updateFirstThreePages() {
		try {
			int index = getActivePage();
			removePage(0);
			removePage(0);
			removePage(0);
			addPage(0, new RuntimePage(this));
			addPage(0, new DependenciesPage(this));
			addPage(0, new OverviewPage(this));
			setActivePage(index);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}		
	}

	protected void createSystemFileContexts(InputContextManager manager,
			SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		File manifestFile = null;
		File buildFile = null;
		File pluginFile = null;
		String name = file.getName().toLowerCase(Locale.ENGLISH);
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
		
		String name = input.getName().toLowerCase(Locale.ENGLISH);
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
		try {
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
		} finally {
			try {
				if (zip != null)
					zip.close();
			} catch (IOException e) {
			}
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
			if (showExtensionTabs()) {
				addExtensionTabs();
			}
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
		if (SHOW_SOURCE) {
			SHOW_SOURCE = false;
			InputContext primary = inputContextManager.getPrimaryContext();
			if (primary != null)
				return primary.getId();
		}
		
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			InputContext primary = inputContextManager.getPrimaryContext();
			if (primary == null)
				return null;
			if (BuildInputContext.CONTEXT_ID.equals(primary.getId()))
				firstPageId = BuildPage.PAGE_ID;
			else if (PluginInputContext.CONTEXT_ID.equals(primary.getId())) {
				if (inputContextManager.hasContext(BundleInputContext.CONTEXT_ID))
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
		if (object instanceof SearchHit)
			object = ((SearchHit)object).getHitElement();
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
	
    /* (non-Javadoc)
     * @see org.eclipse.ui.IShowEditorInput#showEditorInput(org.eclipse.ui.IEditorInput)
     */
    public void showEditorInput(IEditorInput editorInput) {
     	String name = editorInput.getName();
		String id = getActivePageInstance().getId();
		if (name.equals("build.properties")) { //$NON-NLS-1$
    		if (!BuildInputContext.CONTEXT_ID.equals(id))
    			setActivePage(SHOW_SOURCE ? BuildInputContext.CONTEXT_ID : BuildPage.PAGE_ID);
    	} else if (name.equals("plugin.xml") || name.equals("fragment.xml")) { //$NON-NLS-1$ //$NON-NLS-2$
    		if (!PluginInputContext.CONTEXT_ID.equals(id)) {
    			if (SHOW_SOURCE) {
    				setActivePage(PluginInputContext.CONTEXT_ID);
    			} else if (inputContextManager.hasContext(BundleInputContext.CONTEXT_ID)) {
    				setActivePage(ExtensionsPage.PAGE_ID);
    			} else {
    				setActivePage(OverviewPage.PAGE_ID);
    			}
    		}
    	} else if (!BundleInputContext.CONTEXT_ID.equals(id)) {
			setActivePage(SHOW_SOURCE ? BundleInputContext.CONTEXT_ID : OverviewPage.PAGE_ID);
    	}
    }
    
    public boolean showExtensionTabs() {
    	if (inputContextManager.hasContext(PluginInputContext.CONTEXT_ID))
    		return true;
    	return fShowExtensions && getAggregateModel().isEditable();
     }

    public boolean isEquinox() {
    	return fEquinox;
    }
    
    protected void addExtensionTabs() throws PartInitException {
    	addPage(3, new ExtensionPointsPage(this));
    	addPage(3, new ExtensionsPage(this));
    }
    
    protected void setShowExtensions(boolean show) throws BackingStoreException {
    	if (fPrefs != null) {
	    	fPrefs.putBoolean(ICoreConstants.EXTENSIONS_PROPERTY, show);
	    	fPrefs.flush();
    	}
    	fShowExtensions = show;
    }
}
