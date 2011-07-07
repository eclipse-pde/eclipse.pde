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

package org.eclipse.pde.internal.core.builders;

import java.util.HashMap;
import java.util.HashSet;
import org.eclipse.pde.internal.core.ischema.*;
import org.w3c.dom.*;

/**
 * XMLElementProposalComputer
 *
 */
public class ElementOccurenceChecker {

	/**
	 * @param sElement
	 * @param element
	 * @return A set of elements that are first-level children of 
	 * <code>element</code>, that violate max occurence rules defined by 
	 * <code>sElement</code>.
	 */
	public static HashSet findMaxOccurenceViolations(ISchemaElement sElement, Element element) {
		// Calculate the number of occurrences of each XML tag name
		// in the node's direct children
		HashMap tagNameMap = countXMLChildrenByTagName(element);
		return processChildrenMax(sElement, tagNameMap, element);
	}

	/**
	 * @param sElement
	 * @param element
	 * @return A set of elements that are first-level children of 
	 * <code>element</code>, that violate min occurence rules defined by 
	 * <code>sElement</code>.
	 */
	public static HashSet findMinOccurenceViolations(ISchemaElement sElement, Element element) {
		// Calculate the number of occurrences of each XML tag name
		// in the node's direct children
		HashMap tagNameMap = countXMLChildrenByTagName(element);
		return processChildrenMin(sElement, tagNameMap);
	}

	/**
	 * @param element
	 * @return A hash containing singleton entries of node's children mapped
	 * against the number of occurrences found
	 * Key is children's XML tag name
	 * Value is number of occurrences found amongst siblings
	 */
	private static HashMap countXMLChildrenByTagName(Element element) {
		NodeList children = element.getChildNodes();
		HashMap tagNameMap = new HashMap();

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String key = child.getNodeName();
				if (tagNameMap.containsKey(key)) {
					int value = ((Integer) tagNameMap.get(key)).intValue();
					value++;
					tagNameMap.put(key, new Integer(value));
				} else {
					tagNameMap.put(key, new Integer(1));
				}
			}
		}

		return tagNameMap;
	}

	private static HashSet processChildrenMax(ISchemaElement sElement, HashMap tagNameMap, Element element) {
		HashSet elementSet = new HashSet();
		// Get this element's compositor
		ISchemaCompositor compositor = ((ISchemaComplexType) sElement.getType()).getCompositor();
		// Track multiplicity
		int multiplicityTracker = 1;
		// Process the compositor
		processCompositorMax(compositor, elementSet, tagNameMap, multiplicityTracker, element);
		return elementSet;
	}

	private static HashSet processChildrenMin(ISchemaElement sElement, HashMap tagNameMap) {
		HashSet elementSet = new HashSet();
		// Get this element's compositor
		ISchemaCompositor compositor = ((ISchemaComplexType) sElement.getType()).getCompositor();
		// Track multiplicity
		int multiplicityTracker = 1;
		// Process the compositor
		processCompositorMin(compositor, elementSet, tagNameMap, multiplicityTracker);
		return elementSet;
	}

	private static void processCompositorMin(ISchemaCompositor compositor, HashSet elementSet, HashMap siblings, int multiplicityTracker) {
		// Compositor can be null only in cases where we had a schema complex
		// type but that complex type was complex because it had attributes
		// rather than element children
		// All we care about is choices and sequences (Alls and groups not 
		// supported)		
		if (compositor == null) {
			return;
		} else if (compositor.getKind() == ISchemaCompositor.CHOICE) {
			processChoiceMin(compositor, elementSet, siblings, multiplicityTracker);
		} else if (compositor.getKind() == ISchemaCompositor.SEQUENCE) {
			processSequenceMin(compositor, elementSet, siblings, multiplicityTracker);
		}
	}

	private static void processCompositorMax(ISchemaCompositor compositor, HashSet elementSet, HashMap siblings, int multiplicityTracker, Element element) {
		// Compositor can be null only in cases where we had a schema complex
		// type but that complex type was complex because it had attributes
		// rather than element children
		// All we care about is choices and sequences (Alls and groups not 
		// supported)		
		if (compositor == null) {
			return;
		} else if (compositor.getKind() == ISchemaCompositor.CHOICE) {
			processChoiceMax(compositor, elementSet, siblings, multiplicityTracker, element);
		} else if (compositor.getKind() == ISchemaCompositor.SEQUENCE) {
			processSequenceMax(compositor, elementSet, siblings, multiplicityTracker, element);
		}
	}

	private static void processSequenceMin(ISchemaCompositor compositor, HashSet elementSet, HashMap siblings, int multiplicityTracker) {
		ISchemaObject[] schemaObject = compositor.getChildren();
		// Unbounded min occurs are represented by the maximum integer value
		if (multiplicityTracker < Integer.MAX_VALUE) {
			// Multiply the min occurs amount to the overall multiplicity
			multiplicityTracker = compositor.getMinOccurs() * multiplicityTracker;
		}
		// Process the compositors children
		for (int i = 0; i < compositor.getChildCount(); i++) {
			processObjectMin(schemaObject[i], elementSet, siblings, multiplicityTracker);
		}
	}

	private static void processSequenceMax(ISchemaCompositor compositor, HashSet elementSet, HashMap siblings, int multiplicityTracker, Element element) {
		ISchemaObject[] schemaObject = compositor.getChildren();
		// Unbounded max occurs are represented by the maximum integer value
		if (multiplicityTracker < Integer.MAX_VALUE) {
			// Multiply the max occurs amount to the overall multiplicity
			multiplicityTracker = compositor.getMaxOccurs() * multiplicityTracker;
		}
		// Process the compositors children
		for (int i = 0; i < compositor.getChildCount(); i++) {
			processObjectMax(schemaObject[i], elementSet, siblings, multiplicityTracker, element);
		}
	}

	private static void processChoiceMin(ISchemaCompositor compositor, HashSet elementSet, HashMap siblings, int multiplicityTracker) {
		// Unbounded min occurs are represented by the maximum integer value
		if (multiplicityTracker < Integer.MAX_VALUE) {
			// Multiply the min occurs amount to the overall multiplicity
			multiplicityTracker = compositor.getMinOccurs() * multiplicityTracker;
		}
		adjustChoiceMinSiblings(compositor, siblings);

		ISchemaObject[] schemaObject = compositor.getChildren();
		// Process the compositors children
		for (int i = 0; i < compositor.getChildCount(); i++) {
			processObjectMin(schemaObject[i], elementSet, siblings, multiplicityTracker);
		}
	}

	private static void processChoiceMax(ISchemaCompositor compositor, HashSet elementSet, HashMap siblings, int multiplicityTracker, Element element) {
		// Unbounded max occurs are represented by the maximum integer value
		if (multiplicityTracker < Integer.MAX_VALUE) {
			// Multiply the max occurs amount to the overall multiplicity
			multiplicityTracker = compositor.getMaxOccurs() * multiplicityTracker;
		}
		adjustChoiceMaxSiblings(compositor, siblings);

		ISchemaObject[] schemaObject = compositor.getChildren();
		// Process the compositors children
		for (int i = 0; i < compositor.getChildCount(); i++) {
			processObjectMax(schemaObject[i], elementSet, siblings, multiplicityTracker, element);
		}
	}

	private static void adjustChoiceMaxSiblings(ISchemaCompositor compositor, HashMap siblings) {
		if (isSimpleChoice(compositor)) {
			// Supported
			// Update all child element occurrences of the choice compositor
			// to the number of occurrences found
			// Each choice occurrence counts as one occurrence for all child elements
			// of that choice			
			int childElementCount = countChoiceElementChildren(compositor, siblings);
			updateChoiceElementChildren(compositor, siblings, childElementCount);
		} else {
			// Not supported
			// IMPORTANT:  Any child of choice that is not an element (e.g.
			// sequence, choice) is not supported, in future could recursively
			// calculate, but time vs benefit is not worth it
			// Remove all elements nested in compositors from validation check
			// by setting their occurrences to integer MIN
			updateChoiceElementChildren(compositor, siblings, Integer.MIN_VALUE);
		}
	}

	private static boolean isSimpleChoice(ISchemaCompositor compositor) {
		ISchemaObject[] schemaObject = compositor.getChildren();
		// Simple choice compositors only have elements as children
		// Complex choice compositors have one or more choice or sequence
		// compositors as children
		for (int i = 0; i < compositor.getChildCount(); i++) {
			if (schemaObject[i] instanceof ISchemaCompositor) {
				return false;
			}
		}
		return true;
	}

	private static void adjustChoiceMinSiblings(ISchemaCompositor compositor, HashMap siblings) {

		if (isSimpleChoice(compositor)) {
			// Supported
			// Update all child element occurrences of the choice compositor
			// to the number of occurrences found
			// Each choice occurrence counts as one occurrence for all child elements
			// of that choice			
			int childElementCount = countChoiceElementChildren(compositor, siblings);
			updateChoiceElementChildren(compositor, siblings, childElementCount);
		} else {
			// Not supported
			// IMPORTANT:  Any child of choice that is not an element (e.g.
			// sequence, choice) is not supported, in future could recursively
			// caculate, but time vs benefit is not worth it
			// Remove all elements nested in compositors from validation check
			// by setting their occurrences to integer MAX
			updateChoiceElementChildren(compositor, siblings, Integer.MAX_VALUE);
		}
	}

	private static int countChoiceElementChildren(ISchemaCompositor compositor, HashMap siblings) {
		ISchemaObject[] schemaObject = compositor.getChildren();
		// Count the number of child element occurrences of the choice
		// Compositor
		int childElementCount = 0;
		for (int i = 0; i < compositor.getChildCount(); i++) {
			if (schemaObject[i] instanceof ISchemaElement) {
				String name = schemaObject[i].getName();
				if (siblings.containsKey(name)) {
					int occurences = ((Integer) siblings.get(name)).intValue();
					if (childElementCount < Integer.MAX_VALUE) {
						childElementCount = childElementCount + occurences;
					}
				}
			}
		}
		return childElementCount;
	}

	private static void updateChoiceElementChildren(ISchemaCompositor compositor, HashMap siblings, int childElementCount) {
		ISchemaObject[] schemaObject = compositor.getChildren();
		for (int i = 0; i < compositor.getChildCount(); i++) {
			if (schemaObject[i] instanceof ISchemaElement) {
				String name = schemaObject[i].getName();
				siblings.put(name, new Integer(childElementCount));
			} else if (schemaObject[i] instanceof ISchemaCompositor) {
				updateChoiceElementChildren((ISchemaCompositor) schemaObject[i], siblings, childElementCount);
			}
		}
	}

	private static void processObjectMax(ISchemaObject schemaObject, HashSet elementSet, HashMap siblings, int multiplicityTracker, Element element) {
		if (schemaObject instanceof ISchemaElement) {
			ISchemaElement schemaElement = (ISchemaElement) schemaObject;
			Element childElement = findChildElement(element, schemaElement.getName());
			if (childElement != null) {
				processElementMax(schemaElement, elementSet, siblings, multiplicityTracker, childElement);
			}
		} else if (schemaObject instanceof ISchemaCompositor) {
			ISchemaCompositor sCompositor = (ISchemaCompositor) schemaObject;
			processCompositorMax(sCompositor, elementSet, siblings, multiplicityTracker, element);
		}
	}

	private static void processObjectMin(ISchemaObject schemaObject, HashSet elementSet, HashMap siblings, int multiplicityTracker) {
		if (schemaObject instanceof ISchemaElement) {
			ISchemaElement schemaElement = (ISchemaElement) schemaObject;
			processElementMin(schemaElement, elementSet, siblings, multiplicityTracker);
		} else if (schemaObject instanceof ISchemaCompositor) {
			ISchemaCompositor sCompositor = (ISchemaCompositor) schemaObject;
			processCompositorMin(sCompositor, elementSet, siblings, multiplicityTracker);
		}
	}

	private static void processElementMax(ISchemaElement schemaElement, HashSet elementSet, HashMap siblings, int multiplicityTracker, Element element) {

		int occurrences = 0;
		String name = schemaElement.getName();
		// Determine the number of occurrences found of this element
		if (siblings.containsKey(name)) {
			occurrences = ((Integer) siblings.get(schemaElement.getName())).intValue();
		}
		// Determine if the elements max occurrences is respected
		if (multiplicityTracker < Integer.MAX_VALUE) {
			multiplicityTracker = schemaElement.getMaxOccurs() * multiplicityTracker;
		}
		// If a given element occurs more than the tracked max occurs, add
		// it to the list
		// Note:  This is a simple calculation that does not address all complex
		// XML Schema multiplity rules.  For instance, multiple layers of
		// choices and sequences compositors coupled with varying siblings
		// elements require a regex processor
		// For the PDE space this is not required as extension point schemas
		// are always very simple
		if (occurrences > multiplicityTracker) {
			elementSet.add(new ElementOccurrenceResult(element, schemaElement, occurrences, multiplicityTracker));
		}
	}

	private static void processElementMin(ISchemaElement schemaElement, HashSet elementSet, HashMap siblings, int multiplicityTracker) {

		int occurrences = 0;
		String name = schemaElement.getName();
		// Determine the number of occurrences found of this element
		if (siblings.containsKey(name)) {
			occurrences = ((Integer) siblings.get(schemaElement.getName())).intValue();
		}
		// Determine if the elements min occurrences is respected
		if (multiplicityTracker < Integer.MAX_VALUE) {
			multiplicityTracker = schemaElement.getMinOccurs() * multiplicityTracker;
		}
		// If a given element occurs les than the tracked min occurs, add
		// it to the list
		// Note:  This is a simple calculation that does not address all complex
		// XML Schema multiplity rules.  For instance, multiple layers of
		// choices and sequences compositors coupled with varying siblings
		// elements require a regex processor
		// For the PDE space this is not required as extension point schemas
		// are always very simple
		if (occurrences < multiplicityTracker) {
			elementSet.add(new ElementOccurrenceResult(null, schemaElement, occurrences, multiplicityTracker));
		}
	}

	private static Element findChildElement(Element element, String name) {
		NodeList children = element.getChildNodes();
		Element match = null;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String key = child.getNodeName();
				if (key.equals(name)) {
					// Normally we would return as soon as an matching element
					// is found; however, we want to return the last 
					// occurrence at the expense of performance in order to 
					// flag the last element exceeding allowed maximum
					// occurrence
					match = (Element) child;
				}
			}
		}
		return match;
	}

}
