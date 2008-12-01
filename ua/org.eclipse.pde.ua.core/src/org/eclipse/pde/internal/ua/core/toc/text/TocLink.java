/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.toc.text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

/**
 * The TocLink object represents a link to another TOC. Links in TOCs are much
 * like import or include directives, in the sense that they bring all the
 * contents of the linked TOC into the TOC that has this link.
 * 
 * TOC links cannot have any content within them, so they are leaf objects.
 */
public class TocLink extends TocObject {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a link with the given model and parent.
	 * 
	 * @param model
	 *            The model associated with the new link.
	 * @param parent
	 *            The parent TocObject of the new link.
	 */
	public TocLink(TocModel model) {
		super(model, ELEMENT_LINK);
	}

	/**
	 * Constructs a link with the given model, parent and file.
	 * 
	 * @param model
	 *            The model associated with the new link.
	 * @param parent
	 *            The parent TocObject of the new link.
	 * @param file
	 *            The TOC file to link to.
	 */
	public TocLink(TocModel model, IFile file) {
		super(model, ELEMENT_LINK);

		IPath path = file.getFullPath();
		if (file.getProject().equals(
				getSharedModel().getUnderlyingResource().getProject())) { // If
																			// the
																			// file
																			// is
																			// from
																			// the
																			// same
																			// project,
			// remove the project name segment
			setFieldTocPath(path.removeFirstSegments(1).toString()); //$NON-NLS-1$
		} else { // If the file is from another project, add ".."
			// to traverse outside this model's project
			setFieldTocPath(".." + path.toString()); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.text.toc.TocObject#canBeParent()
	 */
	public boolean canBeParent() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_LINK;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.toc.TocObject#getName()
	 */
	public String getName() {
		return getFieldTocPath();
	}

	public String getPath() {
		return getFieldTocPath();
	}

	/**
	 * @return the path associated with this TOC link
	 */
	public String getFieldTocPath() {
		return getXMLAttributeValue(ATTRIBUTE_TOC);
	}

	/**
	 * Change the value of the link path and signal a model change if needed.
	 * 
	 * @param id
	 *            The new path to associate with the link
	 */
	public void setFieldTocPath(String path) {
		setXMLAttribute(ATTRIBUTE_TOC, path);
	}

}
