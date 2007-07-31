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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * The TocTopic class represents a topic element in a TOC.
 * A topic can link to a specific Help page. It can also have
 * children, which can be more topics.
 */
public class TocTopic extends TocObject {

	//The name associated with the topic
	protected String fFieldLabel;
	
	//The link attribute of the topic; contains a path to a Help page
	//that can be reached by clicking on this topic in the Table of Contents
	//of a Help view. This field is optional, and can be null.
	protected String fFieldRef;
	
	//The list of child elements in the topic
	private ArrayList fFieldElements;
	
	//These three lists hold the different child elements
	//that a topic can currently have. Currently only used
	//for debugging purposes.
	//TODO: Remove these lists when the TOC editor is stable
	private ArrayList fFieldLinks;
	private ArrayList fFieldAnchors;
	private ArrayList fFieldTopics;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a topic with the given model and parent.
	 * 
	 * @param model The model associated with the new topic.
	 * @param parent The parent TocObject of the new topic.
	 */
	public TocTopic(TocModel model, TocObject parent) {
		super(model, parent);
	}
	
	/**
	 * Constructs a topic with the given model, parent and file.
	 * 
	 * @param model The model associated with the new link.
	 * @param parent The parent TocObject of the new link.
	 * @param file The page to link to.
	 */
	public TocTopic(TocModel model, TocObject parent, IFile file) {
		super(model, parent);

		IPath path = file.getFullPath();
		if(file.getProject().equals(getModel().getUnderlyingResource().getProject()))
		{	//If the file is from the same project,
			//remove the project name segment
			fFieldRef = path.removeFirstSegments(1).toString(); //$NON-NLS-1$
		}
		else
		{	//If the file is from another project, add ".."
			//to traverse outside this model's project
			fFieldRef = ".." + path.toString(); //$NON-NLS-1$
		}
	}

	/**
	 * @return a copy of the current list of children for this topic.
	 * 
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getChildren()
	 */
	public List getChildren() {
		//Create a copy of the child list instead of 
		//returning the list itself. That way, our list
		//of children cannot be altered from outside
		ArrayList list = new ArrayList();
		// Add children of this topic
		if (fFieldElements.size() > 0) {
			list.addAll(fFieldElements);
		}
		return list;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#canBeParent()
	 */
	public boolean canBeParent() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_TOPIC;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getName()
	 */
	public String getName() {
		return fFieldLabel;
	}

	public String getPath() {
		return fFieldRef;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_TOPIC;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#isFirstChildObject(org.eclipse.pde.internal.core.toc.TocObject)
	 */
	public boolean isFirstChildObject(TocObject tocObject) {
		int position = fFieldElements.indexOf(tocObject);
		if (position == 0) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#isLastChildObject(org.eclipse.pde.internal.core.toc.TocObject)
	 */
	public boolean isLastChildObject(TocObject tocObject) {
		int position = fFieldElements.indexOf(tocObject);
		int lastPosition = fFieldElements.size() - 1;
		if (position == lastPosition) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#parseAttributes(org.w3c.dom.Element)
	 */
	protected void parseAttributes(Element element) {
		parseFieldLabel(element);
		parseFieldLink(element);
	}

	/**
	 * Parse the label attribute of the topic.
	 * 
	 * @param element The XML element to parse
	 */
	private void parseFieldLabel(Element element) {
		// Process label attribute
		// Trim leading and trailing whitespace
		fFieldLabel = element.getAttribute(ATTRIBUTE_LABEL).trim();
	}
	
	/**
	 * Parse the link attribute of the topic, if it exists.
	 * 
	 * @param element The XML element to parse
	 */
	protected void parseFieldLink(Element element) {
		// Process link attribute (href for topic elements)
		// Trim leading and trailing whitespace
		fFieldRef = element.getAttribute(ATTRIBUTE_HREF).trim();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#parseElement(org.w3c.dom.Element)
	 */
	protected void parseElement(Element element) {
		String name = element.getNodeName();
		TocModelFactory factory = getModel().getFactory();
		
		if (name.equals(ELEMENT_TOPIC)) {
			// Process child topic element
			TocTopic topic = factory.createTocTopic(this);
			fFieldTopics.add(topic);
			fFieldElements.add(topic);
			topic.parse(element);
		} else if (name.equals(ELEMENT_ANCHOR)) { 
			// Process child anchor element
			TocAnchor anchor = factory.createTocAnchor(this);
			fFieldAnchors.add(anchor);
			fFieldElements.add(anchor);
			anchor.parse(element);
		} else if (name.equals(ELEMENT_LINK)) { 
			// Process child link element
			TocLink link = factory.createTocLink(this);
			fFieldLinks.add(link);
			fFieldElements.add(link);
			link.parse(element);
		}
	}

	/**
	 * Add a TocObject child to this topic
	 * and signal the model if necessary.
	 * 
	 * @param child The child to add to the TocObject
	 */
	public void addChild(TocObject child) {
		fFieldElements.add(child);
		child.setParent(this);
		if (isEditable()) {
			fireStructureChanged(child, IModelChangedEvent.INSERT);
		}
	}

	/**
	 * Add a TocObject child to this topic
	 * beside a specified sibling
	 * and signal the model if necessary.
	 * 
	 * @param child The child to add to the TocObject
	 * @param sibling The object that will become the child's direct sibling
	 * @param insertBefore If the object should be inserted before the sibling
	 */
	public void addChild(TocObject child, TocObject sibling, boolean insertBefore) {
		int currentIndex = fFieldElements.indexOf(sibling);
		if(!insertBefore)
		{	currentIndex++;
		}

		fFieldElements.add(currentIndex, child);
		child.setParent(this);
		if (isEditable()) {
			fireStructureChanged(child, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#moveChild(org.eclipse.pde.internal.core.toc.TocObject, int)
	 */
	public void moveChild(TocObject tocObject,
			int newRelativeIndex) {
		// Get the current index of the child
		int currentIndex = fFieldElements.indexOf(tocObject);
		// Ensure the object is found
		if (currentIndex == -1) {
			return;
		}
		// Calculate the new location of the child
		int newIndex = newRelativeIndex + currentIndex;
		// Validate the new location
		if ((newIndex < 0) ||
				(newIndex >= fFieldElements.size())) {
			return;
		}
		// Remove the child and add it back at the new location
		fFieldElements.remove(tocObject);
		fFieldElements.add(newIndex, tocObject);
		// Send an insert event
		if (isEditable()) {
			fireStructureChanged(tocObject, IModelChangedEvent.INSERT);
		}	
	}
	
	/**
	 * Remove a TocObject child from this topic
	 * and signal the model if necessary.
	 * 
	 * @param child The child to add to the TocObject
	 */
	public void removeChild(TocObject tocObject) {
		fFieldElements.remove(tocObject);
		if (isEditable()) {
			fireStructureChanged(tocObject, IModelChangedEvent.REMOVE);
		}	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#reset()
	 */
	public void reset() {
		fFieldLabel = null;
		fFieldRef = null;
		
		fFieldElements = new ArrayList();
		
		fFieldTopics = new ArrayList();
		fFieldAnchors = new ArrayList();
		fFieldLinks = new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#writeAttributes(java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer) {
		printLabelAttribute(buffer);		
		printLinkAttribute(buffer);
	}

	/**
	 * Print out the label attribute of the topic.
	 *  
	 * @param buffer the buffer to which the attribute is written.
	 */
	private void printLabelAttribute(StringBuffer buffer) {
		// Print label attribute
		if ((fFieldLabel != null) && 
				(fFieldLabel.length() > 0)) {
			// No trim required
			// No encode required
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_LABEL, fFieldLabel));
		}
	}

	/**
	 * Print out the link attribute of the topic, if it exists.
	 *  
	 * @param buffer the buffer to which the attribute is written.
	 */
	protected void printLinkAttribute(StringBuffer buffer) {
		// Print link attribute
		if ((fFieldRef != null) && 
				(fFieldRef.length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_HREF, 
					PDETextHelper.translateWriteText(
							fFieldRef.trim(), DEFAULT_SUBSTITUTE_CHARS)));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#writeElements(java.lang.String, java.io.PrintWriter)
	 */
	protected void writeElements(String indent, PrintWriter writer) {
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		
		// Print elements
		Iterator iterator = fFieldElements.iterator();
		while (iterator.hasNext()) {
			TocObject object = (TocObject)iterator.next();
			object.write(newIndent, writer);
		}
	}

	/**
	 * @return the label associated with this topic.
	 */
	public String getFieldLabel() {
		return fFieldLabel;
	}

	/**
	 * @return the link associated with this topic, <br />
	 * or <code>null</code> if none exists.
	 */
	public String getFieldRef() {
		return fFieldRef;
	}

	/**
	 * Change the value of the label field and 
	 * signal a model change if needed.
	 * 
	 * @param name The new label for the topic
	 */
	public void setFieldLabel(String name) {
		String old = fFieldLabel;
		fFieldLabel = name;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_LABEL, old, fFieldLabel);
		}
	}

	/**
	 * Change the value of the link field and 
	 * signal a model change if needed.
	 * 
	 * @param value The new page location to be linked by this topic
	 */
	public void setFieldRef(String value) {
		String old = fFieldRef;
		fFieldRef = value;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_HREF, old, fFieldRef);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#parseText(org.w3c.dom.Text)
	 */
	protected void parseText(Text text) {
		// NO-OP
	}

}
