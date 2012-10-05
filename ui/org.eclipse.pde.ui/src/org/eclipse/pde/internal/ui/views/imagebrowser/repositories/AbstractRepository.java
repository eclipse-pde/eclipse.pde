/*******************************************************************************
 *  Copyright (c) 2012 Christian Pontesegger and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.repositories;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.views.imagebrowser.IImageTarget;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;

public abstract class AbstractRepository {

	private static final String[] KNOWN_EXTENSIONS = new String[] {".gif", ".png"}; //$NON-NLS-1$ //$NON-NLS-2$

	public abstract IStatus searchImages(IImageTarget target, IProgressMonitor monitor);

	protected ImageData createImageData(final File file) throws FileNotFoundException {
		return new ImageData(new BufferedInputStream(new FileInputStream(file)));
	}

	protected ImageData createImageData(final IFile file) throws CoreException {
		return new ImageData(new BufferedInputStream(file.getContents()));
	}

	protected boolean isImage(final File resource) {
		if (resource.isFile())
			return isImageName(resource.getName().toLowerCase());

		return false;
	}

	protected boolean isImageName(final String fileName) {
		for (String extension : KNOWN_EXTENSIONS) {
			if (fileName.endsWith(extension))
				return true;
		}

		return false;
	}

	protected boolean isJar(final File file) {
		return file.getName().toLowerCase().endsWith(".jar"); //$NON-NLS-1$
	}

	protected void searchJarFile(final File jarFile, final IImageTarget target, final IProgressMonitor monitor) {
		try {
			ZipFile zipFile = new ZipFile(jarFile);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while ((entries.hasMoreElements()) && (!monitor.isCanceled())) {
				ZipEntry entry = entries.nextElement();
				if (isImageName(entry.getName().toLowerCase())) {
					try {
						ImageData imageData = new ImageData(zipFile.getInputStream(entry));
						target.notifyImage(imageData, jarFile.getName(), entry.getName());
					} catch (IOException e) {
						PDEPlugin.log(e);
					} catch (SWTException e) {
						// could not create image
						PDEPlugin.log(e);
					}
				}
			}
		} catch (ZipException e) {
			PDEPlugin.log(e);
		} catch (IOException e) {
			PDEPlugin.log(e);
		}
	}
}
