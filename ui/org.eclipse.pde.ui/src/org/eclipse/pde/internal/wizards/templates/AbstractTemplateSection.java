package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.core.resources.*;
import org.eclipse.pde.IBasePluginWizard;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.model.plugin.IPluginReference;
import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.*;
import java.io.*;
import java.net.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.core.internal.boot.InternalBootLoader;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

public abstract class AbstractTemplateSection implements ITemplateSection, IVariableProvider {
	protected IProject project;
	protected IPluginModelBase model;
	public static final String KEY_PLUGIN_ID = "pluginId";
	public static final String KEY_PLUGIN_NAME = "pluginName";

	/*
	 * @see ITemplateSection#getReplacementString(String)
	 */
	public String getReplacementString(String fileName, String key) {
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
	
	public Object getValue(String key) {
		return null;
	}

	public URL getTemplateLocation() {
		return null;
	}
	
	public String getDescription() {
		return "";
	}
	
	public String getPluginResourceString(String key) {
		ResourceBundle bundle = getPluginResourceBundle();
		if (bundle==null) return key;
		try {
			return bundle.getString(key);
		}
		catch (MissingResourceException e) {
			return key;
		}
	}
	
	protected abstract ResourceBundle getPluginResourceBundle();

	/*
	 * @see ITemplateSection#addPages(IBasePluginWizard)
	 */
	public void addPages(Wizard wizard) {
	}

	/*
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

	File getTemplateDirectory() {
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

	protected void generateFiles(IProgressMonitor monitor) throws CoreException {
		monitor.setTaskName("Generating: ");

		File templateDirectory = getTemplateDirectory();
		if (!templateDirectory.exists())
			return;
		generateFiles(templateDirectory, project, true, false, monitor);
		monitor.subTask("");
		monitor.worked(1);
	}

	protected void generateFiles(
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
				IContainer dstContainer=null;
			
				if (firstLevel) {
					binary = false;
					if (member.getName().equals("java")) {
						dstContainer = generateJavaSourceFolder(monitor);
					}
					else if (member.getName().equals("bin")) {
						binary = true;
						dstContainer = dst;
					}
				}
				if (dstContainer==null)
				   dstContainer = dst.getFolder(new Path(member.getName()));
				if (dstContainer instanceof IFolder && !dstContainer.exists())
					((IFolder)dstContainer).create(true, true, monitor);
				generateFiles(member, dstContainer, false, binary, monitor);
			} else {
				copyFile(member, dst, binary, monitor);
			}
		}
	}

	protected IFolder generateJavaSourceFolder(IProgressMonitor monitor)
		throws CoreException {
		IFolder sourceFolder = getSourceFolder(monitor);
		IPath path = sourceFolder.getProjectRelativePath();
		path =
			path.append(model.getPluginBase().getId().replace('.', File.separatorChar));
		for (int i = 1; i <= path.segmentCount(); i++) {
			IPath subpath = path.uptoSegment(i);
			IFolder subfolder = sourceFolder.getProject().getFolder(subpath);
			if (subfolder.exists() == false)
				subfolder.create(true, true, monitor);
		}
		return project.getFolder(path);
	}

	protected void copyFile(File file, IContainer dst, boolean binary, IProgressMonitor monitor)
		throws CoreException {
		monitor.subTask(file.getName());
		IFile dstFile = dst.getFile(new Path(file.getName()));

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

	protected InputStream getProcessedStream(File file, boolean binary) throws IOException {
		FileInputStream stream = new FileInputStream(file);
		if (binary) return stream;
		
		InputStreamReader reader = new InputStreamReader(stream);
		int bufsize = 1024;
		char [] cbuffer = new char[bufsize];
		int read = 0;
		StringBuffer keyBuffer = new StringBuffer();
		StringBuffer outBuffer = new StringBuffer();
		StringBuffer preBuffer = new StringBuffer();
		boolean newLine = true;
		TemplateControlStack preStack = new TemplateControlStack();
		preStack.setValueProvider(this);

		boolean replacementMode=false;
		boolean preprocessorMode=false;
		while (read!= -1) {
			read = reader.read(cbuffer);
			for (int i=0; i<read; i++) {
				char c = cbuffer[i];
				
				if (newLine && c=='%') {
					// preprocessor line
					preprocessorMode=true;
					preBuffer.delete(0, preBuffer.length());
					continue;
				}
				if (preprocessorMode) {
					if (c=='\\') {
						char c2=cbuffer[++i];
						preBuffer.append(c2);
						continue;
					}
					if (c=='\n') {
						// handle line
						preprocessorMode=false;
						newLine = true;
						String line = preBuffer.toString().trim();
						preStack.processLine(line);
						continue;
					}
					else {
						preBuffer.append(c);
					}
					continue;
				}
				
				if (preStack.getCurrentState()==false) {
					continue;
				}
				
				if (c=='$') {
					if (replacementMode) {
						replacementMode = false;
						String key = keyBuffer.toString();
						String value = getReplacementString(file.getName(), key);
						outBuffer.append(value);
						keyBuffer.delete(0, keyBuffer.length());
					}
					else {
						replacementMode = true;
					}
				}
				else {
					if (replacementMode)
						keyBuffer.append(c);
					else {
						outBuffer.append(c);
						if (c=='\n') {
							newLine = true;
						}
						else
							newLine = false;
					}
				}
			}
		}
		stream.close();
		return new ByteArrayInputStream(outBuffer.toString().getBytes());
	}

	protected abstract void updateModel(IProgressMonitor monitor)
		throws CoreException;

	/*
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
}