/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.io.File;
import java.util.Dictionary;
import org.eclipse.core.resources.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.FileEditorInput;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ManifestEditor extends MultiSourceEditor {
	protected void createResourceContexts(Dictionary contexts,
			IFileEditorInput input) {
		IFile file = input.getFile();
		IProject project = file.getProject();
		IFile manifestFile = null;
		IFile buildFile = null;
		IFile pluginFile = null;
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
			buildFile = project.getFile("build.properties");
			manifestFile = project.getFile("META-INF/MANIFEST.MF");
		}
		if (manifestFile.exists()) {
			IEditorInput in = new FileEditorInput(manifestFile);
			contexts.put(in, new BundleInputContext(this, in,
					file == manifestFile));
		}
		if (pluginFile.exists()) {
			FileEditorInput in = new FileEditorInput(pluginFile);
			contexts.put(in, new PluginInputContext(this, in,
					file == pluginFile, false));
		}
		if (buildFile.exists()) {
			FileEditorInput in = new FileEditorInput(buildFile);
			contexts
					.put(in, new BuildInputContext(this, in, file == buildFile));
		}
	}
	protected void createSystemFileContexts(Dictionary contexts,
			SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		File manifestFile = null;
		File buildFile = null;
		File pluginFile = null;
		String name = file.getName().toLowerCase();
		File rootFolder;
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
			contexts.put(in, new BundleInputContext(this, in,
					file == manifestFile));
		}
		if (pluginFile.exists()) {
			SystemFileEditorInput in = new SystemFileEditorInput(pluginFile);
			contexts.put(in, new PluginInputContext(this, in,
					file == pluginFile, false));
		}
		if (buildFile.exists()) {
			SystemFileEditorInput in = new SystemFileEditorInput(buildFile);
			contexts
					.put(in, new BuildInputContext(this, in, file == buildFile));
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
	protected void createStorageContexts(Dictionary contexts,
			IStorageEditorInput input) {
		String name = input.getName().toLowerCase();
		if (name.equals("manifest.mf")) {
			contexts.put(input, new BundleInputContext(this, input, true));
		} else if (name.equals("build.properties")) {
			contexts.put(input, new BuildInputContext(this, input, true));
		} else if (name.equals("plugin.xml")) {
			contexts.put(input,
					new PluginInputContext(this, input, true, false));
		} else if (name.equals("fragment.xml")) {
			contexts
					.put(input, new PluginInputContext(this, input, true, true));
		}
	}
	protected void contextMenuAboutToShow(IMenuManager manager) {
	}
	protected void addPages() {
		if (findContext(PluginInputContext.CONTEXT_ID) != null
				|| findContext(BundleInputContext.CONTEXT_ID) != null) {
			try {
				addPage(new OverviewPage(this));
				addPage(new DependenciesPage(this));
				addPage(new RuntimePage(this));
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
			}
		}
		if (findContext(PluginInputContext.CONTEXT_ID) != null) {
			try {
				addPage(new ExtensionsPage(this));
				addPage(new ExtensionPointsPage(this));
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
			}
		}
		if (findContext(BuildInputContext.CONTEXT_ID) != null) {
			try {
				addPage(new BuildPage(this));
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
			}
		}
		addSourcePage(BundleInputContext.CONTEXT_ID);
		addSourcePage(PluginInputContext.CONTEXT_ID);
		addSourcePage(BuildInputContext.CONTEXT_ID);
	}
	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			InputContext primary = getPrimaryContext();
			boolean isBundle = findContext(BundleInputContext.CONTEXT_ID) != null;
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
}