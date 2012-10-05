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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

/**
 * @author cwindatt
 *
 */
public class ActiveImageSourceProvider extends AbstractSourceProvider {

	public static final String ACTIVE_IMAGE = "org.eclipse.pde.ui.imagebrowser.activeImage"; //$NON-NLS-1$

	private ImageElement mImageData = null;

	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISourceProvider#getCurrentState()
	 */
	@SuppressWarnings("rawtypes")
	public Map getCurrentState() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(ACTIVE_IMAGE, mImageData);
		return map;
	}

	public String[] getProvidedSourceNames() {
		return new String[] {ACTIVE_IMAGE};
	}

	public void setImageData(final ImageElement imageData) {
		mImageData = imageData;

		fireSourceChanged(ISources.WORKBENCH, ACTIVE_IMAGE, mImageData);
	}
}
