/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

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
