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

/**
 * The TocModelFactory is a data factory used by a TOC model
 * and by TOC objects to create new TocObjects.
 */
public class TocModelFactory {

	private TocModel fModel;
	
	/**
	 * Creates a new TocModelFactory.
	 * 
	 * @param model the model to be associated with this factory.
	 */
	public TocModelFactory(TocModel model) {
		fModel = model;
	}
	
	/**
	 * @return a new Table of Contents.
	 * 
	 * @see org.eclipse.pde.internal.core.toc.Toc
	 */
	public Toc createToc() {
		return new Toc(fModel);
	}
	
	/**
	 * @return a new TOC topic.
	 * 
	 * @see org.eclipse.pde.internal.core.toc.TocTopic
	 */
	public TocTopic createTocTopic(TocObject parent) {
		return new TocTopic(fModel, parent);
	}
	
	/**
	 * @return a new TOC topic.
	 * 
	 * @see org.eclipse.pde.internal.core.toc.TocTopic
	 */
	public TocTopic createTocTopic(TocObject parent, IFile file) {
		return new TocTopic(fModel, parent, file);
	}
	
	/**
	 * @return a new TOC link.
	 * 
	 * @see org.eclipse.pde.internal.core.toc.TocLink
	 */
	public TocLink createTocLink(TocObject parent) {
		return new TocLink(fModel, parent);
	}
	
	/**
	 * @return a new TOC link pointing to the specified TOC file.
	 * 
	 * @see org.eclipse.pde.internal.core.toc.TocLink
	 */
	public TocLink createTocLink(TocObject parent, IFile file) {
		return new TocLink(fModel, parent, file);
	}

	/**
	 * @return a new TOC anchor.
	 * 
	 * @see org.eclipse.pde.internal.core.toc.TocAnchor
	 */
	public TocAnchor createTocAnchor(TocObject parent) {
		return new TocAnchor(fModel, parent);
	}

	/**
	 * @return a new TOC element enablement.
	 * 
	 * @see org.eclipse.pde.internal.core.toc.TocAnchor
	 */
	public TocEnablement createTocEnablement(TocObject parent) {
		return new TocEnablement(fModel, parent);
	}
}
