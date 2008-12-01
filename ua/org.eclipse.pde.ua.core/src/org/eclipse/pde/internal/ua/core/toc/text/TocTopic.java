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
 * The TocTopic class represents a topic element in a TOC. A topic can link to a
 * specific Help page. It can also have children, which can be more topics.
 */
public class TocTopic extends TocObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a topic with the given model and parent.
	 * 
	 * @param model
	 *            The model associated with the new topic.
	 * @param parent
	 *            The parent TocObject of the new topic.
	 */
	public TocTopic(TocModel model) {
		super(model, ELEMENT_TOPIC);
	}

	/**
	 * Constructs a subclass of a topic with the given model and parent.
	 * 
	 * @param model
	 *            The model associated with the new topic.
	 * @param parent
	 *            The parent TocObject of the new topic.
	 */
	public TocTopic(TocModel model, String tagName) {
		super(model, tagName);
	}

	/**
	 * Constructs a topic with the given model, parent and file.
	 * 
	 * @param model
	 *            The model associated with the new link.
	 * @param parent
	 *            The parent TocObject of the new link.
	 * @param file
	 *            The page to link to.
	 */
	public TocTopic(TocModel model, IFile file) {
		super(model, ELEMENT_TOPIC);

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
			setFieldRef(path.removeFirstSegments(1).toString()); //$NON-NLS-1$
		} else { // If the file is from another project, add ".."
			// to traverse outside this model's project
			setFieldRef(".." + path.toString()); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.toc.TocObject#canBeParent()
	 */
	public boolean canBeParent() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.toc.TocObject#getName()
	 */
	public String getName() {
		return getFieldLabel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.text.toc.TocObject#getPath()
	 */
	public String getPath() {
		return getFieldRef();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_TOPIC;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.toc.TocObject#isFirstChildObject(org.eclipse
	 * .pde.internal.ua.core.toc.TocObject)
	 */
	public boolean isFirstChildObject(TocObject tocObject) {
		return super.isFirstChildNode(tocObject, TocObject.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.toc.TocObject#isLastChildObject(org.eclipse
	 * .pde.internal.ua.core.toc.TocObject)
	 */
	public boolean isLastChildObject(TocObject tocObject) {
		return super.isLastChildNode(tocObject, TocObject.class);
	}

	/**
	 * Add a TocObject child to this topic and signal the model if necessary.
	 * 
	 * @param child
	 *            The child to add to the TocObject
	 */
	public void addChild(TocObject child) {
		addChildNode(child, true);
	}

	/**
	 * Add a TocObject child to this topic beside a specified sibling and signal
	 * the model if necessary.
	 * 
	 * @param child
	 *            The child to add to the TocObject
	 * @param sibling
	 *            The object that will become the child's direct sibling
	 * @param insertBefore
	 *            If the object should be inserted before the sibling
	 */
	public void addChild(TocObject child, TocObject sibling,
			boolean insertBefore) {
		int currentIndex = indexOf(sibling);
		if (!insertBefore) {
			currentIndex++;
		}

		addChildNode(child, currentIndex, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.toc.TocObject#moveChild(org.eclipse.pde
	 * .internal.core.toc.TocObject, int)
	 */
	public void moveChild(TocObject tocObject, int newRelativeIndex) {
		moveChildNode(tocObject, newRelativeIndex, true);
	}

	/**
	 * Remove a TocObject child from this topic and signal the model if
	 * necessary.
	 * 
	 * @param child
	 *            The child to add to the TocObject
	 */
	public void removeChild(TocObject tocObject) {
		removeChildNode(tocObject, true);
	}

	/**
	 * @return the label associated with this topic.
	 */
	public String getFieldLabel() {
		return getXMLAttributeValue(ATTRIBUTE_LABEL);
	}

	/**
	 * Change the value of the label field and signal a model change if needed.
	 * 
	 * @param name
	 *            The new label for the topic
	 */
	public void setFieldLabel(String name) {
		setXMLAttribute(ATTRIBUTE_LABEL, name);
	}

	/**
	 * @return the link associated with this topic, <br />
	 *         or <code>null</code> if none exists.
	 */
	public String getFieldRef() {
		return getXMLAttributeValue(ATTRIBUTE_HREF);
	}

	/**
	 * Change the value of the link field and signal a model change if needed.
	 * 
	 * @param value
	 *            The new page location to be linked by this topic
	 */
	public void setFieldRef(String value) {
		setXMLAttribute(ATTRIBUTE_HREF, value);
	}
}
