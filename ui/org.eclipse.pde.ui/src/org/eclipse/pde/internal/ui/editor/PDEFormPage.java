/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.views.properties.IPropertySheetPage;

public abstract class PDEFormPage extends FormPage {
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public PDEFormPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	protected void createFormContent(IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();
		//form.setBackgroundImage(PDEPlugin.getDefault().getLabelProvider().get(
		//		PDEPluginImages.DESC_FORM_BANNER));
		final String href = getHelpResource();
		if (href != null) {
			IToolBarManager manager = form.getToolBarManager();
			Action helpAction = new Action("help") { //$NON-NLS-1$
				public void run() {
					BusyIndicator.showWhile(form.getDisplay(), new Runnable() {
						public void run() {
							WorkbenchHelp.displayHelpResource(href);
						}
					});
				}
			};
			helpAction.setToolTipText(PDEPlugin.getResourceString("PDEFormPage.help")); //$NON-NLS-1$
			helpAction.setImageDescriptor(PDEPluginImages.DESC_HELP);
			manager.add(helpAction);
			form.updateToolBar();
		}
	}
	public PDEFormEditor getPDEEditor() {
		return (PDEFormEditor) getEditor();
	}
	protected String getHelpResource() {
		return null;
	}
	public IBaseModel getModel() {
		return getPDEEditor().getAggregateModel();
	}
	public void contextMenuAboutToShow(IMenuManager menu) {
	}
	
	protected Control getFocusControl() {
		IManagedForm form = getManagedForm();
		if (form == null)
			return null;
		Control control = form.getForm();
		if (control == null || control.isDisposed())
			return null;
		Display display = control.getDisplay();
		Control focusControl = display.getFocusControl();
		if (focusControl == null || focusControl.isDisposed())
			return null;
		return focusControl;
	}
	public boolean performGlobalAction(String actionId) {
		Control focusControl = getFocusControl();
		if (focusControl == null)
			return false;

		if (canPerformDirectly(actionId, focusControl))
			return true;
		AbstractFormPart focusPart = getFocusSection();
		if (focusPart!=null) {
			if (focusPart instanceof PDESection)
				return ((PDESection)focusPart).doGlobalAction(actionId);
			if (focusPart instanceof PDEDetails)
				return ((PDEDetails)focusPart).doGlobalAction(actionId);
		}
		return false;
	}

	public boolean canPaste(Clipboard clipboard) {
		AbstractFormPart focusPart = getFocusSection();
		if (focusPart != null) {
			if (focusPart instanceof PDESection) {
				return ((PDESection)focusPart).canPaste(clipboard);
			}
			if (focusPart instanceof PDEDetails) {
				return ((PDEDetails)focusPart).canPaste(clipboard);
			}
		}
		return false;
	}
	
	private AbstractFormPart getFocusSection() {
		Control focusControl = getFocusControl();
		if (focusControl == null)
			return null;
		Composite parent = focusControl.getParent();
		AbstractFormPart targetPart = null;
		while (parent != null) {
			Object data = parent.getData("part"); //$NON-NLS-1$
			if (data != null && data instanceof AbstractFormPart) {
				targetPart = (AbstractFormPart) data;
				break;
			}
			parent = parent.getParent();
		}
		return targetPart;
	}
	public IPropertySheetPage getPropertySheetPage() {
		return null;
	}
	protected boolean canPerformDirectly(String id, Control control) {
		if (control instanceof Text) {
			Text text = (Text) control;
			if (id.equals(ActionFactory.CUT.getId())) {
				text.cut();
				return true;
			}
			if (id.equals(ActionFactory.COPY.getId())) {
				text.copy();
				return true;
			}
			if (id.equals(ActionFactory.PASTE.getId())) {
				text.paste();
				return true;
			}
			if (id.equals(ActionFactory.SELECT_ALL.getId())) {
				text.selectAll();
				return true;
			}
			if (id.equals(ActionFactory.DELETE.getId())) {
				int count = text.getSelectionCount();
				if (count == 0) {
					int caretPos = text.getCaretPosition();
					text.setSelection(caretPos, caretPos + 1);
				}
				text.insert(""); //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}	
	public void cancelEdit() {
		IFormPart [] parts = getManagedForm().getParts();
		for (int i=0; i<parts.length; i++) {
			IFormPart part = parts[i];
			if (part instanceof IContextPart)
				((IContextPart)part).cancelEdit();
		}
	}
}