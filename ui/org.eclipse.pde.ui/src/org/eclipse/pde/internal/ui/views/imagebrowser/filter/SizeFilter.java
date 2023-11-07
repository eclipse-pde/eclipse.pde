/*******************************************************************************
 *  Copyright (c) 2012, 2015 Christian Pontesegger and others.
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
 *     IBM Corporation - bugs fixing
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.filter;

import org.eclipse.pde.internal.ui.views.imagebrowser.ImageElement;

public class SizeFilter implements IFilter {

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

	@Override
	public boolean accept(final ImageElement element) {
		

		boolean accept = switch (mWidthType) {
			case TYPE_EXACT -> element.getImageData().width == mWidth;
			case TYPE_BIGGER_EQUALS -> element.getImageData().width >= mWidth;
			case TYPE_SMALLER_EQUALS -> element.getImageData().width <= mWidth;
			default -> true;
		};

		accept &= switch (mHeightType) {
			case TYPE_EXACT -> (element.getImageData().height == mHeight);
			case TYPE_BIGGER_EQUALS -> (element.getImageData().height >= mHeight);
			case TYPE_SMALLER_EQUALS -> (element.getImageData().height <= mHeight);
			default -> accept;
		};

		return accept;
	}
}
