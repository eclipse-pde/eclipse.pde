package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


public class OverviewPage extends PDEFormPage {
	
	public static final String PAGE_ID = "overview";

	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, "Overview");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Overview"); 
		fillBody(managedForm, toolkit);
		managedForm.refresh();
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = 10;
		layout.topMargin = 5;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.numColumns = 2;
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 10;
		layout.makeColumnsEqualWidth = true;
		body.setLayout(layout);

		// sections
		managedForm.addPart(new ProductInfoSection(this, body));	
		createContentSection(body, toolkit);
		createTestingSection(body, toolkit);
		managedForm.addPart(new PackagingSection(this, body));
	}
	
	private void createContentSection(Composite parent, FormToolkit toolkit) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.setText("Product Configuration");
		FormText text = createClient(section, PDEPlugin.getResourceString("Product.overview.content"), toolkit);
		text.setImage("page", getImage(PDEPluginImages.DESC_PAGE_OBJ, PDELabelProvider.F_EDIT));
		section.setClient(text);
	}
	
	private void createTestingSection(Composite parent, FormToolkit toolkit) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.setText("Testing");
		FormText text = createClient(section, PDEPlugin.getResourceString("Product.overview.testing"), toolkit);
		text.setImage("run", getImage(PDEPluginImages.DESC_RUN_EXC)); //$NON-NLS-1$
		text.setImage("debug", getImage(PDEPluginImages.DESC_DEBUG_EXC)); //$NON-NLS-1$
		section.setClient(text);
	}
	
	private FormText createClient(Section section, String content,
			FormToolkit toolkit) {
		FormText text = toolkit.createFormText(section, true);
		try {
			text.setText(content, true, false);
		} catch (SWTException e) {
			text.setText("", false, false);
		}
		section.setClient(text);
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		return text;
	}
	
	private Image getImage(ImageDescriptor desc) {
		return getImage(desc, 0);
	}
	
	private Image getImage(ImageDescriptor desc, int overlay) {
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		return lp.get(desc, overlay);
	}


}
