/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.*;

/**
 * Helper class for the plugin import operation.  Contains methods to assist in the copying and extracting
 * of jar a folder files.
 */
public class PluginImportHelper {

	/**
	 * Imports the contents of a zip file or folder, extracting the necessary files and
	 * putting them in the specified destination.  
	 * @param source the file or folder to import from, should either be the root of the zip file or the File representing the folder
	 * @param dstPath
	 * @param provider
	 * @param filesToImport
	 * @param monitor
	 * @throws CoreException
	 */
	public static void importContent(Object source, IPath dstPath, IImportStructureProvider provider, List filesToImport, IProgressMonitor monitor) throws CoreException {
		IOverwriteQuery query = new IOverwriteQuery() {
			public String queryOverwrite(String file) {
				return ALL;
			}
		};
		try {
			ImportOperation op = new ImportOperation(dstPath, source, provider, query);
			op.setCreateContainerStructure(false);
			if (filesToImport != null) {
				op.setFilesToImport(filesToImport);
			}
			op.run(monitor);
		} catch (InvocationTargetException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} catch (InterruptedException e) {
			throw new OperationCanceledException(e.getMessage());
		}
	}

	/**
	 * Extracts the contents of the specified zip file to the specified destination
	 * @param file
	 * @param dstPath
	 * @param monitor
	 * @throws CoreException
	 */
	public static void extractArchive(File file, IPath dstPath, IProgressMonitor monitor) throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			importContent(provider.getRoot(), dstPath, provider, null, monitor);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
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

	/**
	 * Extracts all of the files and subfolders from a single folder within an archive file.
	 * @param file archive file to search for files
	 * @param folderPath path to the folder to extract from
	 * @param dstPath destination to import content to
	 * @param monitor progress monitor
	 * @throws CoreException if a problem occurs while extracting
	 * @since 3.4
	 */
	public static void extractFolderFromArchive(File file, IPath folderPath, IPath dstPath, IProgressMonitor monitor) throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			ArrayList collected = new ArrayList();
			collectResourcesFromFolder(provider, provider.getRoot(), folderPath, collected);
			importContent(provider.getRoot(), dstPath, provider, collected, monitor);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
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

	/**
	 * Searches the given archive file for java source folders.  Imports the files in the
	 * source folders to the specified destination unless the folder is in the list of 
	 * folders to exclude. 
	 * @param file archive file to search for source in
	 * @param excludeFolders list of IPaths describing folders to ignore while searching
	 * @param dstPath full path to destination to put the extracted source
	 * @param monitor progress monitor
	 * @throws CoreException if there is a problem extracting source from the zip
	 */
	public static void extractJavaSourceFromArchive(File file, List excludeFolders, IPath dstPath, IProgressMonitor monitor) throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			ArrayList collected = new ArrayList();
			collectJavaSourceFromRoot(provider, excludeFolders, collected);
			importContent(provider.getRoot(), dstPath, provider, collected, monitor);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
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

	/**
	 * Copies an archive file to an IFile
	 * @param file
	 * @param dstFile
	 * @throws CoreException
	 */
	public static void copyArchive(File file, IFile dstFile, IProgressMonitor monitor) throws CoreException {
		try {
			FileInputStream fstream = new FileInputStream(file);
			if (dstFile.exists())
				dstFile.setContents(fstream, true, false, monitor);
			else
				dstFile.create(fstream, true, monitor);
			fstream.close();
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, e.getMessage(), e);
			throw new CoreException(status);
		}
	}

	public static String[] getTopLevelResources(File file) {
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
		return (String[]) result.toArray(new String[result.size()]);
	}

	public static void collectRequiredBundleFiles(IImportStructureProvider provider, Object element, ArrayList collected) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				String name = provider.getLabel(curr);
				if (provider.isFolder(curr)) {
					if (!name.equals("src") && !isClassFolder(provider, curr)) { //$NON-NLS-1$
						ArrayList list = new ArrayList();
						collectResources(provider, curr, false, list);
						collected.addAll(list);
					}
				} else if (!name.endsWith(".class")) { //$NON-NLS-1$
					// Ignore the bundle signing files
					if (!(provider.getFullPath(curr).indexOf("META-INF/") != -1 && (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF")))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						collected.add(curr);
					}
				}

			}
		}
	}

	public static void collectNonJavaNonBuildFiles(IImportStructureProvider provider, Object element, ArrayList collected) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					// ignore source folders
					if (folderContainsFileExtension(provider, curr, ".java")) //$NON-NLS-1$
						continue;
					if (provider.getFullPath(curr).equals("META-INF/")) { //$NON-NLS-1$
						continue;
					}
					collected.add(curr);
				} else if (!provider.getLabel(curr).endsWith(".java")) { //$NON-NLS-1$
					collected.add(curr);
				}
			}
		}
	}

	private static void collectResources(IImportStructureProvider provider, Object element, boolean excludeMeta, ArrayList collected) {
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

	/**
	 * Recursively searches through the zip files searching for files inside of
	 * the specified folder.  The files found will be added to the given list.
	 * @param provider zip provider
	 * @param element element of the zip currently being looked at
	 * @param folderPath location of the folder to get resources from
	 * @param collected list of files found
	 * @since 3.4
	 */
	private static void collectResourcesFromFolder(ZipFileStructureProvider provider, Object element, IPath folderPath, ArrayList collected) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					if (provider.getLabel(curr).equals(folderPath.segment(0))) {
						if (folderPath.segmentCount() > 1) {
							collectResourcesFromFolder(provider, curr, folderPath.removeFirstSegments(1), collected);
						} else {
							collectResources(provider, curr, false, collected);
						}
					}
				}
			}
		}
	}

	/**
	 * Searches through the zip file for java source folders.  Collects the files
	 * within the source folders.  If a folder is in the list of folder paths to
	 * ignore, the folder will be skipped.
	 * @param provider zip provider
	 * @param ignoreFolders list of IPaths describing folders to ignore
	 * @param collected list that source files will be added to
	 * @since 3.4
	 */
	private static void collectJavaSourceFromRoot(ZipFileStructureProvider provider, List ignoreFolders, ArrayList collected) {
		List children = provider.getChildren(provider.getRoot());
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr) && folderContainsFileExtension(provider, curr, ".java")) { //$NON-NLS-1$
					// Check if we are in an ignored folder
					List ignoreSubFolders = new ArrayList();
					boolean ignoreThisChild = false;
					for (Iterator iterator = ignoreFolders.iterator(); iterator.hasNext();) {
						IPath currentPath = (IPath) iterator.next();
						if (provider.getLabel(curr).equals(currentPath.segment(0))) {
							if (currentPath.segmentCount() > 1) {
								// There is a subfolder that should be ignored
								ignoreSubFolders.add(currentPath.removeFirstSegments(1));
							} else {
								// This folder should be ignored
								ignoreThisChild = true;
								break;
							}
						}
					}
					if (!ignoreThisChild) {
						collectJavaSource(provider, curr, ignoreSubFolders, collected);
					}
				}
			}
		}
	}

	/**
	 * Recursively searches the children of the given element inside of a zip file. 
	 * If the folder path is in the set of folders to ignore, the folder will be skipped.
	 * All files found, except for .class files, will be added. The given list will be
	 * updated with the source files.
	 * 
	 * @param provider zip provider
	 * @param element current element inside the zip
	 * @param ignoreFolders list of IPath folder paths to skip while searching
	 * @param collected list to update with new files found to import
	 * @since 3.4
	 */
	private static void collectJavaSource(ZipFileStructureProvider provider, Object element, List ignoreFolders, ArrayList collected) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					// Check if we are in an ignored folder
					List ignoreSubFolders = new ArrayList();
					boolean ignoreThisChild = false;
					for (Iterator iterator = ignoreFolders.iterator(); iterator.hasNext();) {
						IPath currentPath = (IPath) iterator.next();
						if (provider.getLabel(curr).equals(currentPath.segment(0))) {
							if (currentPath.segmentCount() > 1) {
								// There is a subfolder that should be ignored.  Remove segment referencing current folder.
								ignoreSubFolders.add(currentPath.removeFirstSegments(1));
							} else {
								// This folder should be ignored
								ignoreThisChild = true;
								break;
							}
						}
					}
					if (!ignoreThisChild) {
						collectJavaSource(provider, curr, ignoreSubFolders, collected);
					}
					// Add the file to the list
				} else if (!provider.getLabel(curr).endsWith(".class")) { //$NON-NLS-1$
					collected.add(curr);
				}
			}
		}
	}

	private static boolean folderContainsFileExtension(IImportStructureProvider provider, Object element, String fileExtension) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					if (folderContainsFileExtension(provider, curr, fileExtension)) {
						return true;
					}
				} else if (provider.getLabel(curr).endsWith(fileExtension)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isClassFolder(IImportStructureProvider provider, Object element) {
		return folderContainsFileExtension(provider, element, ".class"); //$NON-NLS-1$
	}

}
