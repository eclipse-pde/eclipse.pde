/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.zip.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.product.ISplashHandlerConstants;
import org.eclipse.pde.internal.ui.wizards.product.UpdateSplashHandlerAction;
import org.eclipse.pde.internal.ui.wizards.templates.ControlStack;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.IVariableProvider;
import org.osgi.framework.Bundle;

/**
 * TemplateFileGenerator
 *
 */
public class TemplateFileGenerator implements IVariableProvider {

	// TODO: MP: SPLASH: Merge this utility back with template (maybe as an abstract base class?) - Extracted from org.eclipse.pde.ui.templates.AbstractTemplateSection
	// TODO: MP: SPLASH: Major code-clean-up required

	/**
	 * The key for the main plug-in class of the plug-in that the template is
	 * used for (value="pluginClass").  The return value is a fully-qualified class name.
	 */
	public static final String KEY_PLUGIN_CLASS = "pluginClass"; //$NON-NLS-1$

	/**
	 * The key for the simple class name of a bundle activator (value="activator")
	 * 
	 * @since 3.3
	 */
	public static final String KEY_ACTIVATOR_SIMPLE = "activator"; //$NON-NLS-1$
	/**
	 * The key for the plug-in id of the plug-in that the template is used for
	 * (value="pluginId").
	 */
	public static final String KEY_PLUGIN_ID = "pluginId"; //$NON-NLS-1$
	/**
	 * The key for the plug-in name of the plug-in that the template is used for
	 * (value="pluginName").
	 */
	public static final String KEY_PLUGIN_NAME = "pluginName"; //$NON-NLS-1$
	/**
	 * The key for the package name that will be created by this teamplate
	 * (value="packageName").
	 */
	public static final String KEY_PACKAGE_NAME = "packageName"; //$NON-NLS-1$	

	private IProject fProject;

	private IPluginModelBase fModel;

	private String fPluginID;

	private String fPackage;

	private String fClass;

	private String fTemplate;

	/**
	 * @param project
	 * @param model
	 */
	public TemplateFileGenerator(IProject project, IPluginModelBase model, String pluginID, String targetPackage, String targetClass, String template) {
		fProject = project;
		fModel = model;
		fPluginID = pluginID;
		fPackage = targetPackage;
		fClass = targetClass;
		fTemplate = template;
	}

	/**
	 * Generates files as part of the template execution. The default
	 * implementation uses template location as a root of the file templates.
	 * {@link #generateFiles(IProgressMonitor monitor, URL locationUrl)} on how
	 * the location gets processed.
	 * 
	 * @param monitor
	 *            progress monitor to use to indicate generation progress
	 */
	public void generateFiles(IProgressMonitor monitor) throws CoreException {
		// Generate files using the default template location
		generateFiles(monitor, getTemplateLocation());
		// Generate files using the shared branding location (for splash screen)
		Bundle templateBundle = Platform.getBundle("org.eclipse.pde.ui.templates"); //$NON-NLS-1$
		if (templateBundle == null) {
			return;
		}
		generateFiles(monitor, templateBundle.getEntry("branding/")); //$NON-NLS-1$
	}

	public URL getTemplateLocation() {
		Bundle bundle = Platform.getBundle("org.eclipse.pde.ui.templates"); //$NON-NLS-1$
		if (bundle == null) {
			return null;
		}

		try {
			String[] candidates = getDirectoryCandidates();
			for (int i = 0; i < candidates.length; i++) {
				if (bundle.getEntry(candidates[i]) != null) {
					URL candidate = new URL(bundle.getEntry("/"), candidates[i]); //$NON-NLS-1$
					return candidate;
				}
			}
		} catch (MalformedURLException e) {
		}
		return null;
	}

	/**
	 * Generates files as part of the template execution.
	 * The files found in the location are processed in the following way:
	 * <ul>
	 * <li>Files and folders found in the directory <samp>bin </samp> are
	 * copied into the target project without modification.</li>
	 * <li>Files found in the directory <samp>java </samp> are copied into the
	 * Java source folder by creating the folder structure that corresponds to
	 * the package name (variable <samp>packageName </samp>). Java files are
	 * subject to conditional generation and variable replacement.</li>
	 * <li>All other files and folders are copied directly into the target
	 * folder with the conditional generation and variable replacement for
	 * files. Variable replacement also includes file names.</li>
	 * </ul>
	 * 
	 * @since 3.3
	 * @param monitor
	 *            progress monitor to use to indicate generation progress
	 * @param locationUrl a url pointing to a file/directory that will be copied into the template
	 */
	protected void generateFiles(IProgressMonitor monitor, URL locationUrl) throws CoreException {
		monitor.setTaskName(PDEUIMessages.AbstractTemplateSection_generating);

		if (locationUrl == null) {
			return;
		}
		try {
			locationUrl = FileLocator.resolve(locationUrl);
			locationUrl = FileLocator.toFileURL(locationUrl);
		} catch (IOException e) {
			return;
		}
		if ("file".equals(locationUrl.getProtocol())) { //$NON-NLS-1$
			File templateDirectory = new File(locationUrl.getFile());
			if (!templateDirectory.exists())
				return;
			generateFiles(templateDirectory, fProject, true, false, monitor);
		} else if ("jar".equals(locationUrl.getProtocol())) { //$NON-NLS-1$
			String file = locationUrl.getFile();
			int exclamation = file.indexOf('!');
			if (exclamation < 0)
				return;
			URL fileUrl = null;
			try {
				fileUrl = new URL(file.substring(0, exclamation));
			} catch (MalformedURLException mue) {
				return;
			}
			File pluginJar = new File(fileUrl.getFile());
			if (!pluginJar.exists())
				return;
			String templateDirectory = file.substring(exclamation + 1); // "/some/path/"
			IPath path = new Path(templateDirectory);
			ZipFile zipFile = null;
			try {
				zipFile = new ZipFile(pluginJar);
				generateFiles(zipFile, path, fProject, true, false, monitor);
			} catch (ZipException ze) {
			} catch (IOException ioe) {
			} finally {
				if (zipFile != null) {
					try {
						zipFile.close();
					} catch (IOException e) {
					}
				}
			}

		}
		monitor.subTask(""); //$NON-NLS-1$
		monitor.worked(1);
	}

	private void generateFiles(File src, IContainer dst, boolean firstLevel, boolean binary, IProgressMonitor monitor) throws CoreException {
		File[] members = src.listFiles();

		for (int i = 0; i < members.length; i++) {
			File member = members[i];
			if (member.isDirectory()) {
				IContainer dstContainer = null;

				if (firstLevel) {
					binary = false;
					if (!isOkToCreateFolder(member))
						continue;

					if (member.getName().equals("java")) { //$NON-NLS-1$
						IFolder sourceFolder = getSourceFolder(monitor);
						dstContainer = generateJavaSourceFolder(sourceFolder, monitor);
					} else if (member.getName().equals("bin")) { //$NON-NLS-1$
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
					if (firstLevel)
						binary = false;
					InputStream in = null;
					try {
						in = new FileInputStream(member);
						copyFile(member.getName(), in, dst, binary, monitor);
					} catch (IOException ioe) {
					} finally {
						if (in != null)
							try {
								in.close();
							} catch (IOException ioe2) {
							}
					}
				}
			}
		}
	}

	private void copyFile(String fileName, InputStream input, IContainer dst, boolean binary, IProgressMonitor monitor) throws CoreException {
		String targetFileName = getProcessedString(fileName, fileName);

		monitor.subTask(targetFileName);
		IFile dstFile = dst.getFile(new Path(targetFileName));

		try {
			InputStream stream = getProcessedStream(fileName, input, binary);
			if (dstFile.exists()) {
				dstFile.setContents(stream, true, true, monitor);
			} else {
				dstFile.create(stream, true, monitor);
			}
			stream.close();

		} catch (IOException e) {
		}
	}

	private void generateFiles(ZipFile zipFile, IPath path, IContainer dst, boolean firstLevel, boolean binary, IProgressMonitor monitor) throws CoreException {
		int pathLength = path.segmentCount();
		// Immidiate children
		Map childZipEntries = new HashMap(); // "dir/" or "dir/file.java"

		for (Enumeration zipEntries = zipFile.entries(); zipEntries.hasMoreElements();) {
			ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
			IPath entryPath = new Path(zipEntry.getName());
			if (entryPath.segmentCount() <= pathLength) {
				// ancestor or current directory
				continue;
			}
			if (!path.isPrefixOf(entryPath)) {
				// not a descendant
				continue;
			}
			if (entryPath.segmentCount() == pathLength + 1) {
				childZipEntries.put(zipEntry.getName(), zipEntry);
			} else {
				String name = entryPath.uptoSegment(pathLength + 1).addTrailingSeparator().toString();
				if (!childZipEntries.containsKey(name)) {
					ZipEntry dirEntry = new ZipEntry(name);
					childZipEntries.put(name, dirEntry);
				}
			}
		}

		for (Iterator it = childZipEntries.values().iterator(); it.hasNext();) {
			ZipEntry zipEnry = (ZipEntry) it.next();
			String name = new Path(zipEnry.getName()).lastSegment().toString();
			if (zipEnry.isDirectory()) {
				IContainer dstContainer = null;

				if (firstLevel) {
					binary = false;
					if (name.equals("java")) { //$NON-NLS-1$
						IFolder sourceFolder = getSourceFolder(monitor);
						dstContainer = generateJavaSourceFolder(sourceFolder, monitor);
					} else if (name.equals("bin")) { //$NON-NLS-1$
						binary = true;
						dstContainer = dst;
					}
				}
				if (dstContainer == null) {
					if (isOkToCreateFolder(new File(path.toFile(), name)) == false)
						continue;
					String folderName = getProcessedString(name, name);
					dstContainer = dst.getFolder(new Path(folderName));
				}
				if (dstContainer instanceof IFolder && !dstContainer.exists())
					((IFolder) dstContainer).create(true, true, monitor);
				generateFiles(zipFile, path.append(name), dstContainer, false, binary, monitor);
			} else {
				if (isOkToCreateFile(new File(path.toFile(), name))) {
					if (firstLevel)
						binary = false;
					InputStream in = null;
					try {
						in = zipFile.getInputStream(zipEnry);
						copyFile(name, in, dst, binary, monitor);
					} catch (IOException ioe) {
					} finally {
						if (in != null)
							try {
								in.close();
							} catch (IOException ioe2) {
							}
					}
				}
			}
		}
	}

	/**
	 * Tests if the folder found in the template location should be created in
	 * the target project. Subclasses may use this method to conditionally block
	 * the creation of entire directories (subject to user choices).
	 * 
	 * @param sourceFolder
	 *            the folder that is tested
	 * @return <code>true</code> if the provided folder should be created in
	 *         the workspace, <code>false</code> if the values of the
	 *         substitution variables indicate otherwise.
	 */
	protected boolean isOkToCreateFolder(File sourceFolder) {
		boolean extensibleTemplateSelected = UpdateSplashHandlerAction.isExtensibleTemplateSelected(fTemplate);
		String sourceFolderString = sourceFolder.toString();

		if ((extensibleTemplateSelected == false) && sourceFolderString.endsWith("icons")) { //$NON-NLS-1$
			return false;
		} else if ((extensibleTemplateSelected == false) && sourceFolderString.endsWith("schema")) { //$NON-NLS-1$
			return false;
		}

		return true;
	}

	/**
	 * Tests if the file found in the template location should be created in the
	 * target project. Subclasses may use this method to conditionally block
	 * creation of the file (subject to user choices).
	 * 
	 * @param sourceFile
	 *            the file found in the template location that needs to be
	 *            created.
	 * @return <samp>true </samp> if the specified file should be created in the
	 *         project or <samp>false </samp> to skip it. The default
	 *         implementation is <samp>true </samp>.
	 */
	protected boolean isOkToCreateFile(File sourceFile) {
		String javaSuffix = ".java"; //$NON-NLS-1$
		String targetFile = fClass + javaSuffix;
		String copyFile = sourceFile.toString();
		// Prevent needless copying
		// TODO: MP: SPLASH: Propagate / share copy prevention code with org.eclipse.pde.internal.ui.templates.ide.SplashHandlersTemplate
		if (copyFile.endsWith(javaSuffix)) {
			if ((copyFile.endsWith(targetFile) == false) || fProject.exists(new Path("src" + '/' + fPackage.replace('.', '/') + '/' + targetFile))) { //$NON-NLS-1$
				return false;
			}
		} else if (copyFile.endsWith("splash.bmp") && //$NON-NLS-1$
				(fProject.exists(new Path("splash.bmp")))) { //$NON-NLS-1$
			return false;
		} else if (copyFile.endsWith(".png")) { //$NON-NLS-1$
			if (copyFile.endsWith("af.png") && //$NON-NLS-1$
					fProject.exists(new Path("icons/af.png"))) { //$NON-NLS-1$
				return false;
			} else if (copyFile.endsWith("embedded.png") && //$NON-NLS-1$
					fProject.exists(new Path("icons/embedded.png"))) { //$NON-NLS-1$
				return false;
			} else if (copyFile.endsWith("enterprise.png") && //$NON-NLS-1$
					fProject.exists(new Path("icons/enterprise.png"))) { //$NON-NLS-1$
				return false;
			} else if (copyFile.endsWith("rcp.png") && //$NON-NLS-1$
					fProject.exists(new Path("icons/rcp.png"))) { //$NON-NLS-1$
				return false;
			} else if (copyFile.endsWith("languages.png") && //$NON-NLS-1$
					fProject.exists(new Path("icons/languages.png"))) { //$NON-NLS-1$
				return false;
			}
		} else if (copyFile.endsWith("splashExtension.exsd") && //$NON-NLS-1$
				(fProject.exists(new Path("schema/splashExtension.exsd")))) { //$NON-NLS-1$
			return false;
		}

		return true;
	}

	/**
	 * Returns the folder with Java files in the target project. The default
	 * implementation looks for source folders in the classpath of the target
	 * folders and picks the first one encountered. Subclasses may override this
	 * behaviour.
	 * 
	 * @param monitor
	 *            progress monitor to use
	 * @return source folder that will be used to generate Java files or
	 *         <samp>null </samp> if none found.
	 */

	protected IFolder getSourceFolder(IProgressMonitor monitor) throws CoreException {
		IFolder sourceFolder = null;

		try {
			IJavaProject javaProject = JavaCore.create(fProject);
			IClasspathEntry[] classpath = javaProject.getRawClasspath();
			for (int i = 0; i < classpath.length; i++) {
				IClasspathEntry entry = classpath[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath path = entry.getPath().removeFirstSegments(1);
					if (path.segmentCount() > 0)
						sourceFolder = fProject.getFolder(path);
					break;
				}
			}
		} catch (JavaModelException e) {
		}
		return sourceFolder;
	}

	/**
	 * @see IVariableProvider#getValue(String)
	 */

	public Object getValue(String key) {
		return getKeyValue(key);
	}

	private IFolder generateJavaSourceFolder(IFolder sourceFolder, IProgressMonitor monitor) throws CoreException {
		Object packageValue = getValue(KEY_PACKAGE_NAME);
		//
		String packageName = packageValue != null ? packageValue.toString() : null;
		if (packageName == null)
			packageName = fModel.getPluginBase().getId();
		IPath path = new Path(packageName.replace('.', File.separatorChar));
		if (sourceFolder != null)
			path = sourceFolder.getProjectRelativePath().append(path);

		for (int i = 1; i <= path.segmentCount(); i++) {
			IPath subpath = path.uptoSegment(i);
			IFolder subfolder = fProject.getFolder(subpath);
			if (subfolder.exists() == false)
				subfolder.create(true, true, monitor);
		}
		return fProject.getFolder(path);
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
					String value = key.length() == 0 ? "$" //$NON-NLS-1$
							: getReplacementString(fileName, key);
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

	/**
	 * The default implementation of this method provides values of the
	 * following keys: <samp>pluginClass </samp>, <samp>pluginId </samp> and
	 * <samp>pluginName </samp>.
	 * 
	 * @see ITemplateSection#getReplacementString(String,String)
	 */
	public String getReplacementString(String fileName, String key) {
		String result = getKeyValue(key);
		return result != null ? result : key;
	}

	private String getKeyValue(String key) {
		if (fModel == null)
			return null;

		// TODO: MP: SPLASH: Broken and not needed, underlying model does not have class, id, translated name parameters defined
		if (key.equals(KEY_PLUGIN_CLASS) && fModel instanceof IPluginModel) {
			IPlugin plugin = (IPlugin) fModel.getPluginBase();
			return plugin.getClassName();
		}

		// TODO: MP: SPLASH: Broken and not needed, underlying model does not have class, id, translated name parameters defined
		if (key.equals(KEY_ACTIVATOR_SIMPLE) && fModel instanceof IPluginModel) {
			IPlugin plugin = (IPlugin) fModel.getPluginBase();
			String qualified = plugin.getClassName();
			if (qualified != null) {
				int lastDot = qualified.lastIndexOf('.');
				return (lastDot != -1 && lastDot < qualified.length() - 1) ? qualified.substring(lastDot + 1) : qualified;
			}
		}

		if (key.equals(KEY_PLUGIN_ID)) {
			return fPluginID;
			// Old implementation, does not work
			//IPluginBase plugin = fModel.getPluginBase();
			//return plugin.getId();
		}
		// TODO: MP: SPLASH: Broken and not needed, underlying model does not have class, id, translated name parameters defined
		if (key.equals(KEY_PLUGIN_NAME)) {
			IPluginBase plugin = fModel.getPluginBase();
			return plugin.getTranslatedName();
		}

		if (key.equals(KEY_PACKAGE_NAME) && fModel instanceof IPluginModel) {
			return fPackage;
			// Old implementation, does not work
//			IPlugin plugin = (IPlugin) fModel.getPluginBase();
//			String qualified = plugin.getClassName();
//			if (qualified != null) {
//				int lastDot = qualified.lastIndexOf('.');
//				return (lastDot != -1) ? qualified.substring(0, lastDot) : qualified;
//			}
		}
		return null;
	}

	private InputStream getProcessedStream(String fileName, InputStream stream, boolean binary) throws IOException, CoreException {
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
					}
					preBuffer.append(c);

					continue;
				}

				if (preStack.getCurrentState() == false) {
					continue;
				}

				if (c == '$') {
					if (replacementMode) {
						replacementMode = false;
						String key = keyBuffer.toString();
						String value = key.length() == 0 ? "$" //$NON-NLS-1$
								: getReplacementString(fileName, key);
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
		return new ByteArrayInputStream(outBuffer.toString().getBytes(fProject.getDefaultCharset()));
	}

	private String[] getDirectoryCandidates() {
		double version = getTargetVersion();
		ArrayList result = new ArrayList();
		if (version >= 3.3)
			result.add("templates_3.3" + "/" + getSectionId() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (version >= 3.2)
			result.add("templates_3.2" + "/" + getSectionId() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (version >= 3.1)
			result.add("templates_3.1" + "/" + getSectionId() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (version >= 3.0)
			result.add("templates_3.0" + "/" + getSectionId() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return (String[]) result.toArray(new String[result.size()]);
	}

	public String getSectionId() {
		return ISplashHandlerConstants.F_UNQUALIFIED_EXTENSION_ID;
	}

	protected double getTargetVersion() {
		try {
			IPluginBase plugin = fModel.getPluginBase();
			if (plugin instanceof IBundlePluginBase)
				return Double.parseDouble(((IBundlePluginBase) plugin).getTargetVersion());
		} catch (NumberFormatException e) {
		}
		return TargetPlatformHelper.getTargetVersion();
	}

}
