package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;


public class LauncherSection extends PDESection {

	private FormEntry fNameEntry;
	private FormEntry fLinuxIcon;
	private FormEntry fMacIcon;
	private FormEntry fSmallIcon;
	private FormEntry fLargeIcon;
	private FormEntry fMediumIcon;
	private FormEntry fTinyIcon;
	private FormEntry fIco;
	private FormEntry f16Low;
	private FormEntry f16High;
	private FormEntry f32Low;
	private FormEntry f32High;
	private FormEntry f48Low;
	private FormEntry f48High;

	public LauncherSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Program Launcher");
		section.setDescription("Customize the executable that is used to launch the product:");

		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		client.setLayout(layout);
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fNameEntry = new FormEntry(client, toolkit, "Launcher Name:", null, false); //$NON-NLS-1$
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		fNameEntry.setEditable(isEditable());
		
		Label label = toolkit.createLabel(client, "");
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		label = toolkit.createLabel(client, "Customizing the launcher icons varies per platform:");
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		addLinuxSection(client, toolkit, actionBars);
		addMacSection(client, toolkit, actionBars);
		addSolarisSection(client, toolkit, actionBars);
		addWin32Section(client, toolkit, actionBars);
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		gd.verticalSpan = 3;
		section.setLayoutData(gd);
	}
	
	private void addWin32Section(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		ExpandableComposite ec = toolkit.createExpandableComposite(parent, ExpandableComposite.TWISTIE|ExpandableComposite.CLIENT_INDENT);
		ec.setText("win32");
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 10;
		ec.setLayoutData(gd);
		ec.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				getPage().getManagedForm().reflow(true);
			}
		});	
		
		Composite comp = toolkit.createComposite(ec);
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Button button = toolkit.createButton(comp, "Use a single .ICO file", SWT.RADIO);
		gd = new GridData();
		gd.horizontalSpan = 3;
		button.setLayoutData(gd);
		
		fIco = new FormEntry(comp, toolkit, "File:", "Browse...", isEditable(), 20); //$NON-NLS-1$
		fIco.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		fIco.setEditable(isEditable());
		
		button = toolkit.createButton(comp, "Specify six '.bmp' images for both low and high color graphics", SWT.RADIO);
		gd = new GridData();
		gd.horizontalSpan = 3;
		button.setLayoutData(gd);
		
		f16Low = new FormEntry(comp, toolkit, "16x16 (Low):", "Browse...", isEditable(), 20); //$NON-NLS-1$
		f16Low.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		f16Low.setEditable(isEditable());

		f16High = new FormEntry(comp, toolkit, "16x16 (High):", "Browse...", isEditable(), 20); //$NON-NLS-1$
		f16High.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		f16High.setEditable(isEditable());

		f32Low = new FormEntry(comp, toolkit, "32x32 (Low):", "Browse...", isEditable(), 20); //$NON-NLS-1$
		f32Low.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		f32Low.setEditable(isEditable());

		f32High = new FormEntry(comp, toolkit, "32x32 (High):", "Browse...", isEditable(), 20); //$NON-NLS-1$
		f32High.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		f32High.setEditable(isEditable());

		f48Low = new FormEntry(comp, toolkit, "48x48 (Low):", "Browse...", isEditable(), 20); //$NON-NLS-1$
		f48Low.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		f48Low.setEditable(isEditable());

		f48High = new FormEntry(comp, toolkit, "48x48 (High):", "Browse...", isEditable(), 20); //$NON-NLS-1$
		f48High.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		f48High.setEditable(isEditable());
		toolkit.paintBordersFor(comp);
		ec.setClient(comp);
	}
	
	private void addLinuxSection(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		ExpandableComposite ec = toolkit.createExpandableComposite(parent, ExpandableComposite.TWISTIE|ExpandableComposite.CLIENT_INDENT);
		ec.setText("linux");
			
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 10;
		ec.setLayoutData(gd);
		ec.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				getPage().getManagedForm().reflow(true);
			}
		});	
		
		Composite comp = toolkit.createComposite(ec);
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = toolkit.createLabel(comp, "A single icon named 'icon.xpm' is required:");
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		fLinuxIcon = new FormEntry(comp, toolkit, "Icon:", "Browse...", isEditable()); //$NON-NLS-1$
		fLinuxIcon.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		fLinuxIcon.setEditable(isEditable());
		toolkit.paintBordersFor(comp);
		ec.setClient(comp);
		
	}

	private void addSolarisSection(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		ExpandableComposite ec = toolkit.createExpandableComposite(parent, ExpandableComposite.TWISTIE|ExpandableComposite.CLIENT_INDENT);
		ec.setText("solaris");
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 10;
		ec.setLayoutData(gd);
		ec.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				getPage().getManagedForm().reflow(true);
			}
		});	
		
		Composite comp = toolkit.createComposite(ec);
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = toolkit.createLabel(comp, "Four icons with a '.pm' extension must be specified:");
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		fLargeIcon = new FormEntry(comp, toolkit, "Large:", "Browse...", isEditable()); //$NON-NLS-1$
		fLargeIcon.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		fLargeIcon.setEditable(isEditable());

		fMediumIcon = new FormEntry(comp, toolkit, "Medium:", "Browse...", isEditable()); //$NON-NLS-1$
		fMediumIcon.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		fMediumIcon.setEditable(isEditable());

		fSmallIcon = new FormEntry(comp, toolkit, "Small:", "Browse...", isEditable()); //$NON-NLS-1$
		fSmallIcon.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		fSmallIcon.setEditable(isEditable());

		fTinyIcon = new FormEntry(comp, toolkit, "Tiny:", "Browse...", isEditable()); //$NON-NLS-1$
		fTinyIcon.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		fTinyIcon.setEditable(isEditable());

		toolkit.paintBordersFor(comp);
		ec.setClient(comp);
	}
	
	private void addMacSection(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		ExpandableComposite ec = toolkit.createExpandableComposite(parent, ExpandableComposite.TWISTIE|ExpandableComposite.CLIENT_INDENT);
		ec.setText("macosx");
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 10;
		ec.setLayoutData(gd);
		ec.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				getPage().getManagedForm().reflow(true);
			}
		});	
		
		Composite comp = toolkit.createComposite(ec);
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = toolkit.createLabel(comp, "A single '*.icns' file must be specified:");
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		fMacIcon = new FormEntry(comp, toolkit, "Icon:", "Browse...", isEditable()); //$NON-NLS-1$
		fMacIcon.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		fMacIcon.setEditable(isEditable());
		
		toolkit.paintBordersFor(comp);
		ec.setClient(comp);
	}




}
