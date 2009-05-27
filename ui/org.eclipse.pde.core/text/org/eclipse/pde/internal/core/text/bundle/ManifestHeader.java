/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.bundle.BundleObject;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.IEditingModel;

public class ManifestHeader extends BundleObject implements IManifestHeader {
	private static final long serialVersionUID = 1L;

	private int fOffset = -1;
	private int fLength = -1;

	protected String fName;
	protected String fValue;

	protected transient IBundle fBundle;
	protected String fLineDelimiter;

	public ManifestHeader() {
	}

	public ManifestHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		fName = name;
		fBundle = bundle;
		fLineDelimiter = lineDelimiter;
		processValue(value);
		setModel(fBundle.getModel());
	}

	protected void processValue(String value) {
		fValue = value;
	}

	public String getLineLimiter() {
		return fLineDelimiter;
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

	public String getValue() {
		return fValue;
	}

	public void setValue(String value) {
		String old = fValue;
		fValue = value;
		fBundle.getModel().fireModelObjectChanged(this, getName(), old, value);
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
		StringBuffer sb = new StringBuffer(fName);
		sb.append(": "); //$NON-NLS-1$
		try {
			if (fOffset != -1) {
				IBundleModel model = fBundle.getModel();
				if (model instanceof IEditingModel) {
					IDocument doc = ((IEditingModel) model).getDocument();
					int line = doc.getLineOfOffset(fOffset);
					String text = doc.get(fOffset, doc.getLineLength(line)).trim();
					// respect a line break after a ":", if the user had entered it
					// bug 113098
					if (text.length() == fName.length() + 1) {
						sb.append(fLineDelimiter);
						sb.append(" "); //$NON-NLS-1$
					}
				}
			}
		} catch (BadLocationException e) {
		}
		sb.append(getValue());
		sb.append(fLineDelimiter);
		return sb.toString();
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

	public String getKey() {
		return getName();
	}

	public void setKey(String key) throws CoreException {
		setName(key);
	}

	protected int getManifestVersion() {
		return BundlePluginBase.getBundleManifestVersion(fBundle);
	}

	public void update() {
		// TODO
		// should do something for headers that don't have their own class
		// (and don't override this method)
	}

	public void update(boolean notify) {
	}
}
