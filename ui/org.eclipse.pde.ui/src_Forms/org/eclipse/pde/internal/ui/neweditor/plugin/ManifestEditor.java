/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.io.File;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.build.*;
import org.eclipse.pde.internal.ui.neweditor.context.*;
import org.eclipse.pde.internal.ui.neweditor.runtime.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
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
		return new PluginInputContextManager();
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
			manifestFile = file;
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
					file == pluginFile, false));
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
		if (name.equals("manifest.mf")) {
			manager
					.putContext(input,
							new BundleInputContext(this, input, true));
		} else if (name.equals("build.properties")) {
			manager.putContext(input, new BuildInputContext(this, input, true));
		} else if (name.equals("plugin.xml")) {
			manager.putContext(input, new PluginInputContext(this, input, true,
					false));
		} else if (name.equals("fragment.xml")) {
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
	protected PDESourcePage createXMLSourcePage(PDEFormEditor editor, String title, String name) {
		return new ManifestSourcePage(editor, title, name);
	}
	
	protected IContentOutlinePage createContentOutline() {
		return new ManifestOutlinePage(this);
	}

}