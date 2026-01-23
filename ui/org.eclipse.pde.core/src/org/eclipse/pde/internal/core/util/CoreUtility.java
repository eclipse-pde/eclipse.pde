/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 525701
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.FactoryConfigurationError;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.core.PDECore;

public class CoreUtility {

	/**
	 * Read a file from an InputStream and write it to the file system.
	 *
	 * @param in InputStream from which to read.
	 * @param file output file to create.
	 * @exception IOException
	 */
	public static void readFile(InputStream in, File file) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			byte buffer[] = new byte[1024];
			int count;
			try {
				while ((count = in.read(buffer, 0, buffer.length)) > 0) {
					fos.write(buffer, 0, count);
				}
			} finally {
				in.close();
				in = null;
			}
		} catch (IOException e) {
			PDECore.logException(e);
			throw e;
		}
	}

	public static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		if (proj.hasNature(natureId)) {
			return;
		}
		IProjectDescription description = proj.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
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

	public static void createProject(IProject project, IPath location, IProgressMonitor monitor) throws CoreException {
		if (!Platform.getLocation().equals(location)) {
			IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
			desc.setLocation(location);
			project.create(desc, monitor);
		} else {
			project.create(monitor);
		}
	}

	public static String normalize(String text) {
		if (text == null || text.trim().length() == 0) {
			return ""; //$NON-NLS-1$
		}

		text = text.replaceAll("\\r|\\n|\\f|\\t", " "); //$NON-NLS-1$ //$NON-NLS-2$
		return text;
	}

	/**
	 * Convenience method to delete the given file and any content (if the file is a
	 * directory). Equivalent to calling {@link #deleteContent(File, IProgressMonitor)}
	 * with <code>null</code> as the monitor.  This operation cannot be undone.
	 *
	 * @param fileToDelete the file to delete
	 */
	public static void deleteContent(File fileToDelete) {
		deleteContent(fileToDelete, null);
	}

	/**
	 * Deletes the given file and any content (if the file is a directory).  There is
	 * no way to undo this action. Providing a progress monitor allows for cancellation.
	 *
	 * @param fileToDelete the file to delete
	 * @param monitor progress monitor for reporting and cancellation, can be <code>null</code>
	 */
	public static void deleteContent(File fileToDelete, IProgressMonitor monitor) {
		DeleteContentWalker.deleteDirectory(fileToDelete.toPath(), monitor);
	}

	public static boolean jarContainsResource(File file, String resource, boolean directory) {
		try (ZipFile jarFile = new ZipFile(file, ZipFile.OPEN_READ);) {
			ZipEntry resourceEntry = jarFile.getEntry(resource);
			if (resourceEntry != null) {
				return directory ? resourceEntry.isDirectory() : true;
			}
		} catch (IOException | FactoryConfigurationError e) {
			PDECore.logException(e);
		}
		return false;
	}

	public static void copyFile(IPath originPath, String name, File target) {
		File source = new File(originPath.toFile(), name);
		if (source.exists() == false) {
			return;
		}
		try (FileInputStream is = new FileInputStream(source); FileOutputStream os = new FileOutputStream(target)) {
			byte[] buf = new byte[1024];
			int len = is.read(buf);
			while (len != -1) {
				os.write(buf, 0, len);
				len = is.read(buf);
			}
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	public static org.eclipse.jface.text.Document getTextDocument(File bundleLocation, String path) {
		try {
			String extension = IPath.fromOSString(bundleLocation.getName()).getFileExtension();
			if ("jar".equals(extension) && bundleLocation.isFile()) { //$NON-NLS-1$
				try (ZipFile jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ)) {
					ZipEntry manifestEntry = jarFile.getEntry(path);
					if (manifestEntry != null) {
						InputStream stream = jarFile.getInputStream(manifestEntry);
						return getTextDocument(stream);
					}
				}
			} else {
				File file = new File(bundleLocation, path);
				if (file.exists()) {
					try (InputStream stream = new FileInputStream(file)) {
						return getTextDocument(stream);
					}
				}
			}
		} catch (IOException e) {
			PDECore.logException(e);
		}
		return null;
	}

	public static org.eclipse.jface.text.Document getTextDocument(InputStream in) {
		String result = null;
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();) {
			byte buffer[] = new byte[1024];
			int count;
			while ((count = in.read(buffer, 0, buffer.length)) > 0) {
				output.write(buffer, 0, count);
			}

			result = output.toString("UTF-8"); //$NON-NLS-1$
			in.close();
			in = null;
		} catch (IOException e) {
			// close open streams
			try {
				in.close();
			} catch (IOException ee) {
				PDECore.logException(ee);
			}
		}
		return result == null ? null : new org.eclipse.jface.text.Document(result);
	}

}
