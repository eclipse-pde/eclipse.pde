package org.eclipse.pde.internal.ui.neweditor.site;

import java.io.*;

import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;

/**
 * @author melhem
 *
 */
public class SiteFeatureAdapter implements Serializable, IWritable {
	String category;
	ISiteFeature feature;
	public SiteFeatureAdapter(String category, ISiteFeature feature) {
		this.category = category;
		this.feature = feature;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		feature.write(indent, writer);
	}
}
