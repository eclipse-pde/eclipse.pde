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

package org.eclipse.pde.internal.ua.core.cheatsheet.simple.text;

import org.eclipse.pde.internal.core.text.DocumentNodeFactory;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.text.plugin.DocumentGenericNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSAction;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConditionalSubItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSOnCompletion;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItem;

public class SimpleCSDocumentFactory extends DocumentNodeFactory implements
		ISimpleCSModelFactory {

	private SimpleCSModel fModel;

	/**
	 * @param model
	 */
	public SimpleCSDocumentFactory(SimpleCSModel model) {
		super();
		fModel = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.DocumentNodeFactory#createDocumentTextNode
	 * (java.lang.String,
	 * org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	public IDocumentTextNode createDocumentTextNode(String content,
			IDocumentElementNode parent) {
		IDocumentTextNode textNode = new SimpleCSDocumentTextNode();
		textNode.setEnclosingElement(parent);
		parent.addTextNode(textNode);
		textNode.setText(content);
		return textNode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createDocumentNode
	 * (java.lang.String,
	 * org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	public IDocumentElementNode createDocumentNode(String name,
			IDocumentElementNode parent) {

		// Semantics:
		// org.eclipse.platform.doc.isv/reference/extension-points/cheatSheetContentFileSpec.html

		// TODO: MP: TEO: MED: Change factory interface - Parent is not needed
		// as it is set in the DocumentHandler
		// TODO: MP: TEO: MED: Change to interfaces for checking instance of and
		// cast
		// TODO: MP: TEO: LOW: Prioritize "if" order

		if (parent == null) {
			if (isSimpleCS(name)) {
				// Root
				return (IDocumentElementNode) createSimpleCS();
			}
		} else if (parent instanceof SimpleCS) {
			if (isIntro(name)) {
				// Intro
				return (IDocumentElementNode) createSimpleCSIntro((SimpleCS) parent);
			} else if (isItem(name)) {
				// Item
				return (IDocumentElementNode) createSimpleCSItem((SimpleCS) parent);
			}
		} else if (parent instanceof SimpleCSIntro) {
			if (isDescription(name)) {
				// Description
				return (IDocumentElementNode) createSimpleCSDescription((SimpleCSIntro) parent);
			}
		} else if (parent instanceof SimpleCSItem) {
			if (isDescription(name)) {
				// Description
				return (IDocumentElementNode) createSimpleCSDescription((SimpleCSItem) parent);
			} else if (isAction(name)) {
				// Action
				return (IDocumentElementNode) createSimpleCSAction((SimpleCSItem) parent);
			} else if (isCommand(name)) {
				// Command
				return (IDocumentElementNode) createSimpleCSCommand((SimpleCSItem) parent);
			} else if (isPerformWhen(name)) {
				// Perform When
				return (IDocumentElementNode) createSimpleCSPerformWhen((SimpleCSItem) parent);
			} else if (isSubitem(name)) {
				// Subitem
				return (IDocumentElementNode) createSimpleCSSubItem((SimpleCSItem) parent);
			} else if (isConditionalSubitem(name)) {
				// Conditional Subitem
				return (IDocumentElementNode) createSimpleCSConditionalSubItem((SimpleCSItem) parent);
			} else if (isRepeatedSubitem(name)) {
				// Repeated Subitem
				return (IDocumentElementNode) createSimpleCSRepeatedSubItem((SimpleCSItem) parent);
			} else if (isOnCompletion(name)) {
				// On Completion
				return (IDocumentElementNode) createSimpleCSOnCompletion((SimpleCSItem) parent);
			}
		} else if (parent instanceof SimpleCSSubItem) {
			if (isPerformWhen(name)) {
				// Perform When
				return (IDocumentElementNode) createSimpleCSPerformWhen((SimpleCSSubItem) parent);
			} else if (isAction(name)) {
				// Action
				return (IDocumentElementNode) createSimpleCSAction((SimpleCSSubItem) parent);
			} else if (isCommand(name)) {
				// Command
				return (IDocumentElementNode) createSimpleCSCommand((SimpleCSSubItem) parent);
			}
		} else if (parent instanceof SimpleCSConditionalSubItem) {
			if (isSubitem(name)) {
				// Subitem
				return (IDocumentElementNode) createSimpleCSSubItem((SimpleCSConditionalSubItem) parent);
			}
		} else if (parent instanceof SimpleCSRepeatedSubItem) {
			if (isSubitem(name)) {
				// Subitem
				return (IDocumentElementNode) createSimpleCSSubItem((SimpleCSRepeatedSubItem) parent);
			}
		} else if (parent instanceof SimpleCSPerformWhen) {
			if (isAction(name)) {
				// Action
				return (IDocumentElementNode) createSimpleCSAction((SimpleCSPerformWhen) parent);
			} else if (isCommand(name)) {
				// Command
				return (IDocumentElementNode) createSimpleCSCommand((SimpleCSPerformWhen) parent);
			}
		} else if (parent instanceof SimpleCSDescription) {
			if (isBr(name)) {
				// Br
				return createBr();
			}
		} else if (parent instanceof SimpleCSOnCompletion) {
			if (isBr(name)) {
				// Br
				return createBr();
			}
		}
		// Description has no children
		// Action has no children
		// Command has no children
		// OnCompletion has no children
		return super.createDocumentNode(name, parent);
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

	/**
	 * @param name
	 * @return
	 */
	private boolean isBr(String name) {
		return isCSElement(name, ISimpleCSConstants.ELEMENT_BR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCS()
	 */
	public ISimpleCS createSimpleCS() {
		return new SimpleCS(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCSAction
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSAction createSimpleCSAction(ISimpleCSObject parent) {
		return new SimpleCSAction(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCSCommand
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSCommand createSimpleCSCommand(ISimpleCSObject parent) {
		return new SimpleCSCommand(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCSConditionalSubItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSConditionalSubItem createSimpleCSConditionalSubItem(
			ISimpleCSObject parent) {
		return new SimpleCSConditionalSubItem(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCSDescription
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSDescription createSimpleCSDescription(ISimpleCSObject parent) {
		return new SimpleCSDescription(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCSIntro
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSIntro createSimpleCSIntro(ISimpleCSObject parent) {
		return new SimpleCSIntro(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCSItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSItem createSimpleCSItem(ISimpleCSObject parent) {
		return new SimpleCSItem(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCSOnCompletion
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSOnCompletion createSimpleCSOnCompletion(
			ISimpleCSObject parent) {
		return new SimpleCSOnCompletion(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCSPerformWhen
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSPerformWhen createSimpleCSPerformWhen(ISimpleCSObject parent) {
		return new SimpleCSPerformWhen(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCSRepeatedSubItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSRepeatedSubItem createSimpleCSRepeatedSubItem(
			ISimpleCSObject parent) {
		return new SimpleCSRepeatedSubItem(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModelFactory
	 * #createSimpleCSSubItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSSubItem createSimpleCSSubItem(ISimpleCSObject parent) {
		return new SimpleCSSubItem(fModel);
	}

	/**
	 * @return
	 */
	protected IDocumentElementNode createBr() {
		return new DocumentGenericNode(ISimpleCSConstants.ELEMENT_BR) {
			private static final long serialVersionUID = 1L;

			public boolean isLeafNode() {
				return true;
			}
		};
	}

}
