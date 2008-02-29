/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal;


import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * A main icon and several adornments. The adornments
 * are computed according to flags set on creation of the descriptor.
 */
public class ApiImageDescriptor extends CompositeImageDescriptor {
	
	/** Flag to render the error (red x) adornment */
	public final static int ERROR =		0x0001;
	/** Flag to render the success (green check mark) adornment */
	public final static int SUCCESS =	0x0002;
	
	private ImageDescriptor fBaseImage;
	private int fFlags;
	private Point fSize;
	
	/**
	 * Create a new composite image.
	 * 
	 * @param baseImage an image descriptor used as the base image
	 * @param flags flags indicating which adornments are to be rendered
	 * 
	 */
	public ApiImageDescriptor(ImageDescriptor baseImage, int flags) {
		setBaseImage(baseImage);
		setFlags(flags);
	}
	
	/**
	 * @see CompositeImageDescriptor#getSize()
	 */
	protected Point getSize() {
		if (fSize == null) {
			ImageData data= getBaseImage().getImageData();
			setSize(new Point(data.width, data.height));
		}
		return fSize;
	}
	
	/**
	 * @see Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if (!(object instanceof ApiImageDescriptor)){
			return false;
		}
			
		ApiImageDescriptor other= (ApiImageDescriptor)object;
		return (getBaseImage().equals(other.getBaseImage()) && getFlags() == other.getFlags());
	}
	
	/**
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return getBaseImage().hashCode() | getFlags();
	}
	
	/**
	 * @see CompositeImageDescriptor#drawCompositeImage(int, int)
	 */
	protected void drawCompositeImage(int width, int height) {
		ImageData bg= getBaseImage().getImageData();
		if (bg == null) {
			bg= DEFAULT_IMAGE_DATA;
		}
		drawImage(bg, 0, 0);
		drawOverlays();
	}

	private ImageData getImageData(String imageDescriptorKey) {
		return ApiUIPlugin.getImageDescriptor(imageDescriptorKey).getImageData();
	}
	
	/**
	 * Add any overlays to the image as specified in the flags.
	 */
	protected void drawOverlays() {
		int flags= getFlags();
		int x= 0;
		int y= 0;
		ImageData data= null;
		if ((flags & ERROR) != 0) {
			x= 0;
			y= getSize().y;
			data= getImageData(IApiToolsConstants.IMG_OVR_ERROR);
			y -= data.height;
			drawImage(data, x, y);
		} else if ((flags & SUCCESS) != 0) {
			x= 0;
			y= getSize().y;
			data= getImageData(IApiToolsConstants.IMG_OVR_SUCCESS);
			y -= data.height;
			drawImage(data, x, y);
		} 
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
