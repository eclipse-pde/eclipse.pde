/*******************************************************************************
 *  Copyright (c) 2012 Christian Pontesegger and others.
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
