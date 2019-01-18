/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TargetDefinitionDocumentTools {

	/**
	 * From the given parent, follows the path of given names and returns the last
	 * element in the path, if any element in the given path does not exist it is
	 * created
	 *
	 * @param parent
	 *                       root of the name path
	 * @param childNames
	 *                       path of element names leading to the desired element
	 * @return final element in the given path
	 */
	public static Element getChildElement(Element parent, String... childNames) {
		if (parent != null && childNames.length > 0) {
			Element childNode = null;
			NodeList list = parent.getChildNodes();
			for (int i = 0; i < list.getLength(); ++i) {
				Node node = list.item(i);
				if (node instanceof Element && node.getNodeName().equalsIgnoreCase(childNames[0])) {
					childNode = (Element) node;
					break;
				}
			}
			if (childNode == null) {
				Document parentDocument = parent.getOwnerDocument();
				childNode = parentDocument.createElement(childNames[0]);
				addChildWithIndent(parent, childNode);
			}
			if (childNames.length > 1) {
				return getChildElement(childNode, Arrays.copyOfRange(childNames, 1, childNames.length));
			}
			return childNode;
		}
		return null;
	}

	public static void addChildWithIndent(Node parent, Node child) {
		Node lastChild = parent.getLastChild();
		if (isWhitespaceNode(lastChild)) {
			parent.removeChild(lastChild);
		}
		appendTextNode(parent, true);
		parent.appendChild(child);
		// Recursively adds children with indents
		final int initalLength = child.getChildNodes().getLength();
		int editIndex = 0;
		for (int i = 0; i < initalLength; i++) {
			Node node = child.getChildNodes().item(editIndex);
			if (node instanceof Element) {
				addChildWithIndent(child, child.removeChild(node));
			} else {
				editIndex++;
			}
		}
		appendTextNode(parent, false);
	}

	/**
	 * For all the children of the parentElement, if any of the oldElements are not
	 * found in the newElements list then they are removed. All newElements that are
	 * not found in the oldElements are then added to the parentElement
	 *
	 * @param parentElement
	 *                          The parent element
	 * @param oldElements
	 *                          List of all children of the parent element that
	 *                          should be removed if not in newElements
	 * @param newElements
	 *                          List of elements wished to be found in parentElement
	 */
	public static void updateElements(Element parentElement, List<Element> oldElements, List<Element> newElements,
			Comparator<Element> comparator) {
		if (oldElements == null) {
			oldElements = new ArrayList<>();
			NodeList nodes = parentElement.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node instanceof Element) {
					oldElements.add((Element) node);
				}
			}
		}
		for (Element element : oldElements) {
			boolean matchFound = false;
			if (comparator != null) {
				for (int j = 0; j < newElements.size(); j++) {
					if (comparator.compare(element, newElements.get(j)) == 0) {
						if (elementAttributesComparator.compare(element, newElements.get(j)) != 0) {
							parentElement.replaceChild(
									parentElement.getOwnerDocument().importNode(newElements.get(j), true), element);
						}
						newElements.remove(j);
						matchFound = true;
						break;
					}
				}
			}
			if (!matchFound) {
				removeChildAndWhitespace(element);
			}
		}
		if (!newElements.isEmpty()) {
			Node lastChild = parentElement.getLastChild();
			if (isWhitespaceNode(lastChild)) {
				parentElement.removeChild(lastChild);
			}
			newElements.stream().forEach(element -> {
				TargetDefinitionDocumentTools.addChildWithIndent(parentElement,
						parentElement.getOwnerDocument().importNode(element, true));
			});
		}
	}

	private static void appendTextNode(Node parent, boolean addIndent) {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.ui.editors"); //$NON-NLS-1$
		boolean spacesForTabs = preferences.getBoolean("spacesForTabs", false); //$NON-NLS-1$

		String newLineString = "\n"; //$NON-NLS-1$
		if (addIndent) {
			if (spacesForTabs) {
				int tabWidth = preferences.getInt("tabWidth", 4); //$NON-NLS-1$
				char[] chars = new char[tabWidth];
				Arrays.fill(chars, ' ');
				newLineString += new String(chars);
			} else {
				newLineString += "\t"; //$NON-NLS-1$
			}
		}
		Node previousSibling = parent.getPreviousSibling();
		if (isWhitespaceNode(previousSibling)) {
			String textContentString = previousSibling.getTextContent();
			int indexOfLastNewLine = Math.max(textContentString.lastIndexOf('\n'), textContentString.lastIndexOf('\r'));
			if (indexOfLastNewLine >= 0) {
				newLineString += textContentString.substring(indexOfLastNewLine + 1);
			} else {
				newLineString += textContentString;
			}
		}
		parent.appendChild(parent.getOwnerDocument().createTextNode(newLineString));
	}

	/**
	 * From the given parent, follows the path of given names and delete the element
	 * matching the final name if any of the child names have no children after the
	 * final is removed, it too is removed.
	 *
	 * @param parent
	 *                       root of the name path
	 * @param childNames
	 *                       path of element names leading to the desired element
	 * @return true if the parent element has also been deleted
	 */
	public static boolean removeElement(Element parent, String... childNames) {
		if (parent != null && childNames.length > 0) {
			Element childNode = null;
			boolean parentHasOtherChildren = false;
			NodeList list = parent.getChildNodes();
			for (int i = 0; i < list.getLength(); ++i) {
				Node node = list.item(i);
				if (node instanceof Element) {
					if (childNode == null && node.getNodeName().equalsIgnoreCase(childNames[0])) {
						childNode = (Element) node;
						if (parentHasOtherChildren) {
							break;
						}
					} else {
						parentHasOtherChildren = true;
						if (childNode != null) {
							break;
						}
					}
				}
			}
			boolean doesFirstChildExist = childNode != null;
			if (doesFirstChildExist) {
				if (childNames.length > 1) {
					doesFirstChildExist = !removeElement(childNode,
							Arrays.copyOfRange(childNames, 1, childNames.length));
				} else {
					removeChildAndWhitespace(childNode);
					doesFirstChildExist = false;
				}
			}
			if (!parentHasOtherChildren && !doesFirstChildExist
					&& parent != parent.getOwnerDocument().getDocumentElement()) {
				if (parent.getParentNode() != null) {
					removeChildAndWhitespace(parent);
				}
				return true;
			}
			return false;
		}
		return false;
	}

	private static Comparator<Element> elementAttributesComparator = (o1, o2) -> {
		// compare attributes
		NamedNodeMap expectedAttrs = o1.getAttributes();
		NamedNodeMap actualAttrs = o2.getAttributes();
		if (expectedAttrs.getLength() != actualAttrs.getLength()) {
			return expectedAttrs.getLength() - actualAttrs.getLength();
		}
		for (int i = 0; i < expectedAttrs.getLength(); i++) {
			Attr expectedAttr = (Attr) expectedAttrs.item(i);
			Attr actualAttr = null;
			if (expectedAttr.getNamespaceURI() == null) {
				actualAttr = (Attr) actualAttrs.getNamedItem(expectedAttr.getName());
			} else {
				actualAttr = (Attr) actualAttrs.getNamedItemNS(expectedAttr.getNamespaceURI(),
						expectedAttr.getLocalName());
			}
			if (actualAttr == null) {
				return -1;
			}
			int comparedValue = expectedAttr.getValue().compareTo(actualAttr.getValue());
			if (comparedValue != 0) {
				return comparedValue;
			}
		}
		return 0;
	};

	/**
	 * Removes a child from it's parent node along with any leading whitespace found
	 * between it and it's previous sibling
	 *
	 * @param child
	 *                  Node to be removed
	 */
	public static void removeChildAndWhitespace(Node child) {
		if (child.getParentNode() == null) {
			return;
		}
		Node previousNode = child.getPreviousSibling();
		if (isWhitespaceNode(previousNode)) {
			child.getParentNode().removeChild(previousNode);
		}
		child.getParentNode().removeChild(child);
	}

	private static boolean isWhitespaceNode(Node node) {
		return node != null && node.getNodeType() == Node.TEXT_NODE && node.getTextContent().matches("[\\n\\r\\s]+"); //$NON-NLS-1$
	}
}
