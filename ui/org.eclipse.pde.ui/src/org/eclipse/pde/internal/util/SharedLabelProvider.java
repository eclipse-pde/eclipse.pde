/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.util;

import org.eclipse.jface.viewers.*;
import java.util.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.pde.internal.preferences.MainPreferencePage;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.PDEPluginImages;
import org.eclipse.core.runtime.IPluginDescriptor;
import java.net.*;

/**
 * @version 	1.0
 * @author
 */
public class SharedLabelProvider
	extends LabelProvider
	implements ITableLabelProvider {
	public static final int F_ERROR = 0x1;
	public static final int F_WARNING = 0x2;
	public static final int F_EXPORT = 0x4;
	public static final int F_EDIT = 0x8;
	Hashtable images = new Hashtable();
	ArrayList consumers = new ArrayList();

	public SharedLabelProvider() {

	}
	public void connect(Object consumer) {
		if (!consumers.contains(consumer))
			consumers.add(consumer);
	}
	public void disconnect(Object consumer) {
		consumers.remove(consumer);
		if (consumers.size() == 0) {
			reset();
		}
	}
	private void reset() {
		for (Enumeration enum = images.elements(); enum.hasMoreElements();) {
			Image image = (Image) enum.nextElement();
			image.dispose();
		}
		images.clear();
	}

	public Image get(ImageDescriptor desc) {
		return get(desc, 0);
	}

	public Image get(ImageDescriptor desc, int flags) {
		Object key = desc;

		if (flags != 0) {
			key = ("" + desc.hashCode()) + ":" + flags;
		}
		Image image = (Image) images.get(key);
		if (image == null) {
			image = createImage(desc, flags);
			images.put(key, image);
		}
		return image;
	}

	private Image createImage(ImageDescriptor baseDesc, int flags) {
		if (flags == 0) {
			return baseDesc.createImage();
		}
		ImageDescriptor[] lowerLeft = getLowerLeftOverlays(flags);
		ImageDescriptor[] upperRight = getUpperRightOverlays(flags);
		OverlayIcon compDesc =
			new OverlayIcon(
				baseDesc,
				new ImageDescriptor[][] { upperRight, null, lowerLeft, null });
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
		return null;
	}

	public String getColumnText(Object obj, int index) {
		return getText(obj);
	}
	public Image getColumnImage(Object obj, int index) {
		return getImage(obj);
	}

	public Image getImageFromPlugin(
		IPluginDescriptor pluginDescriptor,
		String subdirectoryAndFilename) {
		URL installURL = pluginDescriptor.getInstallURL();
		return getImageFromURL(installURL, subdirectoryAndFilename);
	}

	public Image getImageFromURL(
		URL installURL,
		String subdirectoryAndFilename) {
		Image image = null;
		try {
			URL newURL = new URL(installURL, subdirectoryAndFilename);
			String key = newURL.toString();
			image = (Image)images.get(key);
			if (image == null) {
				ImageDescriptor desc = ImageDescriptor.createFromURL(newURL);
				image = desc.createImage();
				images.put(key, image);
			}
		} catch (MalformedURLException e) {
		}
		return image;
	}
}