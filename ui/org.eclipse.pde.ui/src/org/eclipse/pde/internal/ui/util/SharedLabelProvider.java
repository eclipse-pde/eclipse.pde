/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.jface.viewers.*;
import java.util.*;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.core.runtime.*;
import org.osgi.framework.*;

import java.io.*;
import java.net.*;

/**
 * @version 	1.0
 * @author
 */
public class SharedLabelProvider
	extends LabelProvider
	implements ITableLabelProvider {
	public static final int F_ERROR = 1;
	public static final int F_WARNING = 2;
	public static final int F_EXPORT = 4;
	public static final int F_EDIT = 8;
	public static final int F_BINARY = 16;
	public static final int F_EXTERNAL = 32;
	public static final int F_JAVA = 64;
	public static final int F_JAR = 128;
	public static final int F_PROJECT = 256;
	Hashtable images = new Hashtable();
	ArrayList consumers = new ArrayList();
	private Image fBlankImage;

	public SharedLabelProvider() {

	}
	public void connect(Object consumer) {
		if (!consumers.contains(consumer))
			consumers.add(consumer);
	}
	public void disconnect(Object consumer) {
		consumers.remove(consumer);
		if (consumers.size() == 0) {
			dispose();
		}
	}
	public void dispose() {
		if (consumers.size() == 0) {
			for (Enumeration elements = images.elements(); elements.hasMoreElements();) {
				((Image)elements.nextElement()).dispose();
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
		Image image = (Image) images.get(key);
		if (image == null) {
			image = createImage(desc, flags);
			images.put(key, image);
		}
		return image;
	}
	
	public Image get(Image image, int flags) {
		if (flags==0) return image;
		String key = getKey(image.hashCode(), flags);
		Image resultImage = (Image)images.get(key);
		if (resultImage == null) {
			resultImage = createImage(image, flags);
			images.put(key, resultImage);
		}
		return resultImage;
	}

	private String getKey(long hashCode, int flags) {
		return (""+hashCode) + ":"+flags; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Image createImage(ImageDescriptor baseDesc, int flags) {
		if (flags == 0) {
			return baseDesc.createImage();
		}
		ImageDescriptor[] lowerLeft = getLowerLeftOverlays(flags);
		ImageDescriptor[] upperRight = getUpperRightOverlays(flags);
		ImageDescriptor[] lowerRight = getLowerRightOverlays(flags);
		ImageDescriptor[] upperLeft = getUpperLeftOverlays(flags);
		OverlayIcon compDesc =
			new OverlayIcon(
				baseDesc,
				new ImageDescriptor[][] { upperRight, lowerRight, lowerLeft, upperLeft });
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
		ImageOverlayIcon compDesc =
			new ImageOverlayIcon(
				baseImage,
				new ImageDescriptor[][] { upperRight, lowerRight, lowerLeft, upperLeft });
		return compDesc.createImage();
	}

	private ImageDescriptor[] getLowerLeftOverlays(int flags) {
		if ((flags & F_ERROR) != 0)
			return new ImageDescriptor[] { PDEPluginImages.DESC_ERROR_CO };
		if ((flags & F_WARNING) != 0)
			return new ImageDescriptor[] { PDEPluginImages.DESC_WARNING_CO };
		return null;
	}

	private ImageDescriptor[] getUpperRightOverlays(int flags) {
		if ((flags & F_EXPORT) != 0)
			return new ImageDescriptor[] { PDEPluginImages.DESC_EXPORT_CO };
		if ((flags & F_EDIT) != 0)
			return new ImageDescriptor[] { PDEPluginImages.DESC_DOC_CO };
		if ((flags & F_JAVA) != 0)
			return new ImageDescriptor[] { PDEPluginImages.DESC_JAVA_CO };
		return null;
	}
	
	private ImageDescriptor[] getLowerRightOverlays(int flags) {
		if ((flags & F_JAR) != 0)
			return new ImageDescriptor[] { PDEPluginImages.DESC_JAR_CO };
		if ((flags & F_PROJECT) != 0)
			return new ImageDescriptor[] { PDEPluginImages.DESC_PROJECT_CO };
		return null;
	}
	
	private ImageDescriptor[] getUpperLeftOverlays(int flags) {
		if ((flags & F_EXTERNAL) != 0)
			return new ImageDescriptor[] { PDEPluginImages.DESC_EXTERNAL_CO };
		if ((flags & F_BINARY) != 0)
			return new ImageDescriptor[] { PDEPluginImages.DESC_BINARY_CO };
		return null;
	}

	public String getColumnText(Object obj, int index) {
		return getText(obj);
	}
	public Image getColumnImage(Object obj, int index) {
		return getImage(obj);
	}

	public Image getImageFromPlugin(String bundleID, String subdirectoryAndFilename) {
		try {
			Bundle bundle = Platform.getBundle(bundleID);
			return getImageFromURL(Platform.resolve(bundle.getEntry(subdirectoryAndFilename)));
		} catch (IOException e) {
			return null;
		}
	}

	public Image getImageFromURL(URL url) {
		if (url == null)
			return getBlankImage();
		Image image = null;
		try {
			InputStream stream = null;
			try {
				stream = url.openStream();
				stream.close();
			} catch (IOException e1) {	
				return getBlankImage();
			}
			
			String key = url.toString();
			image = (Image)images.get(key);
			if (image == null) {
				ImageDescriptor desc = ImageDescriptor.createFromURL(url);
				image = desc.createImage();
				images.put(key, image);
			}
		} catch (SWTException e) {
		}
		return image;
	}
	
	public Image getBlankImage() {
		if (fBlankImage == null)
			fBlankImage = ImageDescriptor.getMissingImageDescriptor().createImage();
		return fBlankImage;			
	}
}
