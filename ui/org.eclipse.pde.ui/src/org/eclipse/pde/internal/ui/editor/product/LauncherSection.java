package org.eclipse.pde.internal.ui.editor.product;

import java.util.*;

import org.eclipse.pde.internal.core.iproduct.*;
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

	private Map fIconMap = new HashMap();
	
	class IconEntry extends FormEntry {
		String fIconId;
		public IconEntry(Composite parent, FormToolkit toolkit, String labelText, String iconId) {
			super(parent, toolkit, labelText, "Browse...", isEditable(), 20);
			fIconId = iconId;
			addEntryFormListener();
			setEditable(isEditable());
		}		
		private void addEntryFormListener() {
			IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
			setFormEntryListener(new FormEntryAdapter(LauncherSection.this, actionBars) {
				
			});
		}
		
	}

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
		
		createLabel(client, toolkit, "", 2);	
		createLabel(client, toolkit, "Customizing the launcher icons varies per platform:", 2);
		
		addLinuxSection(client, toolkit);
		addMacSection(client, toolkit);
		addSolarisSection(client, toolkit);
		addWin32Section(client, toolkit);
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		gd.verticalSpan = 3;
		section.setLayoutData(gd);
	}
	
	private void addWin32Section(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit, "win32");
		
		Button button = toolkit.createButton(comp, "Use a single .ICO file", SWT.RADIO);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		button.setLayoutData(gd);
		
		fIconMap.put(ILauncherInfo.P_ICO_PATH, new IconEntry(comp, toolkit, "File:", ILauncherInfo.P_ICO_PATH));
		
		button = toolkit.createButton(comp, "Specify six BMP images for both low (16-color) and high (256-color) graphics", SWT.RADIO);
		gd = new GridData();
		gd.horizontalSpan = 3;
		button.setLayoutData(gd);

		fIconMap.put(ILauncherInfo.WIN32_16_LOW, new IconEntry(comp, toolkit, "16x16 (Low):", ILauncherInfo.WIN32_16_LOW));
		fIconMap.put(ILauncherInfo.WIN32_16_HIGH, new IconEntry(comp, toolkit, "16x16 (High):", ILauncherInfo.WIN32_16_HIGH));
		fIconMap.put(ILauncherInfo.WIN32_32_LOW, new IconEntry(comp, toolkit, "32x32 (Low):", ILauncherInfo.WIN32_32_LOW));
		fIconMap.put(ILauncherInfo.WIN32_32_HIGH, new IconEntry(comp, toolkit, "32x32 (High):", ILauncherInfo.WIN32_32_HIGH));
		fIconMap.put(ILauncherInfo.WIN32_48_LOW, new IconEntry(comp, toolkit, "48x48 (Low):", ILauncherInfo.WIN32_48_LOW));
		fIconMap.put(ILauncherInfo.WIN32_48_HIGH, new IconEntry(comp, toolkit, "48x48 (High):", ILauncherInfo.WIN32_48_HIGH));

		toolkit.paintBordersFor(comp);
	}
	
	private void createLabel(Composite parent, FormToolkit toolkit, String text, int span) {
		Label label = toolkit.createLabel(parent, text);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		label.setLayoutData(gd);
	}
	
	private void addLinuxSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit, "linux");
		createLabel(comp, toolkit, "A single XPM icon is required:", 3);	
		fIconMap.put(ILauncherInfo.LINUX_ICON, new IconEntry(comp, toolkit, "Icon:", ILauncherInfo.LINUX_ICON));
		toolkit.paintBordersFor(comp);
	}

	private void addSolarisSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit, "solaris");
		createLabel(comp, toolkit, "Four PM icons are required:", 3);

		fIconMap.put(ILauncherInfo.SOLARIS_LARGE, new IconEntry(comp, toolkit, "Large:", ILauncherInfo.SOLARIS_LARGE));
		fIconMap.put(ILauncherInfo.SOLARIS_MEDIUM, new IconEntry(comp, toolkit, "Medium:", ILauncherInfo.SOLARIS_MEDIUM));
		fIconMap.put(ILauncherInfo.SOLARIS_SMALL, new IconEntry(comp, toolkit, "Small:", ILauncherInfo.SOLARIS_SMALL));
		fIconMap.put(ILauncherInfo.SOLARIS_TINY, new IconEntry(comp, toolkit, "Tiny:", ILauncherInfo.SOLARIS_TINY));
		
		toolkit.paintBordersFor(comp);
	}
	
	private void addMacSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit, "macosx");		
		createLabel(comp, toolkit, "A single ICNS file must be specified:", 3);		
		fIconMap.put(ILauncherInfo.MACOSX_ICON, new IconEntry(comp, toolkit, "File:", ILauncherInfo.MACOSX_ICON));
		toolkit.paintBordersFor(comp);
	}
	
	private Composite createComposite(Composite parent, FormToolkit toolkit, String text) {
		ExpandableComposite ec = toolkit.createExpandableComposite(parent, ExpandableComposite.TWISTIE|ExpandableComposite.CLIENT_INDENT);
		ec.setText(text);
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		ec.setLayoutData(gd);
		ec.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				getPage().getManagedForm().reflow(true);
			}
		});	
		Composite comp = toolkit.createComposite(ec);
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		ec.setClient(comp);
		return comp;
	}

}
