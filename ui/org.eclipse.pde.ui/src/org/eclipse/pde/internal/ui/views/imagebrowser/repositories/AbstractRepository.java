/*******************************************************************************
 *  Copyright (c) 2012, 2014 Christian Pontesegger and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.repositories;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.views.imagebrowser.IImageTarget;
import org.eclipse.pde.internal.ui.views.imagebrowser.ImageElement;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;

public abstract class AbstractRepository extends Job {

	protected List<ImageElement> mElementsCache = new LinkedList<>();

	private IImageTarget mTarget;

	public AbstractRepository(IImageTarget target) {
		super(PDEUIMessages.AbstractRepository_ScanForUI);

		mTarget = target;
	}

	private static final String[] KNOWN_EXTENSIONS = new String[] {".gif", ".png"}; //$NON-NLS-1$ //$NON-NLS-2$

	@Override
	protected synchronized IStatus run(IProgressMonitor monitor) {
		while ((mTarget.needsMore()) && (!monitor.isCanceled())) {
			if (mElementsCache.isEmpty()) {
				// need more images in cache

				if (!populateCache(monitor)) {
					// could not populate cache, giving up
					return Status.OK_STATUS;
				}
			} else {
				// return 1 image from cache
				mTarget.notifyImage(mElementsCache.remove(0));
			}
		}

		return Status.OK_STATUS;
	}

	public synchronized void clearCache() {
		mElementsCache.clear();
	}

	protected abstract boolean populateCache(IProgressMonitor monitor);

	protected ImageData createImageData(final IFile file) throws CoreException {
		try (InputStream s = new BufferedInputStream(file.getContents())) {
			return new ImageData(s);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
					"Failed to close stream on: " + file.getLocation(), e)); //$NON-NLS-1$
		}
	}

	protected boolean isImage(final File resource) {
		if (resource.isFile())
			return isImageName(resource.getName());

		return false;
	}

	protected boolean isImageName(final String fileName) {
		for (String extension : KNOWN_EXTENSIONS) {
			if (fileName.regionMatches(true, fileName.length() - extension.length(), extension, 0, extension.length()))
				return true;
		}

		return false;
	}

	protected boolean isJar(final File file) {
		return file.getName().toLowerCase().endsWith(".jar"); //$NON-NLS-1$
	}

	protected void searchJarFile(final File jarFile, final IProgressMonitor monitor) {
		try (ZipFile zipFile = new ZipFile(jarFile)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while ((entries.hasMoreElements()) && (!monitor.isCanceled())) {
				ZipEntry entry = entries.nextElement();
				if (isImageName(entry.getName())) {
					try (InputStream inputStream = zipFile.getInputStream(entry)) {
						ImageData imageData = new ImageData(inputStream);
						addImageElement(new ImageElement(imageData, jarFile.getName(), entry.getName()));
					} catch (IOException e) {
						PDEPlugin.log(e);
					} catch (SWTException e) {
						// invalid image format
						PDEPlugin.log(new Status(IStatus.ERROR, PDEPlugin.getPluginId(), NLS.bind(PDEUIMessages.AbstractRepository_ErrorLoadingImageFromJar, jarFile.getAbsolutePath(), entry.getName()), e));
					}
				}
			}
		} catch (ZipException e) {
			PDEPlugin.log(e);
		} catch (IOException e) {
			PDEPlugin.log(e);
		}
	}

	protected void searchDirectory(File directory, final IProgressMonitor monitor) {
		File manifest = new File(directory, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
		if (manifest.exists()) {
			try {
				String pluginName = getPluginName(new FileInputStream(manifest));
				int directoryPathLength = directory.getAbsolutePath().length();

				Collection<File> locations = new HashSet<>();
				locations.add(directory);
				do {
					File next = locations.iterator().next();
					locations.remove(next);

					for (File resource : next.listFiles()) {
						if (monitor.isCanceled())
							return;

						if (resource.isDirectory()) {
							locations.add(resource);

						} else {
							try {
								if (isImage(resource)) {
									addImageElement(new ImageElement(createImageData((IFile) resource), pluginName, resource.getAbsolutePath().substring(directoryPathLength)));
								}

							} catch (Exception e) {
								// could not create image for location
							}
						}
					}

				} while ((!locations.isEmpty()) && (!monitor.isCanceled()));
			} catch (IOException e) {
				// could not read manifest
				PDEPlugin.log(e);
			}
		}
	}

	protected String getPluginName(final InputStream manifest) throws IOException {
		Properties properties = new Properties();
		try (BufferedInputStream stream = new BufferedInputStream(manifest)) {
			properties.load(stream);
		}
		String property = properties.getProperty("Bundle-SymbolicName"); //$NON-NLS-1$
		if (property.contains(";")) //$NON-NLS-1$
			return property.substring(0, property.indexOf(';')).trim();

		return property.trim();
	}

	protected void addImageElement(ImageElement element) {
		mElementsCache.add(element);
	}
}
