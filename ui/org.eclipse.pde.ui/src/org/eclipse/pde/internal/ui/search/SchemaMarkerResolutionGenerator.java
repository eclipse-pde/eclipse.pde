/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;

/**
 * Insert the type's description here.
 * @see IMarkerResolutionGenerator
 */
public class SchemaMarkerResolutionGenerator implements IMarkerResolutionGenerator {
	/**
	 * The constructor.
	 */
	private IMarkerResolution [] resolutions = new IMarkerResolution[1];

	class SchemaMarkerResolution implements IMarkerResolution {
		private ShowDescriptionAction action;
		public String getLabel() {
			return PDEPlugin.getResourceString("SchemaMarkerResolutionGenerator.label"); //$NON-NLS-1$
		}
		
		public void run(IMarker marker) {
			try {
				String point = (String)marker.getAttribute("point"); //$NON-NLS-1$
				if (point==null) return;
				ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(point);

				if (action==null)
					action = new ShowDescriptionAction(schema);
				else
					action.setSchema(schema);
				action.run();
			}
			catch (CoreException e) {
				PDEPlugin.logException(e); 
			}
		}
	}
	public SchemaMarkerResolutionGenerator() {
		resolutions[0] = new SchemaMarkerResolution();
	}

	/**
	 * Insert the method's description here.
	 * @see IMarkerResolutionGenerator#getResolutions
	 */
	public IMarkerResolution [] getResolutions(IMarker marker)  {
		return resolutions;
	}
}
