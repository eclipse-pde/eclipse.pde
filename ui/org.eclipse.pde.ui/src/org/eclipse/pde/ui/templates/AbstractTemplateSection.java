package org.eclipse.pde.ui.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.URL;
import java.util.*;

import org.eclipse.core.internal.boot.InternalBootLoader;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.templates.ControlStack;

/**
 * Common function for template sections. It is recommended
 * to subclass this class rather than implementing ITemplateSection
 * directly when providing extension templates.
 */

public abstract class AbstractTemplateSection
	implements ITemplateSection, IVariableProvider {
	/**
	 * The project handle.
	 */
	protected IProject project;
	/**
	 * The plug-in model.
	 */
	protected IPluginModelBase model;
	/**
	 * The key for the main plug-in class of the plug-in that
	 * the template is used for (value="pluginClass");
	 */
	public static final String KEY_PLUGIN_CLASS = "pluginClass";
	/**
	 * The key for the plug-in id of the plug-in that
	 * the template is used for (value="pluginId").
	 */
	public static final String KEY_PLUGIN_ID = "pluginId";
	/**
	 * The key for the plug-in name of the plug-in that
	 * the template is used for (value="pluginName").
	 */
	public static final String KEY_PLUGIN_NAME = "pluginName";
	/**
	 * The key for the package name that will be created by
	 * this teamplate (value="packageName").
	 */
	public static final String KEY_PACKAGE_NAME = "packageName";

	private static final String KEY_GENERATING = "AbstractTemplateSection.generating";
	
	private boolean pagesAdded=false;
	/**
	 * The default implementation of this method provides
	 * values of the following keys: <samp>pluginClass</samp>,
	 * <samp>pluginId</samp> and <samp>pluginName</samp>.
	 * @see ITemplateSection#getReplacementString(String)
	 */
	public String getReplacementString(String fileName, String key) {
		if (key.equals(KEY_PLUGIN_CLASS) && model != null) {
			if (model instanceof IPluginModel) {
				IPlugin plugin = (IPlugin) model.getPluginBase();
				return plugin.getClassName();
			}
		}
		if (key.equals(KEY_PLUGIN_ID) && model != null) {
			IPluginBase plugin = model.getPluginBase();
			return plugin.getId();
		}
		if (key.equals(KEY_PLUGIN_NAME) && model != null) {
			IPluginBase plugin = model.getPluginBase();
			return plugin.getTranslatedName();
		}
		return key;
	}

	/**
	 *@see IVariableProvider#getValue(String)
	 */

	public Object getValue(String key) {
		return null;
	}
	/**
	 * @see ITemplateSection#getTemplateLocation()
	 */
	public URL getTemplateLocation() {
		return null;
	}
	/**
	 * @see ITemplateSection#getDescription()
	 */
	public String getDescription() {
		return "";
	}
	/**
	 * Returns the translated version of the resource string
	 * represented by the provided key.
	 * @param key the key of the required resource string
	 * @return the translated version of the required resource string
	 * @see #getPluginResourceBundle()
	 */
	public String getPluginResourceString(String key) {
		ResourceBundle bundle = getPluginResourceBundle();
		if (bundle == null)
			return key;
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}
	/**
	 * An abstract method that returns the resource bundle 
	 * that corresponds to the best match of <samp>plugin.properties</samp>
	 * file for the current locale (in case of fragments, the file
	 * is <samp>fragment.properties</samp>).
	 * @return resource bundle for plug-in properties file or <samp>null</samp>
	 * if not found.
	 */
	protected abstract ResourceBundle getPluginResourceBundle();

	/*
	 * @see ITemplateSection#addPages(IBasePluginWizard)
	 */
	public void addPages(Wizard wizard) {
	}
	
	public boolean getPagesAdded() {
		return pagesAdded;
	}
	
	/**
	 * Marks that pages have been added to the wizard by this template.
	 * Call this method in 'addPages'.
	 * @see #addPages(Wizard)
	 */
	protected void markPagesAdded() {
		pagesAdded = true;
	}

	/*
	 * The default implementation of the interface method. The
	 * returned value is 1.
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return 1;
	}

	/*
	 * @see ITemplateSection#getDependencies()
	 */
	public IPluginReference[] getDependencies() {
		return new IPluginReference[0];
	}

	/**
	 * Returns the folder with Java files in the target project. 
	 * The default implementation looks for source folders in
	 * the classpath of the target folders and picks the first one
	 * encountered. Subclasses may override this behaviour.
	 * @param monitor progress monitor to use
	 * @return source folder that will be used to generate Java files
	 * or <samp>null</samp> if none found.
	 */

	protected IFolder getSourceFolder(IProgressMonitor monitor)
		throws CoreException {
		IFolder sourceFolder = null;

		try {
			IJavaProject javaProject = JavaCore.create(project);
			IClasspathEntry[] classpath = javaProject.getRawClasspath();
			for (int i = 0; i < classpath.length; i++) {
				IClasspathEntry entry = classpath[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath path = entry.getPath().removeFirstSegments(1);
					sourceFolder = project.getFolder(path);
					break;
				}
			}
		} catch (JavaModelException e) {
		}
		return sourceFolder;
	}

	/**
	 * Generates files as part of the template execution. The default
	 * implementation uses template location as a root of the file
	 * templates. The files found in the location are processed in
	 * the following way:
	 * <ul>
	 * <li>Files and folders found in the directory <samp>bin</samp> are copied
	 * into the target project without modification.</li>
	 * <li>Files found in the directory <samp>java</samp> are copied
	 * into the Java source folder by creating the folder structure
	 * that corresponds to the package name (variable <samp>packageName</samp>). 
	 * Java files are subject to conditional generation and variable replacement.</li>
	 * <li>All other files and folders are copied directly into the
	 * target folder with the conditional generation and variable replacement
	 * for files. Variable replacement also includes file names.
	 * @param monitor progress monitor to use to indicate generation progress
	 */
	protected void generateFiles(IProgressMonitor monitor) throws CoreException {
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_GENERATING));

		File templateDirectory = getTemplateDirectory();
		if (!templateDirectory.exists())
			return;
		generateFiles(templateDirectory, project, true, false, monitor);
		monitor.subTask("");
		monitor.worked(1);
	}
	/**
	 * Tests if the folder found in the template location should
	 * be created in the target project. Subclasses may use this
	 * method to conditionally block creation of the entire
	 * directories (subject to user choices).
	 * @param sourceFolder the folder found in the template location
	 * that needs to be created.
	 * @return <samp>true</samp> if the specified folder should be created
	 * in the project, or <samp>false</samp> to skip it, including all
	 * subfolders and files it may contain. The default implementation
	 * is <samp>true</samp>.
	 */
	protected boolean isOkToCreateFolder(File sourceFolder) {
		return true;
	}

	/**
	 * Tests if the file found in the template location should
	 * be created in the target project. Subclasses may use this
	 * method to conditionally block createion of the file
	 * (subject to user choices).
	 * @param sourceFile the file found in the template location
	 * that needs to be created.
	 * @return <samp>true</samp> if the specified file should be
	 * created in the project or <samp>false</samp> to skip it. The
	 * default implementation is <samp>true</samp>.
	 */
	protected boolean isOkToCreateFile(File sourceFile) {
		return true;
	}

	/**
	 * Subclass must implement this method to add the required entries
	 * in the plug-in model.
	 * @param monitor the progress monitor to be used
	 */
	protected abstract void updateModel(IProgressMonitor monitor)
		throws CoreException;

	/**
	 * The default implementation of the interface method. It will 
	 * generate required files found in the template location and
	 * then call <samp>updateModel</samp> to add the required
	 * manifest entires.
	 * @see ITemplateSection#execute(IProject, IPluginModelBase, IProgressMonitor)
	 */
	public void execute(
		IProject project,
		IPluginModelBase model,
		IProgressMonitor monitor)
		throws CoreException {
		this.project = project;
		this.model = model;
		generateFiles(monitor);
		updateModel(monitor);
	}
	/**
	 * A utility method to create an extension object for the plug-in model
	 * from the provided extension point id.
	 * @param pointId the identifier of the target extension point
	 * @param reuse if true, new extension object will be created only
	 * if an extension with the same Id does not exist.
	 * @return an existing extension (if exists and <samp>reuse</samp> is <samp>true</samp>),
	 * or a new extension object otherwise.
	 */
	protected IPluginExtension createExtension(String pointId, boolean reuse)
		throws CoreException {
		if (reuse) {
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				IPluginExtension extension = extensions[i];
				if (extension.getPoint().equalsIgnoreCase(pointId)) {
					return extension;
				}
			}
		}
		IPluginExtension extension = model.getFactory().createExtension();
		extension.setPoint(pointId);
		return extension;
	}

	private File getTemplateDirectory() {
		try {
			URL location = getTemplateLocation();
			if (location == null)
				return null;
			URL url = InternalBootLoader.resolve(location);
			String name = url.getFile();
			return new File(name);
		} catch (Exception e) {
			return null;
		}
	}

	private void generateFiles(
		File src,
		IContainer dst,
		boolean firstLevel,
		boolean binary,
		IProgressMonitor monitor)
		throws CoreException {
		File[] members = src.listFiles();

		for (int i = 0; i < members.length; i++) {
			File member = members[i];
			if (member.isDirectory()) {
				IContainer dstContainer = null;

				if (firstLevel) {
					binary = false;
					if (member.getName().equals("java")) {
						dstContainer = generateJavaSourceFolder(monitor);
					} else if (member.getName().equals("bin")) {
						binary = true;
						dstContainer = dst;
					}
				}
				if (dstContainer == null) {
					if (isOkToCreateFolder(member) == false)
						continue;
					String folderName = getProcessedString(member.getName(), member.getName());
					dstContainer = dst.getFolder(new Path(folderName));
				}
				if (dstContainer instanceof IFolder && !dstContainer.exists())
					 ((IFolder) dstContainer).create(true, true, monitor);
				generateFiles(member, dstContainer, false, binary, monitor);
			} else {
				if (isOkToCreateFile(member)) {
					if (firstLevel)  binary=false;
					copyFile(member, dst, binary, monitor);
				}
			}
		}
	}

	private IFolder generateJavaSourceFolder(IProgressMonitor monitor)
		throws CoreException {
		IFolder sourceFolder = getSourceFolder(monitor);
		IPath path = sourceFolder.getProjectRelativePath();
		Object packageValue = getValue(KEY_PACKAGE_NAME);
		String packageName = packageValue != null ? packageValue.toString() : null;
		if (packageName == null)
			packageName = model.getPluginBase().getId();
		path = path.append(packageName.replace('.', File.separatorChar));
		for (int i = 1; i <= path.segmentCount(); i++) {
			IPath subpath = path.uptoSegment(i);
			IFolder subfolder = sourceFolder.getProject().getFolder(subpath);
			if (subfolder.exists() == false)
				subfolder.create(true, true, monitor);
		}
		return project.getFolder(path);
	}

	private void copyFile(
		File file,
		IContainer dst,
		boolean binary,
		IProgressMonitor monitor)
		throws CoreException {
		String targetFileName = getProcessedString(file.getName(), file.getName());

		monitor.subTask(targetFileName);
		IFile dstFile = dst.getFile(new Path(targetFileName));

		try {
			InputStream stream = getProcessedStream(file, binary);
			if (dstFile.exists()) {
				dstFile.setContents(stream, true, false, monitor);
			} else {
				dstFile.create(stream, true, monitor);
			}
			stream.close();

		} catch (IOException e) {
		}
	}

	private String getProcessedString(String fileName, String source) {
		if (source.indexOf('$') == -1)
			return source;
		int loc = -1;
		StringBuffer buffer = new StringBuffer();
		boolean replacementMode = false;
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '$') {
				if (replacementMode) {
					String key = source.substring(loc, i);
					String value = getReplacementString(fileName, key);
					buffer.append(value);
					replacementMode = false;
				} else {
					replacementMode = true;
					loc = i + 1;
					continue;
				}
			} else if (!replacementMode)
				buffer.append(c);
		}
		return buffer.toString();
	}

	private InputStream getProcessedStream(File file, boolean binary)
		throws IOException {
		FileInputStream stream = new FileInputStream(file);
		if (binary)
			return stream;

		InputStreamReader reader = new InputStreamReader(stream);
		int bufsize = 1024;
		char[] cbuffer = new char[bufsize];
		int read = 0;
		StringBuffer keyBuffer = new StringBuffer();
		StringBuffer outBuffer = new StringBuffer();
		StringBuffer preBuffer = new StringBuffer();
		boolean newLine = true;
		ControlStack preStack = new ControlStack();
		preStack.setValueProvider(this);

		boolean replacementMode = false;
		boolean preprocessorMode = false;
		boolean escape = false;
		while (read != -1) {
			read = reader.read(cbuffer);
			for (int i = 0; i < read; i++) {
				char c = cbuffer[i];

				if (escape) {
					StringBuffer buf = preprocessorMode ? preBuffer : outBuffer;
					buf.append(c);
					escape = false;
					continue;
				}

				if (newLine && c == '%') {
					// preprocessor line
					preprocessorMode = true;
					preBuffer.delete(0, preBuffer.length());
					continue;
				}
				if (preprocessorMode) {
					if (c == '\\') {
						escape = true;
						continue;
					}
					if (c == '\n') {
						// handle line
						preprocessorMode = false;
						newLine = true;
						String line = preBuffer.toString().trim();
						preStack.processLine(line);
						continue;
					} else {
						preBuffer.append(c);
					}
					continue;
				}

				if (preStack.getCurrentState() == false) {
					continue;
				}

				if (c == '$') {
					if (replacementMode) {
						replacementMode = false;
						String key = keyBuffer.toString();
						String value = getReplacementString(file.getName(), key);
						outBuffer.append(value);
						keyBuffer.delete(0, keyBuffer.length());
					} else {
						replacementMode = true;
					}
				} else {
					if (replacementMode)
						keyBuffer.append(c);
					else {
						outBuffer.append(c);
						if (c == '\n') {
							newLine = true;
						} else
							newLine = false;
					}
				}
			}
		}
		stream.close();
		return new ByteArrayInputStream(outBuffer.toString().getBytes());
	}

}