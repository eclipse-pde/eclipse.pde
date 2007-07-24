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

package org.eclipse.pde.internal.core.text.cheatsheet.simple;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.text.DocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentNodeFactory;
import org.eclipse.pde.internal.core.text.plugin.DocumentGenericNode;

/**
 * SimpleCSDocumentFactory
 *
 */
public class SimpleCSDocumentFactory implements IDocumentNodeFactory,
		ISimpleCSModelFactory {

	private SimpleCSModel fModel;
	
	/**
	 * @param model
	 */
	public SimpleCSDocumentFactory(SimpleCSModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createAttribute(java.lang.String, java.lang.String, org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public IDocumentAttribute createAttribute(String name, String value,
			IDocumentNode enclosingElement) {

		IDocumentAttribute attribute = new DocumentAttributeNode();
		try {
			attribute.setAttributeName(name);
			attribute.setAttributeValue(value);
		} catch (CoreException e) {
			// Ignore
		}
		attribute.setEnclosingElement(enclosingElement);
		// TODO: MP: TEO: Remove if not needed
		//attribute.setModel(fModel);
		//attribute.setInTheModel(true);
		return attribute;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createDocumentNode(java.lang.String, org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public IDocumentNode createDocumentNode(String name, IDocumentNode parent) {

		// Semantics:
		// org.eclipse.platform.doc.isv/reference/extension-points/cheatSheetContentFileSpec.html
		
		// TODO: MP: TEO: Parent is not needed as it is set in the DocumentHandler
		// TODO: MP: TEO: Could delegate to model classes to do creation?
		// TODO: MP: TEO: Enforce model validity rules? Do not read in extraneous elements?
		// Note: Cannot return null
		// TODO: MP: TEO:  Change to interfaces for checking instance of and cast
		// TODO: MP: TEO:  Prioritize "if" order
		
		if (parent == null) {
			if (isSimpleCS(name)) {
				// Root
				return (IDocumentNode)createSimpleCS();
			}
		} else if (parent instanceof SimpleCS) {
			if (isIntro(name)) {
				// Intro
				return (IDocumentNode)createSimpleCSIntro((SimpleCS)parent);
			} else if (isItem(name)) {
				// Item
				return (IDocumentNode)createSimpleCSItem((SimpleCS)parent);
			}			
		} else if (parent instanceof SimpleCSIntro) {
			if (isDescription(name)) {
				// Description
				return (IDocumentNode)createSimpleCSDescription((SimpleCSIntro)parent);
			}
		} else if (parent instanceof SimpleCSItem) {
			if (isDescription(name)) {
				// Description
				return (IDocumentNode)createSimpleCSDescription((SimpleCSItem)parent);
			} else if (isAction(name)) {
				// Action
				return (IDocumentNode)createSimpleCSAction((SimpleCSItem)parent);
			} else if (isCommand(name)) {
				// Command
				return (IDocumentNode)createSimpleCSCommand((SimpleCSItem)parent);
			} else if (isPerformWhen(name)) {
				// Perform When
				return (IDocumentNode)createSimpleCSPerformWhen((SimpleCSItem)parent);
			} else if (isSubitem(name)) {
				// Subitem
				return (IDocumentNode)createSimpleCSSubItem((SimpleCSItem)parent);
			} else if (isConditionalSubitem(name)) {
				// Conditional Subitem
				return (IDocumentNode)createSimpleCSConditionalSubItem((SimpleCSItem)parent);
			} else if (isRepeatedSubitem(name)) {
				// Repeated Subitem
				return (IDocumentNode)createSimpleCSRepeatedSubItem((SimpleCSItem)parent);
			} else if (isOnCompletion(name)) {
				// On Completion
				return (IDocumentNode)createSimpleCSOnCompletion((SimpleCSItem)parent);
			}
		} else if (parent instanceof SimpleCSSubItem) {
			if (isPerformWhen(name)) {
				// Perform When
				return (IDocumentNode)createSimpleCSPerformWhen((SimpleCSSubItem)parent);
			} else if (isAction(name)) {
				// Action
				return (IDocumentNode)createSimpleCSAction((SimpleCSSubItem)parent);
			} else if (isCommand(name)) {
				// Command
				return (IDocumentNode)createSimpleCSCommand((SimpleCSSubItem)parent);
			}
		} else if (parent instanceof SimpleCSConditionalSubItem) {
			if (isSubitem(name)) {
				// Subitem
				return (IDocumentNode)createSimpleCSSubItem((SimpleCSConditionalSubItem)parent);
			}
		} else if (parent instanceof SimpleCSRepeatedSubItem) {
			if (isSubitem(name)) {
				// Subitem
				return (IDocumentNode)createSimpleCSSubItem((SimpleCSRepeatedSubItem)parent);
			}
		} else if (parent instanceof SimpleCSPerformWhen) {
			if (isAction(name)) {
				// Action
				return (IDocumentNode)createSimpleCSAction((SimpleCSPerformWhen)parent);
			} else if (isCommand(name)) {
				// Command
				return (IDocumentNode)createSimpleCSCommand((SimpleCSPerformWhen)parent);
			}
		}
		// Description has no children
		// Action has no children
		// Command has no children
		// OnCompletion has no children
		
		// Cannot return null
		// Foreign elements are stored as generics
		return createGeneric(name);
	}

	/**
	 * @param name
	 * @param elementName
	 * @return
	 */
	private boolean isCSElement(String name, String elementName) {
		if (name.equals(elementName)) {
			return true;
		}
		return false;
	}
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isSimpleCS(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_CHEATSHEET);
	}
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isIntro(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_INTRO);
	}
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isDescription(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_DESCRIPTION);
	}	
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isItem(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_ITEM);
	}	
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isAction(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_ACTION);
	}		
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isCommand(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_COMMAND);
	}		

	/**
	 * @param name
	 * @return
	 */
	private boolean isPerformWhen(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_PERFORM_WHEN);
	}		

	/**
	 * @param name
	 * @return
	 */
	private boolean isSubitem(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_SUBITEM);
	}		

	/**
	 * @param name
	 * @return
	 */
	private boolean isRepeatedSubitem(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_REPEATED_SUBITEM);
	}		
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isConditionalSubitem(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_CONDITIONAL_SUBITEM);
	}		

	/**
	 * @param name
	 * @return
	 */
	private boolean isOnCompletion(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_ONCOMPLETION);
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCS()
	 */
	public ISimpleCS createSimpleCS() {
		return new SimpleCS(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSAction(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSAction createSimpleCSAction(ISimpleCSObject parent) {
		return new SimpleCSAction(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSCommand(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSCommand createSimpleCSCommand(ISimpleCSObject parent) {
		return new SimpleCSCommand(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSConditionalSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSConditionalSubItem createSimpleCSConditionalSubItem(
			ISimpleCSObject parent) {
		return new SimpleCSConditionalSubItem(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSDescription(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSDescription createSimpleCSDescription(ISimpleCSObject parent) {
		return new SimpleCSDescription(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSIntro(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSIntro createSimpleCSIntro(ISimpleCSObject parent) {
		return new SimpleCSIntro(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSItem createSimpleCSItem(ISimpleCSObject parent) {
		return new SimpleCSItem(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSOnCompletion(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSOnCompletion createSimpleCSOnCompletion(
			ISimpleCSObject parent) {
		return new SimpleCSOnCompletion(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSPerformWhen(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSPerformWhen createSimpleCSPerformWhen(ISimpleCSObject parent) {
		return new SimpleCSPerformWhen(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSRepeatedSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSRepeatedSubItem createSimpleCSRepeatedSubItem(
			ISimpleCSObject parent) {
		return new SimpleCSRepeatedSubItem(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSSubItem createSimpleCSSubItem(ISimpleCSObject parent) {
		return new SimpleCSSubItem(fModel);
	}

	/**
	 * @param name
	 * @return
	 */
	private IDocumentNode createGeneric(String name) {
		return new DocumentGenericNode(name);
	}	
	
}
