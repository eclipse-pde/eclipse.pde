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

package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.schema.SchemaRootElement;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;

public class ExtensionAttributePointDectector implements MouseListener, KeyListener {

	private ManifestSourcePage fSourcePage;

	private StyledText fStyledText;

	private ISelection fSelection;

	public ExtensionAttributePointDectector() {
		fSelection = null;
		fStyledText = null;
		fSourcePage = null;
	}

	public ISelection getSelection() {
		return fSelection;
	}

	public void setTextEditor(ManifestSourcePage editor) {
		fSourcePage = editor;
		// Get the new styled text widget
		ISourceViewer viewer = fSourcePage.getViewer();
		// If the source viewer is not initialized, we can't do anything here
		if (viewer == null) {
			return;
		}
		StyledText newStyledText = viewer.getTextWidget();
		// If the new styled text equals the old one, keep the old one
		if ((fStyledText != null) && fStyledText.equals(newStyledText)) {
			return;
		}
		// Remove the listeners on the old styled text
		removeListeners();
		// Replace the old with the new
		fStyledText = newStyledText;
		// Add the listeners to the new styled text
		addListeners();
		// Enable this action if the selection is within the point attribute on
		// an extension
		checkIfOnTarget();
	}

	private void checkIfOnTarget() {
		// Reset the Selection
		fSelection = null;
		// Ensure the input is valid
		if (isInputInitialized() == false) {
			return;
		}
		// Get the region selected
		Point selectionPoint = fStyledText.getSelection();
		Region selectionRegion = new Region(selectionPoint.x, selectionPoint.y - selectionPoint.x);
		// Determine whether the region selected is the point attribute of
		// and extension
		if ((selectionRegion == null) || (fSourcePage == null)) {
			return;
		}
		// Retrieve the document range corresponding to the selection region
		IDocumentRange element = fSourcePage.getRangeElement(selectionRegion.getOffset(), true);
		// Validate the obtained document range
		if (XMLUtil.withinRange(element, selectionRegion.getOffset()) == false) {
			return;
		}
		// Ensure we have a document attribute 
		if ((element instanceof IDocumentAttributeNode) == false) {
			return;
		}
		// Ignore IDocumentElementNode
		// Ignore IDocumentTextNode
		IDocumentAttributeNode documentAttribute = ((IDocumentAttributeNode) element);
		String attributeValue = documentAttribute.getAttributeValue();
		// Ensure the attribute value is defined
		if ((attributeValue == null) || (attributeValue.length() == 0)) {
			return;
		}
		// Get the parent node: either extension or extension point
		IPluginObject node = XMLUtil.getTopLevelParent(documentAttribute);
		// Ensure the node is defined and comes from and editable model
		if ((node == null) || (node.getModel().isEditable() == false)) {
			return;
		}
		// Ensure the node is an extension
		if ((node instanceof IPluginExtension) == false) {
			return;
		}
		// Ignore IPluginExtensionPoint
		IPluginExtension extension = (IPluginExtension) node;
		// Retrieve the corresponding schema attribute to this node
		ISchemaAttribute schemaAttribute = XMLUtil.getSchemaAttribute(documentAttribute, extension.getPoint());
		// Ensure the schema attribute is defined
		if (schemaAttribute == null) {
			return;
		}
		// Ensure the attribute is a point
		if (((schemaAttribute.getParent() instanceof SchemaRootElement) == false) || (documentAttribute.getAttributeName().equals(IPluginExtension.P_POINT) == false)) {
			return;
		}
		fSelection = new StructuredSelection(extension);
	}

	private void removeListeners() {
		if (isInputInitialized() == false) {
			return;
		}
		fStyledText.removeMouseListener(this);
		fStyledText.removeKeyListener(this);
	}

	private boolean isInputInitialized() {
		if ((fStyledText == null) || (fStyledText.isDisposed())) {
			return false;
		}
		return true;
	}

	private void addListeners() {
		if (isInputInitialized() == false) {
			return;
		}
		fStyledText.addMouseListener(this);
		fStyledText.addKeyListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDoubleClick(MouseEvent e) {
		// Ignore
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDown(MouseEvent e) {
		// Ignore
	}

	public void mouseUp(MouseEvent e) {
		checkIfOnTarget();
	}

	public void keyPressed(KeyEvent e) {
		checkIfOnTarget();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
		// Ignore
	}

}
