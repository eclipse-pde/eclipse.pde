/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - bug 61185
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin.rows;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeFieldAssistDisposer;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeValue;
import org.eclipse.pde.internal.ui.util.PDEJavaHelperUI;
import org.eclipse.pde.internal.ui.util.TextUtil;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class ClassAttributeRow extends ButtonAttributeRow {

	private TypeFieldAssistDisposer fTypeFieldAssistDisposer;

	public ClassAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}

	@Override
	protected boolean isReferenceModel() {
		return !part.getPage().getModel().isEditable();
	}

	@Override
	protected void openReference() {
		String name = TextUtil.trimNonAlphaChars(text.getText()).replace('$', '.');
		name = PDEJavaHelperUI.createClass(name, getProject(), createJavaAttributeValue(name), true);
		if (name != null) {
			text.setText(name);
		}
	}

	@Override
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		super.createContents(parent, toolkit, span);

		if (part.isEditable()) {
			fTypeFieldAssistDisposer = PDEJavaHelperUI.addTypeFieldAssistToText(text, getProject(), IJavaSearchConstants.CLASS_AND_INTERFACE);
		}
	}

	@Override
	protected void browse() {
		BusyIndicator.showWhile(text.getDisplay(), this::doOpenSelectionDialog);
	}

	private JavaAttributeValue createJavaAttributeValue(String name) {
		IProject project = part.getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase) part.getPage().getModel();
		return new JavaAttributeValue(project, model, getAttribute(), name);
	}

	private void doOpenSelectionDialog() {
		IResource resource = getPluginBase().getModel().getUnderlyingResource();
		ISchemaAttribute attr = getAttribute();
		String superName = attr != null ? attr.getBasedOn() : null;
		int index = superName != null ? superName.indexOf(':') : -1;
		if (index > 0) {
			// if the schema specifies a class and interface, then show only types that extend the class (currently can't search on both).
			superName = superName.substring(0, index);
		} else if (index == 0) {
			// if only an interfaces was given (":MyInterface") use this
			superName = superName.substring(1);
		}
		String filter = text.getText();
		if (filter.length() == 0 && superName != null) {
			filter = "**"; //$NON-NLS-1$
		}
		String type = PDEJavaHelperUI.selectType(resource, IJavaElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES, filter, superName);
		if (type != null) {
			text.setText(type);
		}

	}

	private IPluginBase getPluginBase() {
		IBaseModel model = part.getPage().getPDEEditor().getAggregateModel();
		return ((IPluginModelBase) model).getPluginBase();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (fTypeFieldAssistDisposer != null) {
			fTypeFieldAssistDisposer.dispose();
		}
	}
}
