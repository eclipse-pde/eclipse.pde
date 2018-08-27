/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * Used to create images with (possible) adornments
 *
 * @since 1.0.1
 */
public class CompositeApiImageDescriptor extends CompositeImageDescriptor {

	public static final int ERROR = 0x0001;
	public static final int WARNING = 0x0002;

	private Image fOriginalImage = null;
	private int fFlags;
	private Point fSize;

	/**
	 * Constructor
	 *
	 * @param original
	 * @param flags
	 */
	public CompositeApiImageDescriptor(Image original, int flags) {
		fOriginalImage = original;
		fFlags = flags;
	}

	@Override
	protected void drawCompositeImage(int width, int height) {
		ImageData bg = fOriginalImage.getImageData();
		if (bg == null) {
			bg = DEFAULT_IMAGE_DATA;
		}
		drawImage(bg, 0, 0);
		drawOverlays();
	}

	/**
	 * Add any overlays to the image as specified in the flags.
	 */
	protected void drawOverlays() {
		drawTopRight();
	}

	/**
	 * Draws overlay images in the top right corner of the original image
	 */
	private void drawTopRight() {
		Point pos = new Point(getSize().x, 0);
		if ((fFlags & ERROR) != 0) {
			addTopRightImage(ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_OVR_ERROR), pos);
		} else if ((fFlags & WARNING) != 0) {
			addTopRightImage(ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_OVR_WARNING), pos);
		}
	}

	/**
	 * Adds the given {@link ImageDescriptor} to the upper right-hand corner of
	 * the original image
	 *
	 * @param desc
	 * @param pos
	 */
	private void addTopRightImage(ImageDescriptor desc, Point pos) {
		ImageData data = getImageData(desc);
		int x = pos.x - data.width;
		if (x >= 0) {
			drawImage(data, x, pos.y);
			pos.x = x;
		}
	}

	/**
	 * Returns the {@link ImageData} from the given {@link ImageDescriptor} or
	 * <code>null</code>
	 *
	 * @param descriptor
	 * @return the {@link ImageData} from the given {@link ImageDescriptor} or
	 *         <code>null</code>
	 */
	private ImageData getImageData(ImageDescriptor descriptor) {
		ImageData data = descriptor.getImageData();
		if (data == null) {
			data = DEFAULT_IMAGE_DATA;
		}
		return data;
	}

	@Override
	protected Point getSize() {
		if (fSize == null) {
			ImageData data = fOriginalImage.getImageData();
			fSize = new Point(data.width, data.height);
		}
		return fSize;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CompositeApiImageDescriptor) {
			CompositeApiImageDescriptor other = (CompositeApiImageDescriptor) obj;
			return (fOriginalImage.equals(other.fOriginalImage) && fFlags == other.fFlags);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fOriginalImage.hashCode() | fFlags;
	}
}
