package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.custom.BusyIndicator;

public class CategoryDefinitionSection extends ObjectListSection {
	private static final String SECTION_TITLE =
		"SiteEditor.CategoryDefinitionSection.title";
	private static final String SECTION_DESC =
		"SiteEditor.CategoryDefinitionSection.desc";
	private static final String KEY_NEW =
		"SiteEditor.CategoryDefinitionSection.new";
	private static final String KEY_EDIT =
		"SiteEditor.CategoryDefinitionSection.edit";
	private static final String POPUP_EDIT =
		"SiteEditor.CategoryDefinitionSection.popup.edit";

	public CategoryDefinitionSection(SitePage page) {
		super(
			page,
			PDEPlugin.getResourceString(SECTION_TITLE),
			PDEPlugin.getResourceString(SECTION_DESC),
			new String[] {
				PDEPlugin.getResourceString(KEY_NEW),
				PDEPlugin.getResourceString(KEY_EDIT)});
	}

	protected Object[] getElements(Object parent) {
		if (parent instanceof ISite) {
			return ((ISite) parent).getCategoryDefinitions();
		}
		return new Object[0];
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				break;
			case 1 :
				handleOpen();
				break;
		}
	}

	protected boolean isApplicable(Object object) {
		return object instanceof ISiteCategoryDefinition;
	}

	protected String getOpenPopupLabel() {
		return PDEPlugin.getResourceString(POPUP_EDIT);
	}

	protected boolean isOpenable() {
		return true;
	}

	protected void remove(Object input, List objects) throws CoreException {
		ISiteCategoryDefinition[] array =
			(ISiteCategoryDefinition[]) objects.toArray(
				new ISiteCategoryDefinition[objects.size()]);
		ISite site = (ISite) input;
		site.removeCategoryDefinitions(array);
	}

	protected void handleNew() {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		showCategoryDialog(tableViewer, model, null);
	}

	static void showCategoryDialog(
		final Viewer viewer,
		final ISiteModel model,
		final ISiteCategoryDefinition def) {
		BusyIndicator
			.showWhile(viewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				NewCategoryDefinitionDialog dialog =
					new NewCategoryDefinitionDialog(
						viewer.getControl().getShell(),
						model,
						def);
				dialog.create();
				dialog.getShell().setSize(400, 300);
				dialog.open();
			}
		});
	}

	protected void handleOpen() {
		IStructuredSelection sel =
			(IStructuredSelection) tableViewer.getSelection();
		if (sel.size() == 1) {
			ISiteModel model = (ISiteModel) getFormPage().getModel();
			showCategoryDialog(
				tableViewer,
				model,
				(ISiteCategoryDefinition) sel.getFirstElement());
		}
	}

	protected void setButtonsEnabled(boolean value) {
		getTablePart().setButtonEnabled(0, value);
		getTablePart().setButtonEnabled(1, value);
	}

	protected boolean isValidObject(Object obj) {
		return obj instanceof ISiteCategoryDefinition;
	}

	protected void accept(ISite site, ArrayList definitions)
		throws CoreException {
		site.addCategoryDefinitions(
			(ISiteCategoryDefinition[]) definitions.toArray(
				new ISiteCategoryDefinition[definitions.size()]));
	}
}
