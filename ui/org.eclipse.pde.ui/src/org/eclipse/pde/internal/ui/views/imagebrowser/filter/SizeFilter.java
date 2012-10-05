/*******************************************************************************
 *  Copyright (c) 2012 Christian Pontesegger and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.filter;

import org.eclipse.pde.internal.ui.views.imagebrowser.ImageElement;

public class SizeFilter implements IFilter<ImageElement> {

	public static final int TYPE_EXACT = 0;
	public static final int TYPE_BIGGER_EQUALS = 1;
	public static final int TYPE_SMALLER_EQUALS = 2;
	private final int mWidth;
	private final int mWidthType;
	private final int mHeight;
	private final int mHeightType;

	public SizeFilter(final int width, final int widthType, final int height, final int heightType) {
		mWidth = width;
		mWidthType = widthType;
		mHeight = height;
		mHeightType = heightType;
	}

	public boolean accept(final ImageElement element) {
		boolean accept = true;

		switch (mWidthType) {
		case TYPE_EXACT:
			accept &= (element.getImageData().width == mWidth);
			break;
		case TYPE_BIGGER_EQUALS:
			accept &= (element.getImageData().width >= mWidth);
			break;
		case TYPE_SMALLER_EQUALS:
			accept &= (element.getImageData().width <= mWidth);
			break;
		}

		switch (mHeightType) {
		case TYPE_EXACT:
			accept &= (element.getImageData().height == mHeight);
			break;
		case TYPE_BIGGER_EQUALS:
			accept &= (element.getImageData().height >= mHeight);
			break;
		case TYPE_SMALLER_EQUALS:
			accept &= (element.getImageData().height <= mHeight);
			break;
		}

		return accept;
	}
}
