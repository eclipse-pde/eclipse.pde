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

package org.eclipse.pde.internal.ui.views.imagebrowser;

public interface IImageTarget {

	/**
	 * Notifies the target that an image was found.
	 * 
	 * @param element detected image data
	 */
	void notifyImage(ImageElement element);

	/**
	 * Query the target whether additional images are needed
	 * 
	 * @return <code>true</code> when more images should be fetched
	 */
	boolean needsMore();
}
