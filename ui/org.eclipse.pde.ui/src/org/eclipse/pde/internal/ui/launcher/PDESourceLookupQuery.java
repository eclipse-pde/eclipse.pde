/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaFieldVariable;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDEClasspathContainer;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class PDESourceLookupQuery implements ISafeRunnable {
	
	private static String ECLIPSE_CLASSLOADER = "org.eclipse.core.runtime.adaptor.EclipseClassLoader"; //$NON-NLS-1$
	
	private Object fElement;
	private Object fResult;
	
	public PDESourceLookupQuery(Object object) {
		fElement = object;		
	}

	public void handleException(Throwable exception) {
	}

	public void run() throws Exception {
		if (fElement instanceof IJavaStackFrame) {
			IJavaStackFrame stackFrame = (IJavaStackFrame)fElement;
			IJavaObject bundleData = getBundleData(stackFrame);
			if (bundleData != null) {
				String typeName = generateSourceName(stackFrame.getDeclaringTypeName());
				fResult = getSourceElement(bundleData, typeName);
			}
		}
	}
	
	protected Object getResult() {
		return fResult;
	}
	
	private IJavaObject getBundleData(IJavaStackFrame stackFrame) throws DebugException {
		IJavaObject object = stackFrame.getReferenceType().getClassLoaderObject();
		if (object == null)
			return null;
		IJavaClassType type = (IJavaClassType)object.getJavaType();
		if (ECLIPSE_CLASSLOADER.equals(type.getName())) {
			IJavaFieldVariable variable = object.getField("hostdata", true); //$NON-NLS-1$
			if (variable != null) {
				IValue value = variable.getValue();
				if (value instanceof IJavaObject)
					return (IJavaObject)value;
			}
		}
		return null;
	}
	
	private String getValue(IJavaObject object, String variable) throws DebugException {
		IJavaFieldVariable var = object.getField(variable, false);
		return var == null ? null : var.getValue().getValueString();
	}
	
	private Object getSourceElement(IJavaObject bundleData, String elementName) throws CoreException {
		String location = getValue(bundleData, "fileName"); //$NON-NLS-1$
		String id = getValue(bundleData, "symbolicName"); //$NON-NLS-1$
		if (location != null && id != null) {
			Object result = findSourceElement(getSourceContainers(location, id), elementName);
			if (result != null)
				return result;
			
			// don't give up yet, search fragments attached to this host
			State state = TargetPlatform.getState();
			BundleDescription desc = state.getBundle(id, null);
			if (desc != null) {
				BundleDescription[] fragments = desc.getFragments();
				for (int i = 0; i < fragments.length; i++) {
					location = fragments[i].getLocation();
					id = fragments[i].getSymbolicName();
					result = findSourceElement(getSourceContainers(location, id), elementName);
					if (result != null)
						return result;
				}
			}
		}
		return null;
	}
	
	private Object findSourceElement(ISourceContainer[] containers, String elementName) throws CoreException {
		for (int i = 0; i < containers.length; i++) {
			Object[] result = containers[i].findSourceElements(elementName);
			if (result.length > 0)
				return result[0];
		}
		return null;
	}
	
	protected ISourceContainer[] getSourceContainers(String location, String id) throws CoreException {
		ArrayList result = new ArrayList();		
		ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(id);
		IPluginModelBase model = entry.getWorkspaceModel();
		if (isPerfectMatch(model, new Path(location))) {
			IResource resource = model.getUnderlyingResource();
			// if the plug-in matches a workspace model,
			// add the project and any libraries not coming via a container
			// to the list of source containers, in that order
			if (resource != null) {
				addProjectSourceContainers(resource.getProject(), result);
			}
		} else {
			File file = new File(location);
			if (file.isFile()) {
				// in case of linked plug-in projects that map to an external JARd plug-in,
				// use source container that maps to the library in the linked project.
				ISourceContainer container = getArchiveSourceContainer(location);
				if (container != null)
					return new ISourceContainer[] {container};				
			} 
			model = entry.getExternalModel();
			if (isPerfectMatch(model, new Path(location))) {
				// try all source zips found in the source code locations
				IClasspathEntry[] entries = PDEClasspathContainer.getExternalEntries(model);
				for (int i = 0; i < entries.length; i++) {
					IRuntimeClasspathEntry rte = convertClasspathEntry(entries[i]);
					if (rte != null)
						result.add(rte);
				}
			}
		}
		
		IRuntimeClasspathEntry[] entries = (IRuntimeClasspathEntry[])
			 				result.toArray(new IRuntimeClasspathEntry[result.size()]);
		return JavaRuntime.getSourceContainers(entries);
	}
	
	private void addProjectSourceContainers(IProject project, ArrayList result) throws CoreException {
		if (project == null || !project.hasNature(JavaCore.NATURE_ID))
			return;
		
		IJavaProject jProject = JavaCore.create(project);
		result.add(JavaRuntime.newProjectRuntimeClasspathEntry(jProject));
		
		IClasspathEntry[] entries = jProject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				IRuntimeClasspathEntry rte = convertClasspathEntry(entry);
				if (rte != null)
					result.add(rte);
			}
		}	
	}
	
	private IRuntimeClasspathEntry convertClasspathEntry(IClasspathEntry entry) {
		if (entry == null)
			return null;
		
		IPath srcPath = entry.getSourceAttachmentPath();
		if (srcPath != null && srcPath.segmentCount() > 0) {
			IRuntimeClasspathEntry rte = 
				JavaRuntime.newArchiveRuntimeClasspathEntry(entry.getPath());
			rte.setSourceAttachmentPath(srcPath);
			rte.setSourceAttachmentRootPath(entry.getSourceAttachmentRootPath());
			return rte;
		}
		return null;
	}
	
	private boolean isPerfectMatch(IPluginModelBase model, IPath path) {
		if (model != null) {
			IPath path2 = new Path(model.getInstallLocation());
			return ExternalModelManager.arePathsEqual(path, path2);
		}
		return false;
	}
	
	private ISourceContainer getArchiveSourceContainer(String location) throws JavaModelException {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IFile[] containers = root.findFilesForLocation(new Path(location));
		for (int i = 0; i < containers.length; i++) {
			IJavaElement element = JavaCore.create(containers[i]);
			if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot archive = (IPackageFragmentRoot)element;
				IPath path = archive.getSourceAttachmentPath();
				if (path == null || path.segmentCount() == 0)
					continue;
				
				IPath rootPath = archive.getSourceAttachmentRootPath();
				boolean detectRootPath = rootPath != null && rootPath.segmentCount() > 0;
				
				IFile archiveFile = root.getFile(path);
				if (archiveFile.exists()) 
					return new ArchiveSourceContainer(archiveFile, detectRootPath);
				
				File file = path.toFile();
				if (file.exists()) 
					return new ExternalArchiveSourceContainer(file.getAbsolutePath(), detectRootPath);		
			}
		}
		return null;
	}
	/**
	 * Generates and returns a source file path based on a qualified type name.
	 * For example, when <code>java.lang.String</code> is provided,
	 * the returned source name is <code>java/lang/String.java</code>.
	 * 
	 * @param qualifiedTypeName fully qualified type name that may contain inner types
	 *  denoted with <code>$</code> character
	 * @return a source file path corresponding to the type name
	 */
	private static String generateSourceName(String qualifiedTypeName) {
		int index = qualifiedTypeName.indexOf('$');
		if (index >= 0) 
			qualifiedTypeName = qualifiedTypeName.substring(0, index);	
		return qualifiedTypeName.replace('.', File.separatorChar) + ".java"; //$NON-NLS-1$
	}      

}