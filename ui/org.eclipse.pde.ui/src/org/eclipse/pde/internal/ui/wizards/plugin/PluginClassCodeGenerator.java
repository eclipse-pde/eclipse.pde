/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Peter Friese <peter.friese@itemis.de> - bug 241074
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.io.*;
import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.ui.templates.PluginReference;

public class PluginClassCodeGenerator {
	private PluginFieldData fPluginData;
	private IProject fProject;
	private String fQualifiedClassName;
	private boolean fGenerateTemplate;

	public PluginClassCodeGenerator(IProject project, String qualifiedClassName, PluginFieldData data, boolean generateTemplate) {
		fProject = project;
		fQualifiedClassName = qualifiedClassName;
		fPluginData = data;
		fGenerateTemplate = generateTemplate;
	}

	public IFile generate(IProgressMonitor monitor) throws CoreException {
		int nameloc = fQualifiedClassName.lastIndexOf('.');
		String packageName = (nameloc == -1) ? "" : fQualifiedClassName.substring(0, nameloc); //$NON-NLS-1$
		String className = fQualifiedClassName.substring(nameloc + 1);

		IPath path = new Path(packageName.replace('.', '/'));
		if (fPluginData.getSourceFolderName().trim().length() > 0)
			path = new Path(fPluginData.getSourceFolderName()).append(path);

		CoreUtility.createFolder(fProject.getFolder(path));

		IFile file = fProject.getFile(path.append(className + ".java")); //$NON-NLS-1$
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		// only generate/extend plug-in classes if it's a IU plug-in
		if (fPluginData.getOSGiFramework() != null || !fPluginData.isUIPlugin()) {
			generateActivatorClass(packageName, className, writer);
		} else {
			generatePluginClass(packageName, className, writer);
		}
		writer.flush();
		try {
			swriter.close();
			ByteArrayInputStream stream = new ByteArrayInputStream(swriter.toString().getBytes(fProject.getDefaultCharset()));
			if (file.exists())
				file.setContents(stream, false, true, monitor);
			else
				file.create(stream, false, monitor);
			stream.close();
		} catch (IOException e) {

		}
		return file;
	}

	private void generatePluginClass(String packageName, String className, PrintWriter writer) {
		if (!packageName.equals("")) { //$NON-NLS-1$
			writer.println("package " + packageName + ";"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println();
		}
		if (fPluginData.isUIPlugin()) {
			if (fGenerateTemplate)
				writer.println("import org.eclipse.jface.resource.ImageDescriptor;"); //$NON-NLS-1$
			writer.println("import org.eclipse.ui.plugin.AbstractUIPlugin;"); //$NON-NLS-1$
		} else {
			writer.println("import org.eclipse.core.runtime.Plugin;"); //$NON-NLS-1$
		}
		writer.println("import org.osgi.framework.BundleContext;"); //$NON-NLS-1$
		writer.println();
		writer.println("/**"); //$NON-NLS-1$
		writer.println(" * The activator class controls the plug-in life cycle"); //$NON-NLS-1$
		writer.println(" */"); //$NON-NLS-1$
		if (fPluginData.isUIPlugin())
			writer.println("public class " + className + " extends AbstractUIPlugin {"); //$NON-NLS-1$ //$NON-NLS-2$
		else
			writer.println("public class " + className + " extends Plugin {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println();
		writer.println("\t// The plug-in ID"); //$NON-NLS-1$
		writer.println("\tpublic static final String PLUGIN_ID = \"" + fPluginData.getId() + "\"; //$NON-NLS-1$"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println();
		writer.println("\t// The shared instance"); //$NON-NLS-1$
		writer.println("\tprivate static " + className + " plugin;"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t"); //$NON-NLS-1$
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * The constructor"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic " + className + "() {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();

		writer.println("\t/*"); //$NON-NLS-1$
		writer.println("\t * (non-Javadoc)"); //$NON-NLS-1$
		if (fPluginData.isUIPlugin())
			writer.println("\t * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)"); //$NON-NLS-1$
		else
			writer.println("\t * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic void start(BundleContext context) throws Exception {"); //$NON-NLS-1$
		writer.println("\t\tsuper.start(context);"); //$NON-NLS-1$
		writer.println("\t\tplugin = this;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();

		writer.println("\t/*"); //$NON-NLS-1$
		writer.println("\t * (non-Javadoc)"); //$NON-NLS-1$
		if (fPluginData.isUIPlugin())
			writer.println("\t * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)"); //$NON-NLS-1$
		else
			writer.println("\t * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic void stop(BundleContext context) throws Exception {"); //$NON-NLS-1$
		writer.println("\t\tplugin = null;"); //$NON-NLS-1$
		writer.println("\t\tsuper.stop(context);"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();

		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * Returns the shared instance"); //$NON-NLS-1$
		writer.println("\t *"); //$NON-NLS-1$
		writer.println("\t * @return the shared instance"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic static " + className + " getDefault() {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t\treturn plugin;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		if (fPluginData.isUIPlugin() && fGenerateTemplate) {
			writer.println("\t/**"); //$NON-NLS-1$
			writer.println("\t * Returns an image descriptor for the image file at the given"); //$NON-NLS-1$
			writer.println("\t * plug-in relative path"); //$NON-NLS-1$
			writer.println("\t *"); //$NON-NLS-1$
			writer.println("\t * @param path the path"); //$NON-NLS-1$
			writer.println("\t * @return the image descriptor"); //$NON-NLS-1$
			writer.println("\t */"); //$NON-NLS-1$
			writer.println("\tpublic static ImageDescriptor getImageDescriptor(String path) {"); //$NON-NLS-1$
			writer.println("\t\treturn imageDescriptorFromPlugin(PLUGIN_ID, path);"); //$NON-NLS-1$ 
			writer.println("\t}"); //$NON-NLS-1$
		}
		writer.println("}"); //$NON-NLS-1$
	}

	private void generateActivatorClass(String packageName, String className, PrintWriter writer) {
		if (!packageName.equals("")) { //$NON-NLS-1$
			writer.println("package " + packageName + ";"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println();
		}
		writer.println("import org.osgi.framework.BundleActivator;"); //$NON-NLS-1$
		writer.println("import org.osgi.framework.BundleContext;"); //$NON-NLS-1$
		writer.println();
		writer.println("public class " + className + " implements BundleActivator {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println();
		writer.println("\tprivate static BundleContext context;"); //$NON-NLS-1$
		writer.println();
		writer.println("\tstatic BundleContext getContext() {"); //$NON-NLS-1$
		writer.println("\t\treturn context;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("\t/*"); //$NON-NLS-1$
		writer.println("\t * (non-Javadoc)"); //$NON-NLS-1$
		writer.println("\t * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic void start(BundleContext bundleContext) throws Exception {"); //$NON-NLS-1$
		writer.println("\t\t" + className + ".context = bundleContext;"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("\t/*"); //$NON-NLS-1$
		writer.println("\t * (non-Javadoc)"); //$NON-NLS-1$
		writer.println("\t * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic void stop(BundleContext bundleContext) throws Exception {"); //$NON-NLS-1$
		writer.println("\t\t" + className + ".context = null;"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("}"); //$NON-NLS-1$		
	}

	public IPluginReference[] getDependencies() {
		ArrayList result = new ArrayList();
		if (fPluginData.isUIPlugin())
			result.add(new PluginReference("org.eclipse.ui", null, 0)); //$NON-NLS-1$
		if (!fPluginData.isLegacy() && fPluginData.getOSGiFramework() == null)
			result.add(new PluginReference(IPDEBuildConstants.BUNDLE_CORE_RUNTIME, null, 0));
		return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
	}

	public String[] getImportPackages() {
		return fPluginData.getOSGiFramework() != null ? new String[] {"org.osgi.framework;version=\"1.3.0\""} //$NON-NLS-1$
				: new String[0];
	}

}
