package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


public class PackagingSection extends PDESection {

	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public PackagingSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Packaging");
		TableWrapData td = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP);
		td.grabHorizontal = true;
		section.setLayoutData(td);
		section.setDescription("To build and package this product:");
		
		Composite comp = toolkit.createComposite(section);
		comp.setLayout(new TableWrapLayout());
		comp.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		FormText text = toolkit.createFormText(comp, true);
		text.setText(PDEPlugin.getResourceString("Product.overview.validate"), true, false);
		text.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		section.setClient(comp);
	}
	

}
