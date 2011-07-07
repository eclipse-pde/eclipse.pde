/*******************************************************************************
 *  Copyright (c) 2006, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.HashMap;
import java.util.TreeSet;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

public class XMLElementProposalComputer {

	/**
	 * @param sElement
	 * @param node
	 * @return A set of elements that can be added as children to element,
	 * <code>node</code>, as described by schema element, <code>sElement</code>, 
	 * given multiplicity constraints and existing children found under 
	 * <code>node</code>.
	 */
	public static TreeSet computeElementProposal(ISchemaElement sElement, IDocumentElementNode node) {
		// Calculate the number of occurrences of each XML tag name
		// in the node's direct children
		HashMap tagNameMap = countXMLChildrenByTagName(node);
		return computeElementProposal(sElement, tagNameMap);
	}

	private static TreeSet computeElementProposal(ISchemaElement sElement, HashMap tagNameMap) {

		TreeSet elementSet = new TreeSet(new XMLElementProposalComparator());
		// Get this element's compositor
		ISchemaCompositor compositor = ((ISchemaComplexType) sElement.getType()).getCompositor();
		// Track multiplicity
		int multiplicityTracker = 1;
		// Process the compositor
		computeCompositorChildProposal(compositor, elementSet, tagNameMap, multiplicityTracker);
		return elementSet;
	}

	/**
	 * @param node
	 * @return A hash containing singleton entries of node's children mapped
	 * against the number of occurrences found
	 * Key is children's XML tag name
	 * Value is number of occurrences found amongst siblings
	 */
	private static HashMap countXMLChildrenByTagName(IDocumentElementNode node) {
		IDocumentElementNode[] children = node.getChildNodes();
		HashMap tagNameMap = new HashMap();
		for (int i = 0; i < children.length; i++) {
			String key = children[i].getXMLTagName();
			if (tagNameMap.containsKey(key)) {
				int value = ((Integer) tagNameMap.get(key)).intValue();
				value++;
				tagNameMap.put(key, new Integer(value));
			} else {
				tagNameMap.put(key, new Integer(1));
			}
		}
		return tagNameMap;
	}

	/**
	 * @param compositor
	 * @param proposalList
	 * @param siblings
	 * @param multiplicityTracker
	 */
	private static void computeCompositorChildProposal(ISchemaCompositor compositor, TreeSet elementSet, HashMap siblings, int multiplicityTracker) {
		// Compositor can be null only in cases where we had a schema complex
		// type but that complex type was complex because it had attributes
		// rather than element children
		// All we care about is choices and sequences (Alls and groups not 
		// supported)		
		if (compositor == null) {
			return;
		} else if (compositor.getKind() == ISchemaCompositor.CHOICE) {
			computeCompositorChoiceProposal(compositor, elementSet, siblings, multiplicityTracker);
		} else if (compositor.getKind() == ISchemaCompositor.SEQUENCE) {
			computeCompositorSequenceProposal(compositor, elementSet, siblings, multiplicityTracker);
		}
	}

	/**
	 * @param compositor
	 * @param elementSet
	 * @param siblings
	 * @param multiplicityTracker
	 */
	private static void computeCompositorSequenceProposal(ISchemaCompositor compositor, TreeSet elementSet, HashMap siblings, int multiplicityTracker) {

		ISchemaObject[] schemaObject = compositor.getChildren();
		// Unbounded max occurs are represented by the maximum integer value
		if (multiplicityTracker < Integer.MAX_VALUE) {
			// Multiply the max occurs amount to the overall multiplicity
			multiplicityTracker = compositor.getMaxOccurs() * multiplicityTracker;
		}
		// Process the compositors children
		for (int i = 0; i < compositor.getChildCount(); i++) {
			computeObjectChildProposal(schemaObject[i], elementSet, siblings, multiplicityTracker);
		}
	}

	/**
	 * @param compositor
	 * @param elementSet
	 * @param siblings
	 * @param multiplicityTracker
	 */
	private static void computeCompositorChoiceProposal(ISchemaCompositor compositor, TreeSet elementSet, HashMap siblings, int multiplicityTracker) {

		// Unbounded max occurs are represented by the maximum integer value
		if (multiplicityTracker < Integer.MAX_VALUE) {
			// Multiply the max occurs amount to the overall multiplicity
			multiplicityTracker = compositor.getMaxOccurs() * multiplicityTracker;
		}
		adjustChoiceSiblings(compositor, siblings);

		ISchemaObject[] schemaObject = compositor.getChildren();
		// Process the compositors children
		for (int i = 0; i < compositor.getChildCount(); i++) {
			computeObjectChildProposal(schemaObject[i], elementSet, siblings, multiplicityTracker);
		}
	}

	/**
	 * @param compositor
	 * @param siblings
	 */
	private static void adjustChoiceSiblings(ISchemaCompositor compositor, HashMap siblings) {

		ISchemaObject[] schemaObject = compositor.getChildren();
		// Count the number of child element occurrences of the choice
		// Compositor
		int childElementCount = 0;
		for (int i = 0; i < compositor.getChildCount(); i++) {
			if (schemaObject[i] instanceof ISchemaElement) {
				String name = schemaObject[i].getName();
				if (siblings.containsKey(name)) {
					int occurences = ((Integer) siblings.get(name)).intValue();
					childElementCount = childElementCount + occurences;
				}
			}
		}
		// Update all child element occurrences of the choice compositor
		// to the number of occurences found
		// Each choice occurence counts as one occurence for all child elements
		// of that choice
		// IMPORTANT:  Any child of choice that is not an element (e.g.
		// sequence, choice) is not supported, in future could recursively
		// caculate, but time vs benefit is not worth it
		for (int i = 0; i < compositor.getChildCount(); i++) {
			if (schemaObject[i] instanceof ISchemaElement) {
				String name = schemaObject[i].getName();
				siblings.put(name, new Integer(childElementCount));
			}
		}
	}

	/**
	 * @param schemaObject
	 * @param proposalList
	 * @param siblings
	 * @param multiplicityTracker
	 */
	private static void computeObjectChildProposal(ISchemaObject schemaObject, TreeSet elementSet, HashMap siblings, int multiplicityTracker) {
		if (schemaObject instanceof ISchemaElement) {
			ISchemaElement schemaElement = (ISchemaElement) schemaObject;
			computeElementChildProposal(schemaElement, elementSet, siblings, multiplicityTracker);
		} else if (schemaObject instanceof ISchemaCompositor) {
			ISchemaCompositor sCompositor = (ISchemaCompositor) schemaObject;
			computeCompositorChildProposal(sCompositor, elementSet, siblings, multiplicityTracker);
		}
	}

	/**
	 * @param schemaElement
	 * @param proposalList
	 * @param siblings
	 * @param multiplicityTracker
	 */
	private static void computeElementChildProposal(ISchemaElement schemaElement, TreeSet elementSet, HashMap siblings, int multiplicityTracker) {

		int occurrences = 0;
		// Determine the number of occurrences found of this element
		if (siblings.containsKey(schemaElement.getName())) {
			occurrences = ((Integer) siblings.get(schemaElement.getName())).intValue();
		}
		// Determine if the elements max occurrences is respected
		if (multiplicityTracker < Integer.MAX_VALUE) {
			multiplicityTracker = schemaElement.getMaxOccurs() * multiplicityTracker;
		}
		// Only add a new proposal for a given element if it has not exceeded 
		// the multiplicity
		// Note:  This is a simple calculation that does not address all complex
		// XML Schema multiplity rules.  For instance, multiple layers of
		// choices and sequences compositors coupled with varying siblings
		// elements require a regex processor
		// For the PDE space this is not required as extension point schemas
		// are always very simple
		if (occurrences < multiplicityTracker) {
			elementSet.add(schemaElement);
		}
	}
}
