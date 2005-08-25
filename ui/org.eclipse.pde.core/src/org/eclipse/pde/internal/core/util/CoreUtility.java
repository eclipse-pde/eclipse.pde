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
package org.eclipse.pde.internal.core.util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.FactoryConfigurationError;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.plugin.PluginExtension;
import org.eclipse.pde.internal.core.plugin.PluginExtensionPoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class CoreUtility {

	public static void addNatureToProject(IProject proj, String natureId,
			IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}
	
	public static boolean isInterestingExtensionPoint(String point) {
		return "org.eclipse.pde.core.source".equals(point)  //$NON-NLS-1$
				|| "org.eclipse.core.runtime.products".equals(point) //$NON-NLS-1$
				|| "org.eclipse.pde.core.javadoc".equals(point) //$NON-NLS-1$
				|| "org.eclipse.ui.intro".equals(point); //$NON-NLS-1$
	}
	
	public static void createFolder(IFolder folder) throws CoreException {
		if (!folder.exists()) {
			IContainer parent = folder.getParent();
			if (parent instanceof IFolder) {
				createFolder((IFolder) parent);
			}
			folder.create(true, true, null);
		}
	}

	public static void createProject(IProject project, IPath location,
			IProgressMonitor monitor) throws CoreException {
		if (!Platform.getLocation().equals(location)) {
			IProjectDescription desc = project.getWorkspace()
					.newProjectDescription(project.getName());
			desc.setLocation(location);
			project.create(desc, monitor);
		} else
			project.create(monitor);
	}
	
	public static String getWritableString(String source) {
		if (source == null)
			return ""; //$NON-NLS-1$
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}
	
	public static String normalize(String text) {
		if (text == null || text.trim().length() == 0)
			return ""; //$NON-NLS-1$
		
		text = text.replaceAll("\\r|\\n|\\f|\\t", " "); //$NON-NLS-1$ //$NON-NLS-2$
		return text.replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static void deleteContent(File curr) {
		if (curr.exists()) {
			if (curr.isDirectory()) {
				File[] children = curr.listFiles();
				if (children != null) {
					for (int i = 0; i < children.length; i++) {
						deleteContent(children[i]);
					}
				}
			}
			curr.delete();
		}
	}
	
	public static Element writeExtension(Document doc, IPluginExtension extension) {
		Element child = doc.createElement("extension"); //$NON-NLS-1$
		if (extension.getPoint() != null)
			child.setAttribute("point", getWritableString(extension.getPoint())); //$NON-NLS-1$
		if (extension.getName() != null)
			child.setAttribute("name", getWritableString(extension.getName())); //$NON-NLS-1$
		if (extension.getId() != null)
			child.setAttribute("id", getWritableString(extension.getId())); //$NON-NLS-1$
		if (extension instanceof PluginExtension)
			child.setAttribute("line", Integer.toString(((PluginExtension)extension).getStartLine())); //$NON-NLS-1$
		IPluginObject[] children = extension.getChildren();
		for (int i = 0; i < children.length; i++) {
			child.appendChild(writeElement(doc, (IPluginElement)children[i]));
		}
		return child;	
	}
	
	public static Element writeElement(Document doc, IPluginElement element) {
		Element child = doc.createElement(element.getName());
		IPluginAttribute[] attrs = element.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			child.setAttribute(attrs[i].getName(), getWritableString(attrs[i].getValue()));
		}
		IPluginObject[] elements = element.getChildren();
		for (int i = 0; i < elements.length; i++) {
			child.appendChild(writeElement(doc, (IPluginElement)elements[i]));
		}
		return child;
	}
	
	public static Element writeExtensionPoint(Document doc, IPluginExtensionPoint extPoint) {
		Element child = doc.createElement("extension-point"); //$NON-NLS-1$
		if (extPoint.getId() != null)
			child.setAttribute("id", getWritableString(extPoint.getId())); //$NON-NLS-1$
		if (extPoint.getName() != null)
			child.setAttribute("name", getWritableString(extPoint.getName())); //$NON-NLS-1$
		if (extPoint.getSchema() != null)
			child.setAttribute("schema", getWritableString(extPoint.getSchema())); //$NON-NLS-1$
		if (extPoint instanceof PluginExtensionPoint)
			child.setAttribute("line", Integer.toString(((PluginExtensionPoint)extPoint).getStartLine())); //$NON-NLS-1$
		return child;	
	}
	
    public static boolean guessUnpack(BundleDescription bundle) {
		if (bundle == null)
			return true;
	
		if (new File(bundle.getLocation()).isFile()) 
			return false;
		
		IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
		IContainer container = root.getContainerForLocation(new Path(bundle.getLocation()));
		if (container == null) 
			return true;
		
		if (container instanceof IProject) {
			try {
				if (!((IProject)container).hasNature(JavaCore.NATURE_ID)) 
					return true;
			} catch (CoreException e) {
			}
		}		
		
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(bundle);
		if (model == null)
			return true;
	
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		if (libraries.length == 0 && PDECore.getDefault().getModelManager().isOSGiRuntime())
			return false;
	
		for (int i = 0; i < libraries.length; i++) {
			if (libraries[i].getName().equals(".")) //$NON-NLS-1$
				return false;
		}
		return true;
	}
    
	public static boolean jarContainsResource(File file, String resource, boolean directory) {
		ZipFile jarFile = null;
		try {
			jarFile = new ZipFile(file, ZipFile.OPEN_READ);
			ZipEntry resourceEntry = jarFile.getEntry(resource); 
			if (resourceEntry != null)
				return directory ? resourceEntry.isDirectory() : true;
		} catch (IOException e) {
		} catch (FactoryConfigurationError e) {
		} finally {
			try {
				if (jarFile != null)
					jarFile.close();
			} catch (IOException e2) {
			}
		}
		return false;
	}
}
