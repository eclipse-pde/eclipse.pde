package org.eclipse.pde.internal.core.schema;

import java.net.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class IncludedSchemaDescriptor extends AbstractSchemaDescriptor {
	private URL url;
	private String schemaLocation;
	private ISchemaDescriptor parent;

	public IncludedSchemaDescriptor(ISchemaDescriptor parent, String schemaLocation) {
		this.parent = parent;
		this.schemaLocation = schemaLocation;

		try {
			url = computeURL(parent.getSchemaURL(), schemaLocation);
		}
		catch (MalformedURLException e) {
		}
	}
	
	public IFile getFile() {
		if (parent instanceof FileSchemaDescriptor) {
			FileSchemaDescriptor fparent = (FileSchemaDescriptor)parent;
			IFile parentFile = fparent.getFile();
			if (parentFile==null) return null;
			IPath parentPath = parentFile.getProjectRelativePath();
			IPath childPath = parentPath.removeLastSegments(1).append(schemaLocation);
			return parentFile.getProject().getFile(childPath);
		}
		return null;
	}
	
	public static URL computeURL(URL parentURL, String schemaLocation) throws MalformedURLException {
		IPath path = new Path(parentURL.getPath());
		path = path.removeLastSegments(1).append(schemaLocation);
		return new URL(parentURL.getProtocol(), parentURL.getHost(), path.toString());
	}

	/**
	 * @see org.eclipse.pde.internal.core.schema.AbstractSchemaDescriptor#isEnabled()
	 */
	public boolean isEnabled() {
		return true;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getPointId()
	 */
	public String getPointId() {
		int dotLoc = schemaLocation.lastIndexOf('.');
		if (dotLoc!= -1) {
			return schemaLocation.substring(0, dotLoc);
		}
		return null;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getSchemaURL()
	 */
	public URL getSchemaURL() {
		return url;
	}
}
