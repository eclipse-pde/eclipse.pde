/*******************************************************************************
 *  Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 549441
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

public class SharedLabelProvider extends LabelProvider implements ITableLabelProvider {
	public static final int F_ERROR = 1;
	public static final int F_WARNING = 2;
	public static final int F_EXPORT = 4;
	public static final int F_EDIT = 8;
	public static final int F_BINARY = 16;
	public static final int F_EXTERNAL = 32;
	public static final int F_JAVA = 64;
	public static final int F_JAR = 128;
	public static final int F_PROJECT = 256;
	public static final int F_OPTIONAL = 512;
	public static final int F_INTERNAL = 1024;
	public static final int F_FRIEND = 2048;
	Hashtable<Object, Image> images = new Hashtable<>();
	ArrayList<Object> consumers = new ArrayList<>();
	private Image fBlankImage;

	public SharedLabelProvider() {

	}

	public void connect(Object consumer) {
		if (!consumers.contains(consumer))
			consumers.add(consumer);
	}

	public void disconnect(Object consumer) {
		consumers.remove(consumer);
		if (consumers.isEmpty()) {
			dispose();
		}
	}

	@Override
	public void dispose() {
		if (consumers.isEmpty()) {
			for (Enumeration<Image> elements = images.elements(); elements.hasMoreElements();) {
				elements.nextElement().dispose();
			}
			images.clear();
			if (fBlankImage != null) {
				fBlankImage.dispose();
				fBlankImage = null;
			}
		}
	}

	public Image get(ImageDescriptor desc) {
		return get(desc, 0);
	}

	public Image get(ImageDescriptor desc, int flags) {
		Object key = desc;

		if (flags != 0) {
			key = getKey(desc.hashCode(), flags);
		}
		Image image = images.get(key);
		if (image == null) {
			image = createImage(desc, flags);
			images.put(key, image);
		}
		return image;
	}

	public Image get(Image image, int flags) {
		if (flags == 0)
			return image;
		String key = getKey(image.hashCode(), flags);
		Image resultImage = images.get(key);
		if (resultImage == null) {
			resultImage = createImage(image, flags);
			images.put(key, resultImage);
		}
		return resultImage;
	}

	private String getKey(long hashCode, int flags) {
		return ("" + hashCode) + ":" + flags; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Image createImage(ImageDescriptor baseDesc, int flags) {
		if (flags == 0) {
			return baseDesc.createImage();
		}
		ImageDescriptor[] lowerLeft = getLowerLeftOverlays(flags);
		ImageDescriptor[] upperRight = getUpperRightOverlays(flags);
		ImageDescriptor[] lowerRight = getLowerRightOverlays(flags);
		ImageDescriptor[] upperLeft = getUpperLeftOverlays(flags);
		OverlayIcon compDesc = new OverlayIcon(baseDesc, new ImageDescriptor[][] {upperRight, lowerRight, lowerLeft, upperLeft});
		return compDesc.createImage();
	}

	private Image createImage(Image baseImage, int flags) {
		if (flags == 0) {
			return baseImage;
		}
		ImageDescriptor[] lowerLeft = getLowerLeftOverlays(flags);
		ImageDescriptor[] upperRight = getUpperRightOverlays(flags);
		ImageDescriptor[] lowerRight = getLowerRightOverlays(flags);
		ImageDescriptor[] upperLeft = getUpperLeftOverlays(flags);
		ImageOverlayIcon compDesc = new ImageOverlayIcon(baseImage, new ImageDescriptor[][] {upperRight, lowerRight, lowerLeft, upperLeft});
		return compDesc.createImage();
	}

	private ImageDescriptor[] getLowerLeftOverlays(int flags) {
		if ((flags & F_ERROR) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_ERROR_CO};
		if ((flags & F_WARNING) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_WARNING_CO};
		return null;
	}

	private ImageDescriptor[] getUpperRightOverlays(int flags) {
		if ((flags & F_EXPORT) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_EXPORT_CO};
		if ((flags & F_EDIT) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_DOC_CO};
		if ((flags & F_JAVA) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_JAVA_CO};
		return null;
	}

	private ImageDescriptor[] getLowerRightOverlays(int flags) {
		if ((flags & F_JAR) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_JAR_CO};
		if ((flags & F_PROJECT) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_PROJECT_CO};
		if ((flags & F_OPTIONAL) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_OPTIONAL_CO};
		if ((flags & F_INTERNAL) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_INTERNAL_CO};
		if ((flags & F_FRIEND) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_FRIEND_CO};
		return null;
	}

	private ImageDescriptor[] getUpperLeftOverlays(int flags) {
		if ((flags & F_EXTERNAL) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_EXTERNAL_CO};
		if ((flags & F_BINARY) != 0)
			return new ImageDescriptor[] {PDEPluginImages.DESC_BINARY_CO};
		return null;
	}

	@Override
	public String getColumnText(Object obj, int index) {
		return getText(obj);
	}

	@Override
	public Image getColumnImage(Object obj, int index) {
		return getImage(obj);
	}

	public Image getImageFromPlugin(String bundleID, String path) {
		return ResourceLocator.imageDescriptorFromBundle(bundleID, path).map(this::get).orElse(getBlankImage());
	}

	public Image getImageFromPlugin(IPluginModelBase model, String relativePath) {
		String platform = "platform:/plugin/"; //$NON-NLS-1$
		if (relativePath.startsWith(platform)) {
			relativePath = relativePath.substring(platform.length());
			int index = relativePath.indexOf('/');
			if (index == -1)
				return null;
			model = PluginRegistry.findModel(relativePath.substring(0, index));
			if (model == null)
				return null;
			relativePath = relativePath.substring(index + 1);
		}

		String location = model.getInstallLocation();
		if (location == null)
			return null;

		File pluginLocation = new File(location);
		InputStream stream = null;
		ZipFile jarFile = null;
		try {
			if (pluginLocation.isDirectory()) {
				File file = new File(pluginLocation, relativePath);
				if (file.exists())
					stream = new FileInputStream(file);
				else if (relativePath.length() > 5 && relativePath.startsWith("$nl$/")) { //$NON-NLS-1$
					file = new File(pluginLocation, relativePath.substring(5));
					if (file.exists())
						stream = new FileInputStream(file);
				}
			} else {
				jarFile = new ZipFile(pluginLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(relativePath);
				if (manifestEntry != null) {
					stream = jarFile.getInputStream(manifestEntry);
				} else if (relativePath.length() > 5 && relativePath.startsWith("$nl$/")) { //$NON-NLS-1$
					manifestEntry = jarFile.getEntry(relativePath.substring(5));
					if (manifestEntry != null) {
						stream = jarFile.getInputStream(manifestEntry);
					}
				}
			}
			if (stream != null) {
				ImageDescriptor desc = ImageDescriptor.createFromImageData(new ImageData(stream));
				return get(desc);
			}
		} catch (IOException e) {
		} finally {
			try {
				if (stream != null)
					stream.close();
				if (jarFile != null)
					jarFile.close();
			} catch (IOException e) {
			}
		}
		return getBlankImage();
	}

	public Image getBlankImage() {
		if (fBlankImage == null)
			fBlankImage = ImageDescriptor.getMissingImageDescriptor().createImage();
		return fBlankImage;
	}
}
