/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * First page in the target definition editor.  Allows for editing of the name
 * and locations in the target.
 * @see TargetEditor
 * @see InformationSection
 * @see LocationsSection
 */
public class DefinitionPage extends FormPage implements IHyperlinkListener {

	public static final String PAGE_ID = "definition"; //$NON-NLS-1$

	public DefinitionPage(TargetEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.DefinitionPage_0);
	}

	/**
	 * @return The target model backing this editor
	 */
	public ITargetDefinition getTarget() {
		return ((TargetEditor) getEditor()).getTarget();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEUIMessages.DefinitionPage_1);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		toolkit.decorateFormHeading(form.getForm());
		fillBody(managedForm, toolkit);
		((TargetEditor) getEditor()).contributeToToolbar(managedForm.getForm(), IHelpContextIds.TARGET_EDITOR_DEFINITION_PAGE);
		((TargetEditor) getEditor()).addForm(managedForm);
		form.updateToolBar();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.TARGET_EDITOR_DEFINITION_PAGE);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form, IHelpContextIds.TARGET_EDITOR_DEFINITION_PAGE);
	}

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(true, 1));
		managedForm.addPart(new InformationSection(this, body));
		managedForm.addPart(new LocationsSection(this, body));
		Composite linkComposite = toolkit.createComposite(body);
		linkComposite.setLayout(FormLayoutFactory.createFormGridLayout(true, 2));
		linkComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		createContentsSection(linkComposite, toolkit);
		createEnvironmentSection(linkComposite, toolkit);
	}

	private void createContentsSection(Composite parent, FormToolkit toolkit) {
		Section section = createSection(parent, toolkit, PDEUIMessages.OverviewPage_contentTitle);
		createText(section, PDEUIMessages.OverviewPage_contentDescription, toolkit);
	}

	private void createEnvironmentSection(Composite parent, FormToolkit toolkit) {
		Section section = createSection(parent, toolkit, PDEUIMessages.OverviewPage_environmentTitle);
		createText(section, PDEUIMessages.OverviewPage_environmentDescription, toolkit);
	}

	private Section createSection(Composite parent, FormToolkit toolkit, String title) {
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		section.setText(title);
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		return section;
	}

	private FormText createText(Section section, String content, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section, SWT.NONE);
		container.setLayout(FormLayoutFactory.createSectionClientTableWrapLayout(false, 1));
		section.setClient(container);
		FormText text = toolkit.createFormText(container, true);
		try {
			text.setText(content, true, false);
		} catch (SWTException e) {
			text.setText(e.getMessage(), false, false);
		}
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		data.maxWidth = 250;
		text.setLayoutData(data);
		text.addHyperlinkListener(this);
		return text;
	}

	@Override
	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		if (href.equals("content")) //$NON-NLS-1$
			getEditor().setActivePage(ContentPage.PAGE_ID);
		else if (href.equals("environment")) //$NON-NLS-1$
			getEditor().setActivePage(EnvironmentPage.PAGE_ID);
	}

	@Override
	public void linkEntered(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(e.getLabel());
	}

	@Override
	public void linkExited(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(null);
	}

	@Override
	public boolean canLeaveThePage() {
		((TargetEditor) getEditor()).setDirty(isDirty());
		return true;
	}
}
