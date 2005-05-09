/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

public abstract class JarImportOperation implements IWorkspaceRunnable {

	protected void extractZipFile(File file, IPath destPath, IProgressMonitor monitor)
			throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			importContent(provider.getRoot(), destPath, provider, null, monitor);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
					IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
		}
	}

	protected void importContent(Object source, IPath destPath,
			IImportStructureProvider provider, List filesToImport,
			IProgressMonitor monitor) throws CoreException {
		IOverwriteQuery query = new IOverwriteQuery() {
			public String queryOverwrite(String file) {
				return ALL;
			}
		};
		try {
			ImportOperation op = new ImportOperation(destPath, source, provider, query);
			op.setCreateContainerStructure(false);
			if (filesToImport != null) {
				op.setFilesToImport(filesToImport);
			}
			op.run(monitor);
		} catch (InvocationTargetException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
					IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} catch (InterruptedException e) {
			throw new OperationCanceledException(e.getMessage());
		}
	}

	protected void extractResources(File file, IResource dest, IProgressMonitor monitor) throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			ArrayList collected = new ArrayList();
			collectResources(provider, provider.getRoot(), true, collected);
			importContent(provider.getRoot(), dest.getFullPath(), provider, collected,
					monitor);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
					IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	protected void extractJavaResources(File file, IResource dest, IProgressMonitor monitor) throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			ArrayList collected = new ArrayList();
			collectJavaResources(provider, provider.getRoot(), collected);
			importContent(provider.getRoot(), dest.getFullPath(), provider, collected,
					monitor);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
					IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
		}
	}

	protected void importArchive(IProject project, File archive, IPath destPath)
			throws CoreException {
		try {
			if (destPath.segmentCount() > 2)
				CoreUtility.createFolder(project.getFolder(destPath.removeLastSegments(1)));
			IFile file = project.getFile(destPath);
			FileInputStream fstream = new FileInputStream(archive);
			if (file.exists())
				file.setContents(fstream, true, false, null);
			else
				file.create(fstream, true, null);
			fstream.close();
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
					IStatus.OK, e.getMessage(), e);
			throw new CoreException(status);
		}
	}

	private void collectResources(ZipFileStructureProvider provider, Object element, boolean excludeMeta, ArrayList collected) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					if (!excludeMeta || !provider.getLabel(curr).equals("META-INF")) { //$NON-NLS-1$
						collectResources(provider, curr, excludeMeta, collected);
					}
				} else if (!provider.getLabel(curr).endsWith(".class")) { //$NON-NLS-1$
					collected.add(curr);
				}
			}
		}
	}

	protected void collectNonJavaResources(ZipFileStructureProvider provider, Object element, ArrayList collected) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					if (!provider.getLabel(curr).equals("src") && !isClassFolder(provider, curr)) { //$NON-NLS-1$
						ArrayList list = new ArrayList();
						collectResources(provider, curr, false, list);
						collected.addAll(list);
					}
				} else if (!provider.getLabel(curr).endsWith(".class")) { //$NON-NLS-1$
					collected.add(curr);
				}
			}
		}
	}
	
	protected void collectJavaFiles(ZipFileStructureProvider provider, Object element, ArrayList collected) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					if (provider.getLabel(curr).equals("src")) { //$NON-NLS-1$
						ArrayList list = new ArrayList();
						collectResources(provider, curr, false, list);
						collected.addAll(list);
					}
				}
			}
		}
	}
	
	protected void collectJavaResources(ZipFileStructureProvider provider, Object element, ArrayList collected) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					if (isClassFolder(provider, curr)) {
						ArrayList list = new ArrayList();
						collectResources(provider, curr, false, list);
						collected.addAll(list);
					}
				}
			}
		}
	}
	
	private boolean isClassFolder(ZipFileStructureProvider provider, Object element) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					if (isClassFolder(provider, curr)) {
						return true;
					}
				} else if (provider.getLabel(curr).endsWith(".class")) { //$NON-NLS-1$
					return true;
				} 
			}
		}
		return false;
	}
	
	protected boolean hasEmbeddedSource(ZipFileStructureProvider provider) {
		List children = provider.getChildren(provider.getRoot());
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr) && provider.getLabel(curr).equals("src")) { //$NON-NLS-1$
					return true;
				}
			}
		}
		return false;
	}
	
	protected boolean containsCode(ZipFileStructureProvider provider) {
		List children = provider.getChildren(provider.getRoot());
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr) && isClassFolder(provider, curr)) {
					return true;
				}
			}
		}
		return false;
	}
	
	protected boolean containsCode(File file) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			return containsCode(new ZipFileStructureProvider(zipFile));
		} catch (IOException e) {
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
		}
		return true;
	}
	
	protected String[] getTopLevelResources(File file) {
		ArrayList result = new ArrayList();
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			List children = provider.getChildren(provider.getRoot());
			if (children != null && !children.isEmpty()) {
				for (int i = 0; i < children.size(); i++) {
					Object curr = children.get(i);
					if (provider.isFolder(curr)) {
						if (!isClassFolder(provider, curr)) 
							result.add(provider.getLabel(curr) + "/"); //$NON-NLS-1$
						else {
							if (!result.contains(".")) //$NON-NLS-1$
								result.add("."); //$NON-NLS-1$
						}
					} else {
						result.add(provider.getLabel(curr));
					}
				}
			}			
		} catch (IOException e) {
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
		}	
		return (String[])result.toArray(new String[result.size()]);
	}
	
}
