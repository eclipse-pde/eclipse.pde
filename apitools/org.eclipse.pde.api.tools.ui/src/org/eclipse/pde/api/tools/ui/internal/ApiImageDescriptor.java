/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.Point;

/**
 * A main icon and several adornments. The adornments are computed according to
 * flags set on creation of the descriptor.
 */
public class ApiImageDescriptor extends CompositeImageDescriptor {

	/** Flag to render the error (red x) adornment */
	public final static int ERROR = 0x0001;
	/** Flag to render the success (green check mark) adornment */
	public final static int SUCCESS = 0x0002;

	private ImageDescriptor fBaseImage;
	private int fFlags;
	private Point fSize;

	/**
	 * Create a new composite image.
	 *
	 * @param baseImage an image descriptor used as the base image
	 * @param flags flags indicating which adornments are to be rendered
	 */
	public ApiImageDescriptor(ImageDescriptor baseImage, int flags) {
		setBaseImage(baseImage);
		setFlags(flags);
	}

	/**
	 * @see CompositeImageDescriptor#getSize()
	 */
	@Override
	protected Point getSize() {
		if (fSize == null) {
			CachedImageDataProvider data = createCachedImageDataProvider(getBaseImage());
			setSize(new Point(data.getWidth(), data.getHeight()));
		}
		return fSize;
	}

	/**
	 * @see Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof ApiImageDescriptor)) {
			return false;
		}

		ApiImageDescriptor other = (ApiImageDescriptor) object;
		return (getBaseImage().equals(other.getBaseImage()) && getFlags() == other.getFlags());
	}

	/**
	 * @see Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getBaseImage().hashCode() | getFlags();
	}

	/**
	 * @see CompositeImageDescriptor#drawCompositeImage(int, int)
	 */
	@Override
	protected void drawCompositeImage(int width, int height) {
		ImageDataProvider bg = createCachedImageDataProvider(getBaseImage());
		drawImage(bg, 0, 0);
		drawOverlays();
	}

	/**
	 * Add any overlays to the image as specified in the flags.
	 */
	protected void drawOverlays() {
		int flags = getFlags();
		String imageDescriptorKey;
		if ((flags & ERROR) != 0) {
			imageDescriptorKey = IApiToolsConstants.IMG_OVR_ERROR;
		} else if ((flags & SUCCESS) != 0) {
			imageDescriptorKey = IApiToolsConstants.IMG_OVR_SUCCESS;
		} else {
			return;
		}
		ImageDescriptor imageDescriptor = ApiUIPlugin.getImageDescriptor(imageDescriptorKey);
		CachedImageDataProvider data = createCachedImageDataProvider(imageDescriptor);
		drawImage(data, 0, getSize().y - data.getHeight());
	}

	protected ImageDescriptor getBaseImage() {
		return fBaseImage;
	}

	protected void setBaseImage(ImageDescriptor baseImage) {
		fBaseImage = baseImage;
	}

	protected int getFlags() {
		return fFlags;
	}

	protected void setFlags(int flags) {
		fFlags = flags;
	}

	protected void setSize(Point size) {
		fSize = size;
	}
}
