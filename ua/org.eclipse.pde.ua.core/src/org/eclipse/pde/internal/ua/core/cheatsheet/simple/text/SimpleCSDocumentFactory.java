/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.core.cheatsheet.simple.text;

import org.eclipse.pde.internal.core.text.DocumentNodeFactory;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.text.plugin.DocumentGenericNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.*;

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

	@Override
	public IDocumentTextNode createDocumentTextNode(String content,
			IDocumentElementNode parent) {
		IDocumentTextNode textNode = new SimpleCSDocumentTextNode();
		textNode.setEnclosingElement(parent);
		parent.addTextNode(textNode);
		textNode.setText(content);
		return textNode;
	}

	@Override
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
				return createSimpleCS();
			}
		} else if (parent instanceof SimpleCS) {
			if (isIntro(name)) {
				// Intro
				return createSimpleCSIntro((SimpleCS) parent);
			} else if (isItem(name)) {
				// Item
				return createSimpleCSItem((SimpleCS) parent);
			}
		} else if (parent instanceof SimpleCSIntro) {
			if (isDescription(name)) {
				// Description
				return createSimpleCSDescription((SimpleCSIntro) parent);
			}
		} else if (parent instanceof SimpleCSItem) {
			if (isDescription(name)) {
				// Description
				return createSimpleCSDescription((SimpleCSItem) parent);
			} else if (isAction(name)) {
				// Action
				return createSimpleCSAction((SimpleCSItem) parent);
			} else if (isCommand(name)) {
				// Command
				return createSimpleCSCommand((SimpleCSItem) parent);
			} else if (isPerformWhen(name)) {
				// Perform When
				return createSimpleCSPerformWhen((SimpleCSItem) parent);
			} else if (isSubitem(name)) {
				// Subitem
				return createSimpleCSSubItem((SimpleCSItem) parent);
			} else if (isConditionalSubitem(name)) {
				// Conditional Subitem
				return createSimpleCSConditionalSubItem((SimpleCSItem) parent);
			} else if (isRepeatedSubitem(name)) {
				// Repeated Subitem
				return createSimpleCSRepeatedSubItem((SimpleCSItem) parent);
			} else if (isOnCompletion(name)) {
				// On Completion
				return createSimpleCSOnCompletion((SimpleCSItem) parent);
			}
		} else if (parent instanceof SimpleCSSubItem) {
			if (isPerformWhen(name)) {
				// Perform When
				return createSimpleCSPerformWhen((SimpleCSSubItem) parent);
			} else if (isAction(name)) {
				// Action
				return createSimpleCSAction((SimpleCSSubItem) parent);
			} else if (isCommand(name)) {
				// Command
				return createSimpleCSCommand((SimpleCSSubItem) parent);
			}
		} else if (parent instanceof SimpleCSConditionalSubItem) {
			if (isSubitem(name)) {
				// Subitem
				return createSimpleCSSubItem((SimpleCSConditionalSubItem) parent);
			}
		} else if (parent instanceof SimpleCSRepeatedSubItem) {
			if (isSubitem(name)) {
				// Subitem
				return createSimpleCSSubItem((SimpleCSRepeatedSubItem) parent);
			}
		} else if (parent instanceof SimpleCSPerformWhen) {
			if (isAction(name)) {
				// Action
				return createSimpleCSAction((SimpleCSPerformWhen) parent);
			} else if (isCommand(name)) {
				// Command
				return createSimpleCSCommand((SimpleCSPerformWhen) parent);
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

	@Override
	public ISimpleCS createSimpleCS() {
		return new SimpleCS(fModel);
	}

	@Override
	public ISimpleCSAction createSimpleCSAction(ISimpleCSObject parent) {
		return new SimpleCSAction(fModel);
	}

	@Override
	public ISimpleCSCommand createSimpleCSCommand(ISimpleCSObject parent) {
		return new SimpleCSCommand(fModel);
	}

	@Override
	public ISimpleCSConditionalSubItem createSimpleCSConditionalSubItem(
			ISimpleCSObject parent) {
		return new SimpleCSConditionalSubItem(fModel);
	}

	@Override
	public ISimpleCSDescription createSimpleCSDescription(ISimpleCSObject parent) {
		return new SimpleCSDescription(fModel);
	}

	@Override
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
	@Override
	public ISimpleCSItem createSimpleCSItem(ISimpleCSObject parent) {
		return new SimpleCSItem(fModel);
	}

	@Override
	public ISimpleCSOnCompletion createSimpleCSOnCompletion(
			ISimpleCSObject parent) {
		return new SimpleCSOnCompletion(fModel);
	}

	@Override
	public ISimpleCSPerformWhen createSimpleCSPerformWhen(ISimpleCSObject parent) {
		return new SimpleCSPerformWhen(fModel);
	}

	@Override
	public ISimpleCSRepeatedSubItem createSimpleCSRepeatedSubItem(
			ISimpleCSObject parent) {
		return new SimpleCSRepeatedSubItem(fModel);
	}

	@Override
	public ISimpleCSSubItem createSimpleCSSubItem(ISimpleCSObject parent) {
		return new SimpleCSSubItem(fModel);
	}

	/**
	 * @return
	 */
	protected IDocumentElementNode createBr() {
		return new DocumentGenericNode(ISimpleCSConstants.ELEMENT_BR) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isLeafNode() {
				return true;
			}
		};
	}

}
