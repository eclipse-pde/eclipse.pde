package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.update.ui.forms.internal.FormSection;

public class CategorySection extends CheckboxObjectListSection {
	private static final String SECTION_TITLE =
		"SiteEditor.CategorySection.title";
	private static final String SECTION_DESC =
		"SiteEditor.CategorySection.desc";
	private static final String KEY_NEW = "SiteEditor.CategorySection.new";

	private ISiteFeature currentInput;

	public CategorySection(FeaturePage page) {
		super(
			page,
			PDEPlugin.getResourceString(SECTION_TITLE),
			PDEPlugin.getResourceString(SECTION_DESC),
			new String[] { PDEPlugin.getResourceString(KEY_NEW)});
	}

	protected Object[] getElements(Object parent) {
		if (parent instanceof ISite && currentInput!=null) {
			return ((ISite) parent).getCategoryDefinitions();
		}
		return new Object[0];
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				break;
		}
	}

	protected boolean isApplicable(Object object) {
		return object instanceof ISiteCategoryDefinition;
	}

	protected String getOpenPopupLabel() {
		return null;
	}

	protected boolean isOpenable() {
		return false;
	}

	protected void handleNew() {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		CategoryDefinitionSection.showCategoryDialog(tableViewer, model, null);
	}
	
	protected boolean canDelete(IStructuredSelection ssel) {
		return false;
	}

	protected void remove(Object input, List objects) throws CoreException {
		/*
		ISiteCategory[] array =
			(ISiteCategory[]) objects.toArray(
				new ISiteCategory[objects.size()]);
		ISiteFeature feature = (ISiteFeature) input;
		feature.removeCategories(array);
		*/
	}

	protected void handleOpen() {
	}

	protected void setButtonsEnabled(boolean value) {
		getTablePart().setButtonEnabled(0, value);
	}

	public void sectionChanged(
		FormSection source,
		int changeType,
		Object changeObject) {
		if (changeType == SELECTION) {
			inputChanged(changeObject);
		}
	}

	private void inputChanged(Object changeObject) {
		if (changeObject instanceof ISiteFeature) {
			currentInput = (ISiteFeature) changeObject;
			tableViewer.refresh();
			refresh();
		}
		else {
			currentInput = null;
			tableViewer.refresh();
		}
	}

	private void refresh() {
		ArrayList checked = new ArrayList();
		if (currentInput != null) {
			ISiteCategoryDefinition[] defs =
				currentInput.getSite().getCategoryDefinitions();
			//ISiteCategory[] categories = currentInput.getCategories();

			for (int i = 0; i < defs.length; i++) {
				ISiteCategoryDefinition def = defs[i];
				if (findMatchingCategory(def) != null)
					checked.add(def);
			}
		}
		tableViewer.setCheckedElements(checked.toArray());
	}

	public void update(Object input) {
		refresh();
		updateNeeded = false;
	}

	private ISiteCategory findMatchingCategory(ISiteCategoryDefinition def) {
		ISiteCategory[] categories = currentInput.getCategories();
		for (int j = 0; j < categories.length; j++) {
			ISiteCategory category = categories[j];
			if (category.getName().equalsIgnoreCase(def.getName())) {
				return category;
			}
		}
		return null;
	}

	protected void elementChecked(Object element, boolean checked) {
		if (currentInput == null
			|| !(getFormPage().getModel() instanceof IEditable)) {
			tableViewer.setChecked(element, !checked);
			return;
		}
		try {
			ISiteCategoryDefinition def = (ISiteCategoryDefinition) element;
			ISiteCategory category = findMatchingCategory(def);
			if (checked && category == null) {
				category =
					currentInput.getModel().getFactory().createCategory(
						currentInput);
				category.setName(def.getName());
				currentInput.addCategories(new ISiteCategory[] { category });
			} else if (!checked && category != null) {
				currentInput.removeCategories(new ISiteCategory[] { category });
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void initialize(Object input) {
		super.initialize(input);
		ISiteModel model = (ISiteModel) input;
		getTablePart().getViewer().setInput(model.getSite());
	}
}