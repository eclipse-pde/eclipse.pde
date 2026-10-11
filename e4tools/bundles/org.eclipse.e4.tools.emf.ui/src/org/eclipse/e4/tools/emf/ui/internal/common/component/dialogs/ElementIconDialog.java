/*******************************************************************************
 * Copyright (c) 2010 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 *     Steven Spungin <steven@spungin.tv> - Bug 404136, Bug 424730
 ******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.emf.ui.internal.Messages;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.impl.UiPackageImpl;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.swt.widgets.Shell;

/**
 * Searches for an icon and sets its URI on a feature of a model element.
 */
public class ElementIconDialog extends AbstractIconDialogWithScopeAndFilter {
	private final MApplicationElement element;
	private final EStructuralFeature feature;
	private final EditingDomain editingDomain;
	private final String shellTitle;
	private final String dialogTitle;
	private final String dialogMessage;
	private String filterTextMessage;
	private String resourceNameText;

	/** Sets the icon URI of the {@code UI_LABEL__ICON_URI} feature. */
	public ElementIconDialog(Shell parentShell, IEclipseContext context, EditingDomain editingDomain,
			MApplicationElement element, Messages messages, String shellTitle, String dialogTitle,
			String dialogMessage) {
		this(parentShell, context, editingDomain, element, UiPackageImpl.Literals.UI_LABEL__ICON_URI, messages,
				shellTitle, dialogTitle, dialogMessage);
	}

	public ElementIconDialog(Shell parentShell, IEclipseContext context, EditingDomain editingDomain,
			MApplicationElement element, EStructuralFeature feature, Messages messages, String shellTitle,
			String dialogTitle, String dialogMessage) {
		super(parentShell, context);
		this.editingDomain = editingDomain;
		this.element = element;
		this.feature = feature;
		this.shellTitle = shellTitle;
		this.dialogTitle = dialogTitle;
		this.dialogMessage = dialogMessage;
		this.filterTextMessage = messages.AbstractIconDialog_Type_To_Start_Search;
		this.resourceNameText = messages.AbstractIconDialog_Icon_Name;
		context.set(Messages.class, messages);
	}

	/** Replaces the placeholder of the search field and the label in front of it. */
	public ElementIconDialog withSearchLabels(String filterTextMessage, String resourceNameText) {
		this.filterTextMessage = filterTextMessage;
		this.resourceNameText = resourceNameText;
		return this;
	}

	@Override
	protected String getShellTitle() {
		return shellTitle;
	}

	@Override
	protected String getDialogTitle() {
		return dialogTitle;
	}

	@Override
	protected String getDialogMessage() {
		return dialogMessage;
	}

	@Override
	protected String getFilterTextMessage() {
		return filterTextMessage;
	}

	@Override
	protected String getResourceNameText() {
		return resourceNameText;
	}

	@Override
	protected void okPressed() {
		super.okPressed();
		String uri = getValue();
		if (uri != null) {
			Command cmd = SetCommand.create(editingDomain, element, feature, uri);
			if (cmd.canExecute()) {
				editingDomain.getCommandStack().execute(cmd);
			}
		}
	}
}
