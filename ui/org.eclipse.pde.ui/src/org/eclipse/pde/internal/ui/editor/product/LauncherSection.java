package org.eclipse.pde.internal.ui.editor.product;

import java.util.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
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
			super(parent, toolkit, labelText, PDEPlugin.getResourceString("LauncherSection.browse"), isEditable(), 20); //$NON-NLS-1$
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
		section.setText(PDEPlugin.getResourceString("LauncherSection.title")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString("LauncherSection.desc")); //$NON-NLS-1$

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
		
		createLabel(client, toolkit, "", 2);	 //$NON-NLS-1$
		createLabel(client, toolkit, PDEPlugin.getResourceString("LauncherSection.label"), 2); //$NON-NLS-1$
		
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
		Composite comp = createComposite(parent, toolkit, "win32"); //$NON-NLS-1$
		
		Button button = toolkit.createButton(comp, PDEPlugin.getResourceString("LauncherSection.ico"), SWT.RADIO); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		button.setLayoutData(gd);
		
		fIconMap.put(ILauncherInfo.P_ICO_PATH, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.file"), ILauncherInfo.P_ICO_PATH)); //$NON-NLS-1$
		
		button = toolkit.createButton(comp, PDEPlugin.getResourceString("LauncherSection.bmpImages"), SWT.RADIO); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		button.setLayoutData(gd);

		fIconMap.put(ILauncherInfo.WIN32_16_LOW, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.Low16"), ILauncherInfo.WIN32_16_LOW)); //$NON-NLS-1$
		fIconMap.put(ILauncherInfo.WIN32_16_HIGH, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.High16"), ILauncherInfo.WIN32_16_HIGH)); //$NON-NLS-1$
		fIconMap.put(ILauncherInfo.WIN32_32_LOW, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.32Low"), ILauncherInfo.WIN32_32_LOW)); //$NON-NLS-1$
		fIconMap.put(ILauncherInfo.WIN32_32_HIGH, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.32High"), ILauncherInfo.WIN32_32_HIGH)); //$NON-NLS-1$
		fIconMap.put(ILauncherInfo.WIN32_48_LOW, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.48Low"), ILauncherInfo.WIN32_48_LOW)); //$NON-NLS-1$
		fIconMap.put(ILauncherInfo.WIN32_48_HIGH, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.48High"), ILauncherInfo.WIN32_48_HIGH)); //$NON-NLS-1$

		toolkit.paintBordersFor(comp);
	}
	
	private void createLabel(Composite parent, FormToolkit toolkit, String text, int span) {
		Label label = toolkit.createLabel(parent, text);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		label.setLayoutData(gd);
	}
	
	private void addLinuxSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit, "linux"); //$NON-NLS-1$
		createLabel(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.linuxLabel"), 3);	 //$NON-NLS-1$
		fIconMap.put(ILauncherInfo.LINUX_ICON, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.icon"), ILauncherInfo.LINUX_ICON)); //$NON-NLS-1$
		toolkit.paintBordersFor(comp);
	}

	private void addSolarisSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit, "solaris"); //$NON-NLS-1$
		createLabel(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.solarisLabel"), 3); //$NON-NLS-1$

		fIconMap.put(ILauncherInfo.SOLARIS_LARGE, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.large"), ILauncherInfo.SOLARIS_LARGE)); //$NON-NLS-1$
		fIconMap.put(ILauncherInfo.SOLARIS_MEDIUM, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.medium"), ILauncherInfo.SOLARIS_MEDIUM)); //$NON-NLS-1$
		fIconMap.put(ILauncherInfo.SOLARIS_SMALL, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.small"), ILauncherInfo.SOLARIS_SMALL)); //$NON-NLS-1$
		fIconMap.put(ILauncherInfo.SOLARIS_TINY, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.tiny"), ILauncherInfo.SOLARIS_TINY)); //$NON-NLS-1$
		
		toolkit.paintBordersFor(comp);
	}
	
	private void addMacSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit, "macosx");		 //$NON-NLS-1$
		createLabel(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.macLabel"), 3);		 //$NON-NLS-1$
		fIconMap.put(ILauncherInfo.MACOSX_ICON, new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.file"), ILauncherInfo.MACOSX_ICON)); //$NON-NLS-1$
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
