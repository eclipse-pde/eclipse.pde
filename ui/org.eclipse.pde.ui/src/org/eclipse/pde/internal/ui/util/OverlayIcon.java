package org.eclipse.pde.internal.ui.util;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * An OverlayIcon consists of a main icon and several adornments.
 */
public class OverlayIcon extends AbstractOverlayIcon {
	private ImageDescriptor fBase;

	public OverlayIcon(ImageDescriptor base, ImageDescriptor[][] overlays) {
		this(base, overlays, null);
	}
	
	public OverlayIcon(ImageDescriptor base, ImageDescriptor[][] overlays, Point size) {
		super(overlays, size);
		fBase= base;
		if (fBase == null)
			fBase= ImageDescriptor.getMissingImageDescriptor();
	}
	
	protected ImageData getBaseImageData() {
		return fBase.getImageData();
	}
}
