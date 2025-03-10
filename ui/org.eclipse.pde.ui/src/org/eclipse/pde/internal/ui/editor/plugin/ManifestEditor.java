/*******************************************************************************
 *  Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Läubrich - Bug 577637 - ManifestEditor must use Project-BundleRoot to open Manifest
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IIdentifiable;
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
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.build.IBuildObject;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelProvider;
import org.eclipse.pde.internal.core.natures.BndProject;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.ILauncherFormPageHelper;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.JarEntryFile;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDELauncherFormEditor;
import org.eclipse.pde.internal.ui.editor.bnd.BndInputContext;
import org.eclipse.pde.internal.ui.editor.bnd.BndSourcePage;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildPage;
import org.eclipse.pde.internal.ui.editor.build.BuildSourcePage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifestsAction;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.osgi.resource.Resource;
import org.osgi.service.prefs.BackingStoreException;

public class ManifestEditor extends PDELauncherFormEditor implements IShowEditorInput {

	private static int BUILD_INDEX = 5;
	private static boolean SHOW_SOURCE;
	private boolean fEquinox = true;
	private boolean fShowExtensions = true;
	private IEclipsePreferences fPrefs;
	private PluginExportAction fExportAction;
	private ILauncherFormPageHelper fLauncherHelper;

	@Override
	protected String getEditorID() {
		return IPDEUIConstants.MANIFEST_EDITOR_ID;
	}

	public static IEditorPart openPluginEditor(String id) {
		return openPluginEditor(PluginRegistry.findModel(id));
	}

	public static IEditorPart openPluginEditor(Resource bd) {
		return openPluginEditor(PluginRegistry.findModel(bd));
	}

	public static IEditorPart openPluginEditor(IPluginModelBase model) {
		if (model == null) {
			Display.getDefault().beep();
			return null;
		}
		return openPluginEditor(model, false);
	}

	public static IEditorPart openPluginEditor(IPluginModelBase model, boolean source) {
		return open(model.getPluginBase(), source);
	}

	public static IEditorPart open(Object object, boolean source) {
		SHOW_SOURCE = source;
		if (object instanceof IPluginObject pluginObject) {
			ISharedPluginModel model = pluginObject.getModel();
			if (model instanceof IBundlePluginModelProvider provider) {
				model = provider.getBundlePluginModel();
			}
			if (model instanceof IPluginModelBase pluginModel) {
				String filename = pluginModel.isFragmentModel() //
						? ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR
						: ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR;
				if (!(object instanceof IPluginExtension) && !(object instanceof IPluginExtensionPoint)) {
					String installLocation = model.getInstallLocation();
					if (installLocation == null) {
						return null;
					}
					File file = new File(installLocation);
					if (file.isFile()) {
						if (CoreUtility.jarContainsResource(file, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR, false)) {
							filename = ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR;
						}
					} else if (new File(file, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR).exists()) {
						filename = ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR;
					}
				}
				IResource resource = model.getUnderlyingResource();
				if (resource == null) {
					return openExternalPlugin(new File(model.getInstallLocation()), filename);
				}
				IFile filePath = PDEProject.getBundleRelativeFile(resource.getProject(), IPath.fromOSString(filename));
				return openEditor(new FileEditorInput(filePath));
			}
		}
		if (object instanceof BaseDescription baseDescription) {
			BundleDescription desc = baseDescription.getSupplier();
			String id = desc.getSymbolicName();
			String version = desc.getVersion().toString();

			ModelEntry entry = PluginRegistry.findEntry(id);
			IPluginModelBase[] models = entry.getActiveModels();
			for (IPluginModelBase model : models) {
				if (version.equals(model.getPluginBase().getVersion())) {
					return open(model.getPluginBase(), true);
				}
			}
		}
		return null;
	}

	private static IEditorPart openExternalPlugin(File location, String filename) {
		if (location.isFile()) {
			try (ZipFile zipFile = new ZipFile(location)) {
				if (zipFile.getEntry(filename) != null) {
					return openEditor(new JarEntryEditorInput(new JarEntryFile(zipFile, filename)));
				}
			} catch (IOException e) {
				// ignore
			}
		} else {
			File file = new File(location, filename);
			if (file.exists()) {
				try {
					IFileStore store = EFS.getStore(file.toURI());
					return openEditor(new FileStoreEditorInput(store));
				} catch (CoreException e) {
				}
			}
		}
		return null;
	}

	private static IEditorPart openEditor(IEditorInput input) {
		if (input != null) {
			try {
				return PDEPlugin.getActivePage().openEditor(input, IPDEUIConstants.MANIFEST_EDITOR_ID);
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
			}
		}
		return null;
	}

	@Override
	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		IFile file = input.getFile();
		IContainer container = file.getParent();

		IFile manifestFile = null;
		IFile buildFile = null;
		IFile pluginFile = null;
		boolean fragment = false;

		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals(ICoreConstants.MANIFEST_FILENAME_LOWER_CASE)) {
			if (container instanceof IFolder) {
				container = container.getParent();
			}
			manifestFile = file;
			buildFile = container.getFile(ICoreConstants.BUILD_PROPERTIES_PATH);
			pluginFile = createPluginFile(container);
		} else if (name.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)
				|| name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)) {
			pluginFile = file;
			fragment = name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
			buildFile = container.getFile(ICoreConstants.BUILD_PROPERTIES_PATH);
			manifestFile = container.getFile(ICoreConstants.MANIFEST_PATH);
		} else if (name.endsWith(BndProject.INSTRUCTIONS_FILE_EXTENSION)) {
			IProject project = file.getProject();
			if (name.equalsIgnoreCase(BndProject.INSTRUCTIONS_FILE)) {
				manifestFile = PDEProject.getManifest(project);
				buildFile = PDEProject.getBuildProperties(project);
				pluginFile = PDEProject.getPluginXml(project);
			}
			IEditorInput in = new FileEditorInput(file);
			BndInputContext bndInputContext = new BndInputContext(this, in, true);
			manager.putContext(in, bndInputContext);
			manager.monitorFile(file);
			manager.addInputContextListener(bndInputContext);
		}
		if (manifestFile != null && manifestFile.exists()) {
			IEditorInput in = new FileEditorInput(manifestFile);
			manager.putContext(in, new BundleInputContext(this, in, file == manifestFile));
		}
		manager.monitorFile(manifestFile);
		if (pluginFile != null && pluginFile.exists()) {
			FileEditorInput in = new FileEditorInput(pluginFile);
			manager.putContext(in, new PluginInputContext(this, in, file == pluginFile, fragment));
		}
		manager.monitorFile(pluginFile);
		if (buildFile != null && buildFile.exists()) {
			FileEditorInput in = new FileEditorInput(buildFile);
			manager.putContext(in, new BuildInputContext(this, in, false));
		}
		manager.monitorFile(buildFile);

		fPrefs = new ProjectScope(container.getProject()).getNode(PDECore.PLUGIN_ID);
		if (fPrefs != null) {
			fShowExtensions = fPrefs.getBoolean(ICoreConstants.EXTENSIONS_PROPERTY, true);
			fEquinox = fPrefs.getBoolean(ICoreConstants.EQUINOX_PROPERTY, true);
		}
	}

	@Override
	protected InputContextManager createInputContextManager() {
		PluginInputContextManager manager = new PluginInputContextManager(this);
		manager.setUndoManager(new PluginUndoManager(this));
		return manager;
	}

	@Override
	protected boolean hasMissingResources() {
		return super.hasMissingResources() && fInputContextManager.findContext(BndInputContext.CONTEXT_ID) == null;
	}

	@Override
	public void monitoredFileAdded(IFile file) {
		if (fInputContextManager == null) {
			return;
		}
		String name = file.getName();
		if (name.equalsIgnoreCase(ICoreConstants.MANIFEST_FILENAME)) {
			if (!fInputContextManager.hasContext(BundleInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new BundleInputContext(this, in, false));
			}
		} else if (name.equalsIgnoreCase(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)) {
			if (!fInputContextManager.hasContext(PluginInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new PluginInputContext(this, in, false, false));
			}
		} else if (name.equalsIgnoreCase(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)) {
			if (!fInputContextManager.hasContext(PluginInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new PluginInputContext(this, in, false, true));
			}
		} else if (name.equalsIgnoreCase(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			if (!fInputContextManager.hasContext(BuildInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new BuildInputContext(this, in, false));
			}
		} else if (name.equalsIgnoreCase(BndProject.INSTRUCTIONS_FILE)) {
			if (!fInputContextManager.hasContext(BndInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new BndInputContext(this, in, false));
			}
		}
	}

	public void ensurePluginContextPresence() {
		if (fInputContextManager.hasContext(PluginInputContext.CONTEXT_ID)) {
			return;
		}
		IProject project = fInputContextManager.getCommonProject();
		WorkspacePluginModelBase model = null;
		IFile file = null;
		if (fInputContextManager.getAggregateModel() instanceof IFragmentModel) {
			file = PDEProject.getFragmentXml(project);
			model = new WorkspaceFragmentModel(file, false);
		} else {
			file = PDEProject.getPluginXml(project);
			model = new WorkspacePluginModel(file, false);
		}

		IPluginBase pluginBase = model.getPluginBase(true);
		try {
			pluginBase.setSchemaVersion(TargetPlatformHelper.getSchemaVersion());
		} catch (CoreException e) {
		}
		model.save();
		IEditorInput in = new FileEditorInput(file);
		fInputContextManager.putContext(in, new PluginInputContext(this, in, false, false));

		updateBuildProperties(file.getName());
	}

	private void updateBuildProperties(String filename) {
		try {
			InputContext context = fInputContextManager.findContext(BuildInputContext.CONTEXT_ID);
			if (context != null) {
				IBuildModel buildModel = (IBuildModel) context.getModel();
				IBuild build = buildModel.getBuild();
				IBuildEntry entry = build.getEntry("bin.includes"); //$NON-NLS-1$
				if (entry == null) {
					entry = buildModel.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
					build.add(entry);
				}
				if (!entry.contains(filename)) {
					entry.addToken(filename);
				}
			}
		} catch (CoreException e) {
		}
	}

	@Override
	public boolean monitoredFileRemoved(IFile file) {
		// TODO may need to check with the user if there
		// are unsaved changes in the model for the
		// file that just got removed under us.
		return true;
	}

	@Override
	public void editorContextAdded(InputContext context) {
		addSourcePage(context.getId());
		try {
			if (context.getId().equals(BuildInputContext.CONTEXT_ID)) {
				addPage(BUILD_INDEX, new BuildPage(this));
			} else {
				updateFirstThreePages();
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	public void contextRemoved(InputContext context) {
		InputContext bundleInput = fInputContextManager.findContext(BundleInputContext.CONTEXT_ID);
		InputContext instructionInput = fInputContextManager.findContext(BndInputContext.CONTEXT_ID);
		if (bundleInput == null && instructionInput == null) {
			close(true);
		}
	}

	private void updateFirstThreePages() {
		try {
			int index = getActivePage();
			removePage(RuntimePage.PAGE_ID);
			removePage(DependenciesPage.PAGE_ID);
			removePage(OverviewPage.PAGE_ID);
			if (fInputContextManager.hasContext(BundleInputContext.CONTEXT_ID)) {
				addPage(0, new RuntimePage(this));
				addPage(0, new DependenciesPage(this));
				addPage(0, new OverviewPage(this));
			}
			setActivePage(index);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	protected void createSystemFileContexts(InputContextManager manager, FileStoreEditorInput input) {
		File file = new File(input.getURI());
		File manifestFile = null;
		File buildFile = null;
		File pluginFile = null;
		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals(ICoreConstants.MANIFEST_FILENAME_LOWER_CASE)) {
			manifestFile = file;
			File dir = file.getParentFile().getParentFile();
			buildFile = new File(dir, ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
			pluginFile = createPluginFile(dir);
		} else if (name.equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			buildFile = file;
			File dir = file.getParentFile();
			pluginFile = createPluginFile(dir);
			manifestFile = new File(dir, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
		} else if (name.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)
				|| name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)) {
			pluginFile = file;
			File dir = file.getParentFile();
			buildFile = new File(dir, ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
			manifestFile = new File(dir, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
		}
		try {
			if (manifestFile != null && manifestFile.exists()) {
				IFileStore store = EFS.getStore(manifestFile.toURI());
				IEditorInput in = new FileStoreEditorInput(store);
				manager.putContext(in, new BundleInputContext(this, in, file == manifestFile));
			}
			if (pluginFile != null && pluginFile.exists()) {
				IFileStore store = EFS.getStore(pluginFile.toURI());
				IEditorInput in = new FileStoreEditorInput(store);
				manager.putContext(in, new PluginInputContext(this, in, file == pluginFile,
						name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)));
			}
			if (buildFile != null && buildFile.exists()) {
				IFileStore store = EFS.getStore(buildFile.toURI());
				IEditorInput in = new FileStoreEditorInput(store);
				manager.putContext(in, new BuildInputContext(this, in, file == buildFile));
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private File createPluginFile(File dir) {
		File pluginFile = new File(dir, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
		if (!pluginFile.exists()) {
			pluginFile = new File(dir, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
		}
		return pluginFile;
	}

	private IFile createPluginFile(IContainer container) {
		IFile pluginFile = container.getFile(ICoreConstants.PLUGIN_PATH);
		if (!pluginFile.exists()) {
			pluginFile = container.getFile(ICoreConstants.FRAGMENT_PATH);
		}
		return pluginFile;
	}

	@Override
	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		if (input instanceof JarEntryEditorInput) {
			createJarEntryContexts(manager, (JarEntryEditorInput) input);
			return;
		}

		String name = input.getName().toLowerCase(Locale.ENGLISH);
		if (name.startsWith(ICoreConstants.MANIFEST_FILENAME_LOWER_CASE)) {
			manager.putContext(input, new BundleInputContext(this, input, true));
		} else if (name.startsWith(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			manager.putContext(input, new BuildInputContext(this, input, true));
		} else if (name.startsWith(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)) {
			manager.putContext(input, new PluginInputContext(this, input, true, false));
		} else if (name.startsWith(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)) {
			manager.putContext(input, new PluginInputContext(this, input, true, true));
		}
	}

	protected void createJarEntryContexts(InputContextManager manager, JarEntryEditorInput input) {
		IStorage storage = input.getStorage();
		try (ZipFile zip = storage.getAdapter(ZipFile.class)) {
			if (zip == null) {
				return;
			}

			if (zip.getEntry(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR) != null) {
				input = new JarEntryEditorInput(new JarEntryFile(zip, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR));
				manager.putContext(input, new BundleInputContext(this, input,
						storage.getName().equals(ICoreConstants.MANIFEST_FILENAME)));
			}

			if (zip.getEntry(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) != null) {
				input = new JarEntryEditorInput(new JarEntryFile(zip, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR));
				manager.putContext(input, new PluginInputContext(this, input,
						storage.getName().equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), false));
			} else if (zip.getEntry(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR) != null) {
				input = new JarEntryEditorInput(new JarEntryFile(zip, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR));
				manager.putContext(input, new PluginInputContext(this, input,
						storage.getName().equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR), true));
			}

			if (zip.getEntry(ICoreConstants.BUILD_FILENAME_DESCRIPTOR) != null) {
				input = new JarEntryEditorInput(new JarEntryFile(zip, ICoreConstants.BUILD_FILENAME_DESCRIPTOR));
				manager.putContext(input, new BuildInputContext(this, input,
						storage.getName().equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)));
			}
		} catch (IOException e) {
			// ignore
		}
	}

	@Override
	protected void addEditorPages() {
		try {
			int extTabPosition;
			if (fInputContextManager.hasContext(BundleInputContext.CONTEXT_ID)) {
				addPage(new OverviewPage(this));
				addPage(new DependenciesPage(this));
				addPage(new RuntimePage(this));
				extTabPosition = 3;
			} else {
				extTabPosition = 0;
			}
			if (showExtensionTabs()) {
				addExtensionTabs(extTabPosition);
			}
			if (fInputContextManager.hasContext(BuildInputContext.CONTEXT_ID)) {
				addPage(new BuildPage(this));
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(BundleInputContext.CONTEXT_ID);
		addSourcePage(PluginInputContext.CONTEXT_ID);
		addSourcePage(BuildInputContext.CONTEXT_ID);
		addSourcePage(BndInputContext.CONTEXT_ID);
	}

	private boolean isSourcePageID(String pageID) {
		if (pageID == null) {
			return false;
		}
		// Determine whether the page ID is a source page ID
		switch (pageID)
			{
			case BuildInputContext.CONTEXT_ID: // build.properites
			case PluginInputContext.CONTEXT_ID: // plugin.xml
			case BundleInputContext.CONTEXT_ID: // MANIFEST.MF
			case BndInputContext.CONTEXT_ID: // bnd instruction
				return true;
			default:
				break;
			}
		return false;
	}

	@Override
	protected String computeInitialPageId() {
		// Used by plug-in search view to open query results, etc.
		if (SHOW_SOURCE) {
			SHOW_SOURCE = false;
			return getPrimarySourceInputContextID();
		}
		// Retrieve the initial page
		String firstPageId = super.computeInitialPageId();
		// If none is defined, return the default
		// If the initial page is a source page ID (e.g. build.properties,
		// MANIFEST.MF, plugin.xml), then return the page ID belonging to the
		// input context or file used to open this editor
		if (firstPageId == null) {
			if (fInputContextManager.hasContext(BndInputContext.CONTEXT_ID)) {
				InputContext context = fInputContextManager.findContext(BndInputContext.CONTEXT_ID);
				if (context != null) {
					return context.getId();
				}
			}
			return OverviewPage.PAGE_ID;
		} else if (isSourcePageID(firstPageId)) {
			return getPrimarySourceInputContextID();
		}

		return firstPageId;
	}

	@Override
	protected String getPropertyEditorPageKey(IFileEditorInput input) {
		// We are using the project itself to persist the editor page key
		// property
		// The value persists even after the editor is closed
		// The project is used rather than the file in this case because the
		// manifest editor has 3 input files and only one build.properties,
		// one MANIFEST.MF and one plugin.xml should exist for each project.
		// We also want the last editor page to be shared between the two
		// input contexts. The build.properties file has its own editor.
		IFile file = input.getFile();
		IProject project = file.getProject();
		// Ensure the project is defined
		if (project == null) {
			// Check the file for the key
			return super.getPropertyEditorPageKey(input);
		}
		// Get the persistent editor page key from the project
		try {
			return project.getPersistentProperty(IPDEUIConstants.PROPERTY_MANIFEST_EDITOR_PAGE_KEY);
		} catch (CoreException e) {
			// Check the file for the key
			return super.getPropertyEditorPageKey(input);
		}
	}

	@Override
	protected void setPropertyEditorPageKey(IFileEditorInput input, String pageId) {
		// We are using the project itself to persist the editor page key
		// property
		// The value persists even after the editor is closed
		// The project is used rather than the file in this case because the
		// manifest editor has 3 input files and only one build.properties,
		// one MANIFEST.MF and one plugin.xml should exist for each project.
		// We also want the last editor page to be shared between the two
		// input contexts. The build.properties file has its own editor.
		IFile file = input.getFile();
		IProject project = file.getProject();
		// Ensure the project is defined
		if (project == null) {
			// Set the key on the file
			super.setPropertyEditorPageKey(input, pageId);
			return;
		}
		// Set the editor page ID as a persistent property on the project
		try {
			project.setPersistentProperty(IPDEUIConstants.PROPERTY_MANIFEST_EDITOR_PAGE_KEY, pageId);
		} catch (CoreException e) {
			// Set the key on the file
			super.setPropertyEditorPageKey(input, pageId);
			return;
		}
	}

	private String getPrimarySourceInputContextID() {
		// Get the input context used to open this editor
		InputContext primary = fInputContextManager.getPrimaryContext();
		// Ensure it is defined
		if (primary == null) {
			return null;
		}
		// Return its ID
		return primary.getId();
	}

	@Override
	protected IEditorPart createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		if (contextId.equals(PluginInputContext.CONTEXT_ID)) {
			return new ManifestSourcePage(editor, title, name);
		}
		if (contextId.equals(BuildInputContext.CONTEXT_ID)) {
			return new BuildSourcePage(editor, title, name);
		}
		if (contextId.equals(BundleInputContext.CONTEXT_ID)) {
			return new BundleSourcePage(editor, title, name);
		}
		if (contextId.equals(BndInputContext.CONTEXT_ID)) {
			return new BndSourcePage(editor, contextId, title);
		}
		return super.createSourcePage(editor, title, name, contextId);
	}

	@Override
	protected ISortableContentOutlinePage createContentOutline() {
		return new ManifestOutlinePage(this);
	}

	@Override
	public <T> T getAdapter(Class<T> key) {
		// No property sheet needed - block super
		if (key.equals(IPropertySheetPage.class)) {
			return null;
		}
		return super.getAdapter(key);
	}

	@Override
	public String getTitle() {
		if (getAggregateModel() instanceof IPluginModelBase model) {
			if (!model.isValid()) {
				return super.getTitle();
			}
			String text = getTitleText(model.getPluginBase());
			if (text == null) {
				return super.getTitle();
			}
			return model.getResourceString(text);
		}
		return super.getTitle();
	}

	@Override
	public String getTitleProperty() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		String pref = store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
		if (pref != null && pref.equals(IPreferenceConstants.VALUE_USE_NAMES)) {
			return IPluginObject.P_NAME;
		}
		return IIdentifiable.P_ID;
	}

	private String getTitleText(IPluginBase pluginBase) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		String pref = store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
		if (pref != null && pref.equals(IPreferenceConstants.VALUE_USE_NAMES)) {
			return pluginBase.getName();
		}
		return pluginBase.getId();
	}

	@Override
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof IFile) {
			String name = ((IFile) object).getName();
			if (name.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)
					|| name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)) {
				context = fInputContextManager.findContext(PluginInputContext.CONTEXT_ID);
			} else if (name.equals(ICoreConstants.MANIFEST_FILENAME)) {
				context = fInputContextManager.findContext(BundleInputContext.CONTEXT_ID);
			} else if (name.equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
				context = fInputContextManager.findContext(BuildInputContext.CONTEXT_ID);
			}
		} else if (object instanceof IBuildObject) {
			context = fInputContextManager.findContext(BuildInputContext.CONTEXT_ID);
		} else if (object instanceof IPluginExtensionPoint || object instanceof IPluginExtension) {
			context = fInputContextManager.findContext(PluginInputContext.CONTEXT_ID);
		} else {
			context = fInputContextManager.findContext(BundleInputContext.CONTEXT_ID);
			if (context == null) {
				context = fInputContextManager.findContext(PluginInputContext.CONTEXT_ID);
			}
		}
		return context;
	}

	@Override
	public void showEditorInput(IEditorInput editorInput) {
		String name = editorInput.getName();
		IFormPage page = getActivePageInstance();
		String id = page == null ? "" : page.getId(); //$NON-NLS-1$
		if (name.equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			if (!BuildInputContext.CONTEXT_ID.equals(id)) {
				setActivePage(SHOW_SOURCE ? BuildInputContext.CONTEXT_ID : BuildPage.PAGE_ID);
			}
		} else if (name.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR)
				|| name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR)) {
			if (!PluginInputContext.CONTEXT_ID.equals(id)) {
				if (SHOW_SOURCE) {
					setActivePage(PluginInputContext.CONTEXT_ID);
				} else if (fInputContextManager.hasContext(BundleInputContext.CONTEXT_ID)) {
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
		if (fInputContextManager.hasContext(PluginInputContext.CONTEXT_ID)) {
			return true;
		}
		IBaseModel model = getAggregateModel();
		return fShowExtensions && model != null && model.isEditable();
	}

	public boolean isEquinox() {
		return fEquinox;
	}

	protected void addExtensionTabs(int extTabPosition) throws PartInitException {
		addPage(extTabPosition, new ExtensionPointsPage(this));
		addPage(extTabPosition, new ExtensionsPage(this));
	}

	protected void setShowExtensions(boolean show) throws BackingStoreException {
		if (fPrefs != null) {
			fPrefs.putBoolean(ICoreConstants.EXTENSIONS_PROPERTY, show);
			fPrefs.flush();
		}
		fShowExtensions = show;
	}

	@Override
	public void contributeToToolbar(IToolBarManager manager) {
		contributeLaunchersToToolbar(manager);
		manager.add(getOrganizeManifestsAction());
		manager.add(getExportAction());
	}

	private Action getOrganizeManifestsAction() {
		Action action = new Action() {
			@Override
			public void run() {
				OrganizeManifestsAction organizeAction = new OrganizeManifestsAction();
				organizeAction.runOrganizeManfestsAction(new StructuredSelection(getCommonProject()));
			}
		};
		action.setToolTipText(PDEUIMessages.OrganizeManifestJob_taskName);
		action.setImageDescriptor(PDEPluginImages.DESC_ORGANIZE_MANIFESTS_ACTION);
		return action;
	}

	private PluginExportAction getExportAction() {
		if (fExportAction == null) {
			fExportAction = new PluginExportAction(this);
			fExportAction.setToolTipText(PDEUIMessages.PluginEditor_exportTooltip);
			fExportAction.setImageDescriptor(PDEPluginImages.DESC_EXPORT_PLUGIN_TOOL);
		}
		return fExportAction;
	}

	@Override
	protected ILauncherFormPageHelper getLauncherHelper() {
		if (fLauncherHelper == null) {
			fLauncherHelper = new PluginLauncherFormPageHelper(this);
		}
		return fLauncherHelper;
	}
}
