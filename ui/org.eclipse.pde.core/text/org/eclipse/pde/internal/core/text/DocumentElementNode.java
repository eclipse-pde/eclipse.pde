/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.util.PDETextHelper;

public abstract class DocumentElementNode extends DocumentXMLNode implements IDocumentElementNode {

	private static final long serialVersionUID = 1L;

	public static final Charset ATTRIBUTE_VALUE_ENCODING = StandardCharsets.UTF_8;

	public static final String ATTRIBUTE_VALUE_TRUE = "true"; //$NON-NLS-1$

	public static final String ATTRIBUTE_VALUE_FALSE = "false"; //$NON-NLS-1$

	public static final String EMPTY_VALUE = ""; //$NON-NLS-1$

	private transient IDocumentElementNode fParent;
	private transient boolean fIsErrorNode;
	private transient int fLength;
	private transient int fOffset;
	private transient IDocumentElementNode fPreviousSibling;
	private transient int fIndent;

	private final ArrayList<IDocumentElementNode> fChildren;
	private final TreeMap<String, IDocumentAttributeNode> fAttributes;
	private String fTag;
	private IDocumentTextNode fTextNode;

	private String fNamespace = EMPTY_VALUE;
	private String fNamespacePrefix = EMPTY_VALUE;

	// TODO: MP: TEO: LOW: Regenerate comments

	/**
	 *
	 */
	public DocumentElementNode() {
		fParent = null;
		fIsErrorNode = false;
		fLength = -1;
		fOffset = -1;
		fPreviousSibling = null;
		fIndent = 0;

		fChildren = new ArrayList<>();
		fAttributes = new TreeMap<>();
		fTag = null;
		fTextNode = null;
	}

	@Override
	public ArrayList<IDocumentElementNode> getChildNodesList() {
		// Not used by text edit operations
		return fChildren;
	}

	@Override
	public TreeMap<String, IDocumentAttributeNode> getNodeAttributesMap() {
		// Not used by text edit operations
		return fAttributes;
	}

	@Override
	public String writeShallow(boolean terminate) {
		// Used by text edit operations
		StringBuilder buffer = new StringBuilder();
		// Print opening angle bracket
		buffer.append("<"); //$NON-NLS-1$
		// Print namespace
		String prefix = getNamespacePrefix();
		if (prefix != null && prefix.length() > 0) {
			buffer.append(getNamespacePrefix());
			buffer.append(":"); //$NON-NLS-1$
		}
		// Print element
		buffer.append(getXMLTagName());

		// Print xmlns if is root
		// FIXME... this may be limiting...
		if (isRoot()) {
			String namespace = getNamespace();
			if (namespace != null && namespace.length() > 0) {
				buffer.append(" "); //$NON-NLS-1$
				buffer.append("xmlns"); //$NON-NLS-1$
				if (prefix != null && prefix.length() > 0) {
					buffer.append(":"); //$NON-NLS-1$
					buffer.append(getNamespacePrefix());
				}
				buffer.append("="); //$NON-NLS-1$
				buffer.append("\""); //$NON-NLS-1$
				buffer.append(namespace);
				buffer.append("\""); //$NON-NLS-1$
			}
		}
		// Print attributes
		buffer.append(writeAttributes());
		// Make self-enclosing element if specified
		if (terminate) {
			buffer.append("/"); //$NON-NLS-1$
		}
		// Print closing angle bracket
		buffer.append(">"); //$NON-NLS-1$

		return buffer.toString();
	}

	@Override
	public boolean isLeafNode() {
		return false;
	}

	@Override
	public boolean canTerminateStartTag() {
		if ((hasXMLChildren() == false) && (hasXMLContent() == false) && (isLeafNode() == true)) {
			return true;
		}
		return false;
	}

	@Override
	public String write(boolean indent) {
		// Used by text edit operations
		// TODO: MP: TEO: LOW: Refactor into smaller methods
		// TODO: MP: TEO: LOW: Do we care about the indent flag? If so make consistent with write attributes and content
		StringBuilder buffer = new StringBuilder();
		boolean hasChildren = hasXMLChildren();
		boolean hasContent = hasXMLContent();
		boolean terminate = canTerminateStartTag();

		// Print XML decl if root
		if (isRoot()) {
			buffer.append(writeXMLDecl());
		}
		// Print indent
		if (indent) {
			buffer.append(getIndent());
		}
		// Print start element and attributes
		buffer.append(writeShallow(terminate));
		// Print child elements
		if (hasChildren) {
			IDocumentElementNode[] children = getChildNodes();
			for (IDocumentElementNode childNode : children) {
				childNode.setLineIndent(getLineIndent() + 3);
				buffer.append(getLineDelimiter() + childNode.write(true));
			}
		}
		// Print text content
		if (hasContent) {
			buffer.append(writeXMLContent());
		}
		// Print end element
		// TODO: MP: TEO: LOW: Replace with XMLPrintHandler constants
		if (terminate == false) {
			buffer.append(getTerminateIndent());
			buffer.append("</"); //$NON-NLS-1$
			String prefix = getNamespacePrefix();
			if (prefix != null && prefix.length() > 0) {
				buffer.append(getNamespacePrefix());
				buffer.append(":"); //$NON-NLS-1$
			}
			buffer.append(getXMLTagName());
			buffer.append(">"); //$NON-NLS-1$
		}

		return buffer.toString();
	}

	protected String writeXMLContent() {
		StringBuilder buffer = new StringBuilder();
		if (isDefined(fTextNode)) {
			buffer.append(getContentIndent());
			buffer.append(fTextNode.write());
		}
		return buffer.toString();
	}

	protected String writeAttributes() {
		StringBuilder buffer = new StringBuilder();
		IDocumentAttributeNode[] attributes = getNodeAttributes();
		// Write all attributes
		for (IDocumentAttributeNode attrNode : attributes) {
			if (isDefined(attrNode) && !attrNode.getAttributeName().startsWith("xmlns:")) { //$NON-NLS-1$
				buffer.append(getAttributeIndent() + attrNode.write());
			}
		}
		return buffer.toString();
	}

	@Override
	public IDocumentElementNode[] getChildNodes() {
		// Used by text edit operations
		return fChildren.toArray(new IDocumentElementNode[fChildren.size()]);
	}

	@Override
	public int indexOf(IDocumentElementNode child) {
		// Not used by text edit operations
		return fChildren.indexOf(child);
	}

	@Override
	public IDocumentElementNode getChildAt(int index) {
		// Used by text edit operations
		if (index < fChildren.size()) {
			return fChildren.get(index);
		}
		return null;
	}

	@Override
	public IDocumentElementNode getParentNode() {
		// Used by text edit operations
		return fParent;
	}

	@Override
	public void setParentNode(IDocumentElementNode node) {
		// Used by text edit operations (indirectly)
		fParent = node;
	}

	@Override
	public void addChildNode(IDocumentElementNode child) {
		// Used by text edit operations
		addChildNode(child, fChildren.size());
	}

	@Override
	public void addChildNode(IDocumentElementNode child, int position) {
		// Used by text edit operations
		fChildren.add(position, child);
		child.setParentNode(this);
		linkNodeWithSiblings(child);
	}

	@Override
	public IDocumentElementNode removeChildNode(IDocumentElementNode child) {
		// Used by text edit operations
		int index = fChildren.indexOf(child);
		if (index != -1) {
			fChildren.remove(child);
			if (index < fChildren.size()) {
				IDocumentElementNode prevSibling = index == 0 ? null : (IDocumentElementNode) fChildren.get(index - 1);
				fChildren.get(index).setPreviousSibling(prevSibling);
			}
			return child;
		}
		return null;
	}

	@Override
	public IDocumentElementNode removeChildNode(int index) {
		// NOT used by text edit operations
		if ((index < 0) || (index >= fChildren.size())) {
			return null;
		}
		// Get the child at the specified index
		IDocumentElementNode child = fChildren.get(index);
		// Remove the child
		fChildren.remove(child);
		// Determine the new previous sibling for the new element at the
		// specified index
		if (index < fChildren.size()) {
			IDocumentElementNode previousSibling = null;
			if (index != 0) {
				previousSibling = fChildren.get(index - 1);
			}
			IDocumentElementNode newNode = fChildren.get(index);
			newNode.setPreviousSibling(previousSibling);
		}
		return child;
	}

	@Override
	public boolean isErrorNode() {
		// Used by text edit operations (indirectly)
		return fIsErrorNode;
	}

	@Override
	public void setIsErrorNode(boolean isErrorNode) {
		// Used by text edit operations
		fIsErrorNode = isErrorNode;
	}

	@Override
	public void setOffset(int offset) {
		// Used by text edit operations
		fOffset = offset;
	}

	@Override
	public void setLength(int length) {
		// Used by text edit operations
		fLength = length;
	}

	@Override
	public int getOffset() {
		// Used by text edit operations
		return fOffset;
	}

	@Override
	public int getLength() {
		// Used by text edit operations
		return fLength;
	}

	@Override
	public void setXMLAttribute(IDocumentAttributeNode attribute) {
		// Used by text edit operations
		fAttributes.put(attribute.getAttributeName(), attribute);
	}

	@Override
	public String getXMLAttributeValue(String name) {
		// Not used by text edit operations
		IDocumentAttributeNode attribute = fAttributes.get(name);
		if (attribute == null) {
			return null;
		}
		return attribute.getAttributeValue();
	}

	@Override
	public void setXMLTagName(String tag) {
		// Used by text edit operations (indirectly)
		fTag = tag;
	}

	@Override
	public String getXMLTagName() {
		// Used by text edit operations
		return fTag;
	}

	@Override
	public IDocumentAttributeNode getDocumentAttribute(String name) {
		// Used by text edit operations
		return fAttributes.get(name);
	}

	@Override
	public int getLineIndent() {
		// Used by text edit operations
		return fIndent;
	}

	@Override
	public void setLineIndent(int indent) {
		// Used by text edit operations
		fIndent = indent;
	}

	@Override
	public IDocumentAttributeNode[] getNodeAttributes() {
		// Used by text edit operations
		ArrayList<IDocumentAttributeNode> list = new ArrayList<>();
		Iterator<IDocumentAttributeNode> iter = fAttributes.values().iterator();
		while (iter.hasNext()) {
			list.add(iter.next());
		}
		return list.toArray(new IDocumentAttributeNode[list.size()]);
	}

	@Override
	public IDocumentElementNode getPreviousSibling() {
		// Used by text edit operations
		return fPreviousSibling;
	}

	@Override
	public void setPreviousSibling(IDocumentElementNode sibling) {
		// Used by text edit operations
		fPreviousSibling = sibling;
	}

	/**
	 * @return the length to indent
	 */
	@Override
	public String getIndent() {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < fIndent; i++) {
			buffer.append(" "); //$NON-NLS-1$
		}
		return buffer.toString();
	}

	@Override
	public void swap(IDocumentElementNode child1, IDocumentElementNode child2) {
		// Not used by text edit operations
		int index1 = fChildren.indexOf(child1);
		int index2 = fChildren.indexOf(child2);

		fChildren.set(index1, child2);
		fChildren.set(index2, child1);

		child1.setPreviousSibling(index2 == 0 ? null : (IDocumentElementNode) fChildren.get(index2 - 1));
		child2.setPreviousSibling(index1 == 0 ? null : (IDocumentElementNode) fChildren.get(index1 - 1));

		if (index1 < fChildren.size() - 1) {
			fChildren.get(index1 + 1).setPreviousSibling(child2);
		}

		if (index2 < fChildren.size() - 1) {
			fChildren.get(index2 + 1).setPreviousSibling(child1);
		}
	}

	@Override
	public void addTextNode(IDocumentTextNode textNode) {
		// Used by text edit operations
		fTextNode = textNode;
	}

	@Override
	public IDocumentTextNode getTextNode() {
		// Used by text edit operations
		return fTextNode;
	}

	@Override
	public void removeTextNode() {
		// Used by text edit operations
		fTextNode = null;
	}

	@Override
	public void removeDocumentAttribute(IDocumentAttributeNode attr) {
		// Used by text edit operations
		fAttributes.remove(attr.getAttributeName());
	}

	@Override
	public void reconnect(IDocumentElementNode parent, IModel model) {
		// Not used by text edit operations
		// Reconnect XML document characteristics
		reconnectDocument();
		// Reconnect parent
		reconnectParent(parent);
		// Reconnect previous sibling
		reconnectPreviousSibling();
		// Reconnect text node
		reconnectText();
		// Reconnect attribute nodes
		reconnectAttributes();
		// Reconnect children nodes
		reconnectChildren(model);
	}

	private void reconnectAttributes() {
		// Get all attributes
		// Fill in appropriate transient field values for all attributes
		fAttributes.values().forEach(attribute -> attribute.reconnect(this));
	}

	/**
	 * @param model
	 */
	private void reconnectChildren(IModel model) {
		// Fill in appropriate transient field values
		fChildren.forEach(child -> child.reconnect(this, model));
	}

	/**
	 *
	 */
	private void reconnectDocument() {
		// Transient field:  Indent
		fIndent = 0;
		// Transient field:  Error Node
		fIsErrorNode = false;
		// Transient field:  Length
		fLength = -1;
		// Transient field:  Offset
		fOffset = -1;
	}

	/**
	 * @param parent
	 */
	private void reconnectParent(IDocumentElementNode parent) {
		// Transient field:  Parent
		fParent = parent;
	}

	private void reconnectPreviousSibling() {
		// Transient field:  Previous Sibling
		linkNodeWithSiblings(this);
	}

	/**
	 * PRE: Node must have a set parent
	 * @param targetNode
	 */
	private void linkNodeWithSiblings(IDocumentElementNode targetNode) {
		// Get the node's parent
		IDocumentElementNode parentNode = targetNode.getParentNode();
		// Ensure we have a parent
		if (parentNode == null) {
			return;
		}
		// Get the position of the node in the parent's children
		int targetNodePosition = parentNode.indexOf(targetNode);
		// Get the number of children the parent has (including the node)
		int parentNodeChildCount = parentNode.getChildCount();
		// Set this node's previous sibling as the node before it
		if (targetNodePosition <= 0) {
			// null <- targetNode <- ?
			targetNode.setPreviousSibling(null);
		} else if ((targetNodePosition >= 1) && (parentNodeChildCount >= 2)) {
			// ? <- previousNode <- targetNode <- ?
			IDocumentElementNode previousNode = parentNode.getChildAt(targetNodePosition - 1);
			targetNode.setPreviousSibling(previousNode);
		}
		int secondLastNodeIndex = parentNodeChildCount - 2;
		// Set the node after this node's previous sibling as this node
		if ((targetNodePosition >= 0) && (targetNodePosition <= secondLastNodeIndex) && (parentNodeChildCount >= 2)) {
			// ? <- targetNode <- nextNode <- ?
			IDocumentElementNode nextNode = parentNode.getChildAt(targetNodePosition + 1);
			nextNode.setPreviousSibling(targetNode);
		}
		// previousNode <- targetNode <- nextNode
	}

	/**
	 *
	 */
	private void reconnectText() {
		// Transient field:  Text Node
		if (fTextNode != null) {
			fTextNode.reconnect(this);
		}
	}

	@Override
	public int getChildCount() {
		// Not used by text edit operations
		return fChildren.size();
	}

	@Override
	public boolean isRoot() {
		// Used by text edit operations
		return false;
	}

	protected Charset getFileEncoding() {
		return ATTRIBUTE_VALUE_ENCODING;
	}

	protected String writeXMLDecl() {
		StringBuilder buffer = new StringBuilder(XMLPrintHandler.XML_HEAD);
		buffer.append(getFileEncoding());
		buffer.append(XMLPrintHandler.XML_DBL_QUOTES);
		buffer.append(XMLPrintHandler.XML_HEAD_END_TAG);
		buffer.append(getLineDelimiter());
		return buffer.toString();
	}

	protected String getAttributeIndent() {
		return getLineDelimiter() + getIndent() + "      "; //$NON-NLS-1$
	}

	protected String getContentIndent() {
		// TODO: MP: TEO: LOW: Add indent methods on documenttextnode?
		return getLineDelimiter() + getIndent() + "   "; //$NON-NLS-1$
	}

	protected String getTerminateIndent() {
		// Subclasses to override
		return getLineDelimiter() + getIndent();
	}

	protected String getLineDelimiter() {
		// Subclasses to override
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	/**
	 * @param attribute
	 * @return if the attribute is defined
	 */
	protected boolean isDefined(IDocumentAttributeNode attribute) {
		if (attribute == null) {
			return false;
		} else if (attribute.getAttributeValue().trim().length() <= 0) {
			return false;
		}
		return true;
	}

	/**
	 * @param node
	 * @return if the node is defined
	 */
	protected boolean isDefined(IDocumentTextNode node) {
		if (node == null) {
			return false;
		}
		return PDETextHelper.isDefinedAfterTrim(node.getText());
	}

	@Override
	public boolean hasXMLChildren() {
		if (getChildCount() == 0) {
			return false;
		}
		return true;
	}

	@Override
	public boolean hasXMLContent() {
		if (isDefined(fTextNode)) {
			return true;
		}
		return false;
	}

	@Override
	public int getNodeAttributesCount() {
		// Returns the number of attributes with defined values
		int count = 0;
		IDocumentAttributeNode[] attributes = getNodeAttributes();
		for (IDocumentAttributeNode attrNode : attributes) {
			if (isDefined(attrNode)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public boolean hasXMLAttributes() {
		if (getNodeAttributesCount() == 0) {
			return false;
		}
		return true;
	}

	@Override
	public boolean setXMLAttribute(String name, String value) {
		// Not used by text edit operations

		// Ensure name is defined
		if ((name == null) || (name.length() == 0)) {
			return false;
		}
		// Null values are not allowed
		if (value == null) {
			value = ""; //$NON-NLS-1$
		}
		String oldValue = getXMLAttributeValue(name);
		// Check if the value is different
		if ((oldValue != null) && oldValue.equals(value)) {
			return false;
		}
		// Check to see if the attribute already exists
		IDocumentAttributeNode attribute = getNodeAttributesMap().get(name);
		try {
			if (attribute == null) {
				// Attribute does not exist
				attribute = createDocumentAttributeNode();
				attribute.setAttributeName(name);
				attribute.setEnclosingElement(this);
				setXMLAttribute(attribute);
			}
			// Update the value
			attribute.setAttributeValue(value);
		} catch (CoreException e) {
			// Ignore
			return false;
		}
		return true;
	}

	@Override
	public boolean setXMLContent(String text) {
		// Not used by text edit operations
		// Null text not allowed
		if (text == null) {
			text = ""; //$NON-NLS-1$
		}
		// Check to see if the node already exists
		IDocumentTextNode node = getTextNode();
		if (node == null) {
			// Text does not exist, create it
			node = createDocumentTextNode();
			node.setEnclosingElement(this);
			addTextNode(node);
		}
		// Update text on node
		node.setText(text);
		// Always changed
		return true;
	}

	@Override
	public String getXMLContent() {
		IDocumentTextNode node = getTextNode();
		if (node == null) {
			// No text node
			return null;
		}
		return node.getText();
	}

	@Override
	public String write() {
		return write(false);
	}

	@Override
	public int getXMLType() {
		return F_TYPE_ELEMENT;
	}

	@Override
	public boolean isContentCollapsed() {
		return false;
	}

	protected IDocumentAttributeNode createDocumentAttributeNode() {
		return new DocumentAttributeNode();
	}

	protected IDocumentTextNode createDocumentTextNode() {
		return new DocumentTextNode();
	}

	@Override
	public String getNamespace() {
		return fNamespace;
	}

	@Override
	public String getNamespacePrefix() {
		return fNamespacePrefix;
	}

	@Override
	public void setNamespace(String namespace) {
		fNamespace = namespace;
	}

	@Override
	public void setNamespacePrefix(String prefix) {
		fNamespacePrefix = prefix;
	}

}
