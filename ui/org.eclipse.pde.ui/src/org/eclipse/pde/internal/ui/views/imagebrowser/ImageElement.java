/*******************************************************************************
 *  Copyright (c) 2012, 2024 Christian Pontesegger and others.
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

package org.eclipse.pde.internal.ui.views.imagebrowser;

import java.lang.ref.SoftReference;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.swt.graphics.ImageData;

public class ImageElement {

	private static final Pattern PLUGIN_PATTERN = Pattern.compile("([a-zA-Z0-9]+\\.[a-zA-Z0-9\\.]+)_.+"); //$NON-NLS-1$
	private SoftReference<ImageData> mImageData;
	private final Callable<ImageData> mImageProvider;
	private final String mPlugin;
	private final String mPath;

	public ImageElement(final Callable<ImageData> imageProvider, final String plugin, final String path) {
		mImageData = new SoftReference<>(null);
		mImageProvider = imageProvider;
		mPlugin = plugin;
		mPath = path;
	}

	public String getFullPlugin() {
		return mPlugin;
	}

	public String getPlugin() {
		Matcher matcher = PLUGIN_PATTERN.matcher(getFullPlugin());
		if (matcher.matches()) {
			if (matcher.groupCount() > 0)
				return matcher.group(1);
		}

		return getFullPlugin();
	}

	public String getPath() {
		return mPath;
	}

	public ImageData getImageData() {
		ImageData imageData = mImageData.get();
		if (imageData == null) {
			try {
				imageData = mImageProvider.call();
			} catch (Exception e) {
				// could not create image for location
				PDECore.log(e);
				imageData = ImageDescriptor.getMissingImageDescriptor().getImageData(100);
			}
			mImageData = new SoftReference<>(imageData);
		}
		return imageData;
	}

	public String getFileName() {
		return IPath.fromOSString(mPath).lastSegment();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mPath == null) ? 0 : mPath.hashCode());
		result = prime * result + ((mPlugin == null) ? 0 : mPlugin.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageElement other = (ImageElement) obj;
		if (mPath == null) {
			if (other.mPath != null)
				return false;
		} else if (!mPath.equals(other.mPath))
			return false;
		if (mPlugin == null) {
			if (other.mPlugin != null)
				return false;
		} else if (!mPlugin.equals(other.mPlugin))
			return false;
		return true;
	}
}
