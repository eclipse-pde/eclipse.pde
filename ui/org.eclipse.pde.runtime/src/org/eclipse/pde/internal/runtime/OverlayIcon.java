/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.runtime;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.Point;

public class OverlayIcon extends CompositeImageDescriptor {

	static final int DEFAULT_WIDTH = 16;
	static final int DEFAULT_HEIGHT = 16;

	private Point fSize = null;

	private ImageDescriptor fBase;
	private final ImageDescriptor[][] fOverlays;

	public OverlayIcon(ImageDescriptor base, ImageDescriptor[][] overlays) {
		fBase = base;
		if (fBase == null)
			fBase = ImageDescriptor.getMissingImageDescriptor();
		fOverlays = overlays;
		fSize = new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public OverlayIcon(ImageDescriptor base, ImageDescriptor[][] overlays, Point size) {
		fBase = base;
		if (fBase == null)
			fBase = ImageDescriptor.getMissingImageDescriptor();
		fOverlays = overlays;
		fSize = size;
	}

	protected void drawBottomLeft(ImageDescriptor[] overlays) {
		if (overlays == null)
			return;
		int length = overlays.length;
		int x = 0;
		for (int i = 0; i < 3; i++) {
			if (i < length && overlays[i] != null) {
				CachedImageDataProvider id = createCachedImageDataProvider(overlays[i]);
				drawImage(id, x, getSize().y - id.getHeight());
				x += id.getWidth();
			}
		}
	}

	protected void drawBottomRight(ImageDescriptor[] overlays) {
		if (overlays == null)
			return;
		int length = overlays.length;
		int x = getSize().x;
		for (int i = 2; i >= 0; i--) {
			if (i < length && overlays[i] != null) {
				CachedImageDataProvider id = createCachedImageDataProvider(overlays[i]);
				x -= id.getWidth();
				drawImage(id, x, getSize().y - id.getHeight());
			}
		}
	}

	@Override
	protected void drawCompositeImage(int width, int height) {
		ImageDataProvider bg = createCachedImageDataProvider(fBase);
		drawImage(bg, 0, 0);

		if (fOverlays != null) {
			if (fOverlays.length > 0)
				drawTopRight(fOverlays[0]);

			if (fOverlays.length > 1)
				drawBottomRight(fOverlays[1]);

			if (fOverlays.length > 2)
				drawBottomLeft(fOverlays[2]);

			if (fOverlays.length > 3)
				drawTopLeft(fOverlays[3]);
		}
	}

	protected void drawTopLeft(ImageDescriptor[] overlays) {
		if (overlays == null)
			return;
		int length = overlays.length;
		int x = 0;
		for (int i = 0; i < 3; i++) {
			if (i < length && overlays[i] != null) {
				CachedImageDataProvider id = createCachedImageDataProvider(overlays[i]);
				drawImage(id, x, 0);
				x += id.getWidth();
			}
		}
	}

	protected void drawTopRight(ImageDescriptor[] overlays) {
		if (overlays == null)
			return;
		int length = overlays.length;
		int x = getSize().x;
		for (int i = 2; i >= 0; i--) {
			if (i < length && overlays[i] != null) {
				CachedImageDataProvider id = createCachedImageDataProvider(overlays[i]);
				x -= id.getWidth();
				drawImage(id, x, 0);
			}
		}
	}

	@Override
	protected Point getSize() {
		return fSize;
	}
}
