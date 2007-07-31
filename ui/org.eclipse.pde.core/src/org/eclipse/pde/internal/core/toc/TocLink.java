/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.toc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.w3c.dom.Element;

/**
 * The TocLink object represents a link to another TOC.
 * Links in TOCs are much like import or include directives, in the
 * sense that they bring all the contents of the linked TOC into the
 * TOC that has this link.
 *
 * TOC links cannot have any content within them, so they are leaf objects.
 */
public class TocLink extends TocLeafObject {

	private static final long serialVersionUID = 1L;

	//TOC links only have one attribute:
	//the path to the linked TOC	
	private String fFieldTocPath;
	
	/**
	 * Constructs a link with the given model and parent.
	 * 
	 * @param model The model associated with the new link.
	 * @param parent The parent TocObject of the new link.
	 */
	public TocLink(TocModel model, TocObject parent) {
		super(model, parent);
	}

	/**
	 * Constructs a link with the given model, parent and file.
	 * 
	 * @param model The model associated with the new link.
	 * @param parent The parent TocObject of the new link.
	 * @param file The TOC file to link to.
	 */
	public TocLink(TocModel model, TocObject parent, IFile file) {
		super(model, parent);

		IPath path = file.getFullPath();
		if(file.getProject().equals(getModel().getUnderlyingResource().getProject()))
		{	//If the file is from the same project,
			//remove the project name segment
			fFieldTocPath = path.removeFirstSegments(1).toString(); //$NON-NLS-1$
		}
		else
		{	//If the file is from another project, add ".."
			//to traverse outside this model's project
			fFieldTocPath = ".." + path.toString(); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_LINK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_LINK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#parseAttributes(org.w3c.dom.Element)
	 */
	protected void parseAttributes(Element element) {
		parseFieldTocPath(element);
	}

	/**
	 * Process the path attribute for TOC link elements.
	 * 
	 * @param element The XML element to parse
	 */
	private void parseFieldTocPath(Element element) {
		// Process path attribute
		// Trim leading and trailing whitespace
		fFieldTocPath = element.getAttribute(ATTRIBUTE_TOC).trim();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#reset()
	 */
	public void reset() {
		fFieldTocPath = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#writeAttributes(java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer) {
		if ((fFieldTocPath != null) && 
				(fFieldTocPath.length() > 0)) {
			// No trim required
			// No encode required
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_TOC, fFieldTocPath));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getName()
	 */
	public String getName() {
		return fFieldTocPath;
	}

	public String getPath() {
		return fFieldTocPath;
	}

	/**
	 * @return the path associated with this TOC link
	 */
	public String getFieldTocPath() {
		return fFieldTocPath;
	}

	/**
	 * Change the value of the link path and 
	 * signal a model change if needed.
	 * 
	 * @param id The new path to associate with the link
	 */
	public void setFieldTocPath(String path) {
		String old = path;
		fFieldTocPath = path;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_ID, old, fFieldTocPath);
		}
	}
	
}
