package org.eclipse.pde.internal.builders;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SchemaMarkerFactory implements IMarkerFactory {
	private String point;
	
	public SchemaMarkerFactory() {
	}

	public SchemaMarkerFactory(String point) {
		this.point = point;
	}
	
	public void setPoint(String point) {
		this.point = point;
	}

	/**
	 * @see org.eclipse.pde.internal.builders.IMarkerFactory#createMarker(org.eclipse.core.resources.IFile)
	 */
	public IMarker createMarker(IFile file) throws CoreException {
		IMarker marker = file.createMarker("org.eclipse.pde.validation-marker");
		marker.setAttribute("point", point);
		return marker;
	}
}
