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
package org.eclipse.pde.internal.ui.wizards.plugin;
import java.io.*;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.wizards.templates.PluginReference;
import org.eclipse.pde.ui.IPluginFieldData;

public class PluginClassCodeGenerator {
	private IPluginFieldData fPluginData;
	private IProject fProject;
	private String fQualifiedClassName;

	public PluginClassCodeGenerator(IProject project, String qualifiedClassName,
			IPluginFieldData data) {
		this.fProject = project;
		this.fQualifiedClassName = qualifiedClassName;
		fPluginData = data;
	}

	public IFile generate(IProgressMonitor monitor) throws CoreException {
		int nameloc = fQualifiedClassName.lastIndexOf('.');
		String packageName = (nameloc == -1)
				? "" : fQualifiedClassName.substring(0, nameloc); //$NON-NLS-1$
		String className = fQualifiedClassName.substring(nameloc + 1);

		IPath path = new Path(packageName.replace('.', '/'));
		if (fPluginData.getSourceFolderName().trim().length() > 0)
			path = new Path(fPluginData.getSourceFolderName()).append(path);

		CoreUtility.createFolder(fProject.getFolder(path));

		IFile file = fProject.getFile(path.append(className + ".java")); //$NON-NLS-1$
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		if (fPluginData.isLegacy()) {
			generateLegacyPluginClass(packageName, className, writer);
		} else {
			generatePluginClass(packageName, className, writer);
		}
		writer.flush();
		try {
			swriter.close();
			ByteArrayInputStream stream = new ByteArrayInputStream(swriter.toString()
					.getBytes(fProject.getDefaultCharset()));
			if (file.exists())
				file.setContents(stream, false, true, monitor);
			else
				file.create(stream, false, monitor);
			stream.close();
		} catch (IOException e) {

		}
		return file;
	}

	private void generatePluginClass(String packageName, String className,
			PrintWriter writer) {
		if (!packageName.equals("")) { //$NON-NLS-1$
			writer.println("package " + packageName + ";"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println();
		}
		if (fPluginData.isUIPlugin()) {
			writer.println("import org.eclipse.ui.plugin.*;"); //$NON-NLS-1$
			writer.println("import org.eclipse.jface.resource.ImageDescriptor;"); //$NON-NLS-1$
		} else {
			writer.println("import org.eclipse.core.runtime.Plugin;"); //$NON-NLS-1$
		}
		writer.println("import org.osgi.framework.BundleContext;"); //$NON-NLS-1$
		writer.println();
		writer.println("/**"); //$NON-NLS-1$
		writer.println(" * The main plugin class to be used in the desktop."); //$NON-NLS-1$
		writer.println(" */"); //$NON-NLS-1$
		if (fPluginData.isUIPlugin())
			writer.println("public class " + className + " extends AbstractUIPlugin {"); //$NON-NLS-1$ //$NON-NLS-2$
		else
			writer.println("public class " + className + " extends Plugin {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t//The shared instance."); //$NON-NLS-1$
		writer.println("\tprivate static " + className + " plugin;"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t"); //$NON-NLS-1$
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * The constructor."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic " + className + "() {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t\tsuper();"); //$NON-NLS-1$
		writer.println("\t\tplugin = this;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();

		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * This method is called upon plug-in activation"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic void start(BundleContext context) throws Exception {"); //$NON-NLS-1$
		writer.println("\t\tsuper.start(context);"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();

		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * This method is called when the plug-in is stopped"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic void stop(BundleContext context) throws Exception {"); //$NON-NLS-1$
		writer.println("\t\tsuper.stop(context);"); //$NON-NLS-1$
		writer.println("\t\tplugin = null;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();

		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * Returns the shared instance."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic static " + className + " getDefault() {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t\treturn plugin;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		if (fPluginData.isUIPlugin()) {
			writer.println("\t/**"); //$NON-NLS-1$
		    writer.println("\t * Returns an image descriptor for the image file at the given"); //$NON-NLS-1$
		    writer.println("\t * plug-in relative path."); //$NON-NLS-1$
		    writer.println("\t *"); //$NON-NLS-1$
		    writer.println("\t * @param path the path"); //$NON-NLS-1$
		    writer.println("\t * @return the image descriptor"); //$NON-NLS-1$
		    writer.println("\t */"); //$NON-NLS-1$
		    writer.println("\tpublic static ImageDescriptor getImageDescriptor(String path) {"); //$NON-NLS-1$
		    writer.println("\t\treturn AbstractUIPlugin.imageDescriptorFromPlugin(\"" + fPluginData.getId() + "\", path);"); //$NON-NLS-1$ //$NON-NLS-2$
		    writer.println("\t}"); //$NON-NLS-1$
		}
		writer.println("}"); //$NON-NLS-1$
	}
	private void generateLegacyPluginClass(String packageName, String className,
			PrintWriter writer) {
		if (!packageName.equals("")) { //$NON-NLS-1$
			writer.println("package " + packageName + ";"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println();
		}
		if (fPluginData.isUIPlugin())
			writer.println("import org.eclipse.ui.plugin.*;"); //$NON-NLS-1$
		writer.println("import org.eclipse.core.runtime.*;"); //$NON-NLS-1$
		writer.println("import java.util.*;"); //$NON-NLS-1$
		writer.println();
		writer.println("/**"); //$NON-NLS-1$
		writer.println(" * The main plugin class to be used in the desktop."); //$NON-NLS-1$
		writer.println(" */"); //$NON-NLS-1$
		if (fPluginData.isUIPlugin())
			writer.println("public class " + className + " extends AbstractUIPlugin {"); //$NON-NLS-1$ //$NON-NLS-2$
		else
			writer.println("public class " + className + " extends Plugin {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t//The shared instance."); //$NON-NLS-1$
		writer.println("\tprivate static " + className + " plugin;"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t//Resource bundle."); //$NON-NLS-1$
		writer.println("\tprivate ResourceBundle resourceBundle;"); //$NON-NLS-1$
		writer.println("\t"); //$NON-NLS-1$
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * The constructor."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic " + className //$NON-NLS-1$
				+ "(IPluginDescriptor descriptor) {"); //$NON-NLS-1$
		writer.println("\t\tsuper(descriptor);"); //$NON-NLS-1$
		writer.println("\t\tplugin = this;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * Returns the shared instance."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic static " + className + " getDefault() {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t\treturn plugin;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * Returns the string from the plugin's resource bundle,"); //$NON-NLS-1$
		writer.println("\t * or 'key' if not found."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic static String getResourceString(String key) {"); //$NON-NLS-1$
		writer.println("\t\tResourceBundle bundle = " + className //$NON-NLS-1$
				+ ".getDefault().getResourceBundle();"); //$NON-NLS-1$
		writer.println("\t\ttry {"); //$NON-NLS-1$
		writer.println("\t\t\treturn (bundle != null) ? bundle.getString(key) : key;"); //$NON-NLS-1$
		writer.println("\t\t} catch (MissingResourceException e) {"); //$NON-NLS-1$
		writer.println("\t\t\treturn key;"); //$NON-NLS-1$
		writer.println("\t\t}"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * Returns the plugin's resource bundle,"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic ResourceBundle getResourceBundle() {"); //$NON-NLS-1$
		writer.println("\t\ttry {"); //$NON-NLS-1$
		writer.println("\t\t\tif (resourceBundle == null)"); //$NON-NLS-1$
		writer.println("\t\t\t\tresourceBundle   = ResourceBundle.getBundle(\"" //$NON-NLS-1$
				+ packageName + "." + className + "Resources\");"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t\t} catch (MissingResourceException x) {"); //$NON-NLS-1$
		writer.println("\t\t\tresourceBundle = null;"); //$NON-NLS-1$
		writer.println("\t\t}"); //$NON-NLS-1$
		writer.println("\t\treturn resourceBundle;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println("}"); //$NON-NLS-1$
	}

	public IPluginReference[] getDependencies() {
		ArrayList result = new ArrayList();
		if (fPluginData.isUIPlugin())
			result.add(new PluginReference("org.eclipse.ui", null, 0)); //$NON-NLS-1$
		if (!fPluginData.isLegacy())
			result.add(new PluginReference("org.eclipse.core.runtime", null, 0)); //$NON-NLS-1$
		return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
	}

}
