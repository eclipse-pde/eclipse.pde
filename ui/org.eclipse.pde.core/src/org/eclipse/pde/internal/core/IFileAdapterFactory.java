package org.eclipse.pde.internal.core;

import java.io.File;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public interface IFileAdapterFactory {
	public Object createAdapterChild(FileAdapter parent, File file);

}
