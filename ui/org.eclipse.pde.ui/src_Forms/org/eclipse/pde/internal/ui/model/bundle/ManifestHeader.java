/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.bundle;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.bundle.BundleObject;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.ui.model.IDocumentKey;

public class ManifestHeader extends BundleObject implements IDocumentKey {
    private static final long serialVersionUID = 1L;
    private int fOffset = -1;
	private int fLength = -1;
    
	protected String fName;
	protected String fValue;
    private IBundle fBundle;
    
    public ManifestHeader(String name, String value, IBundle bundle) {
        fName = name;
        fValue = value;
        fBundle = bundle;
        setModel(fBundle.getModel());
    }

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setName(java.lang.String)
	 */
	public void setName(String name) {
		fName = name;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getName()
	 */
	public String getName() {
		return fName;
	}
	
	public void setValue(String value) {
		fValue = value;
	}
	
	public String getValue() {
		return fValue;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setOffset(int)
	 */
	public void setOffset(int offset) {
		fOffset = offset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getOffset()
	 */
	public int getOffset() {
		return fOffset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setLength(int)
	 */
	public void setLength(int length) {
		fLength = length;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getLength()
	 */
	public int getLength() {
		return fLength;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#write()
	 */
	public String write() {
        updateValue();
		return fName + ": " + fValue; //$NON-NLS-1$
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}
    
    public void setBundle(IBundle bundle) {
        fBundle = bundle;
    }
    
    public IBundle getBundle() {
        return fBundle;
    }
    
    public void updateValue() {
    }
    
	
}
