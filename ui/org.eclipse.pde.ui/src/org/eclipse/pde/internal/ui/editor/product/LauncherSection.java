package org.eclipse.pde.internal.ui.editor.product;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.model.*;


public class LauncherSection extends PDESection {

	private FormEntry fNameEntry;

	private ArrayList fIcons = new ArrayList();

	private Button fIcoButton;

	private Button fBmpButton;

	private FormEntry fDirEntry;
	
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
				public void textValueChanged(FormEntry entry) {
					getLauncherInfo().setIconPath(fIconId, entry.getValue());
				}			
				public void browseButtonSelected(FormEntry entry) {
					handleBrowse((IconEntry)entry);
				}			
				public void linkActivated(HyperlinkEvent e) {
					openImage(IconEntry.this.getValue());
				}
			});
		}		
		public String getIconId() {
			return fIconId;
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
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		client.setLayout(layout);
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fNameEntry = new FormEntry(client, toolkit, PDEPlugin.getResourceString("LauncherSection.launcherName"), null, false); //$NON-NLS-1$
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getLauncherInfo().setLauncherName(entry.getValue());
			}
		});
		fNameEntry.setEditable(isEditable());
		
		createLabel(client, toolkit, "", 2);	 //$NON-NLS-1$
		createLabel(client, toolkit, PDEPlugin.getResourceString("LauncherSection.rootLabel"), 2);  //$NON-NLS-1$
		fDirEntry = new FormEntry(client, toolkit, PDEPlugin.getResourceString("LauncherSection.root"), null, false); //$NON-NLS-1$
		fDirEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getLauncherInfo().setRootDirectory(entry.getValue());
			}
		});
		fDirEntry.setEditable(isEditable());
		
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
		
		fIcoButton = toolkit.createButton(comp, PDEPlugin.getResourceString("LauncherSection.ico"), SWT.RADIO); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fIcoButton.setLayoutData(gd);
		fIcoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fIcoButton.getSelection();
				getLauncherInfo().setUseWinIcoFile(selected);
				updateWinEntries(selected);
			}
		});
		fIcoButton.setEnabled(isEditable());
		
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.file"), ILauncherInfo.P_ICO_PATH)); //$NON-NLS-1$
		
		fBmpButton = toolkit.createButton(comp, PDEPlugin.getResourceString("LauncherSection.bmpImages"), SWT.RADIO); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		fBmpButton.setLayoutData(gd);
		fBmpButton.setEnabled(isEditable());
		
		final Label label = toolkit.createLabel(comp, PDEPlugin.getResourceString("LauncherSection.bmpImagesText"), SWT.WRAP);
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		comp.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				// A hack to make the label wrap inside GridLayout
				// This illustrates why TableWrapLayout is used
				Composite c = (Composite)e.widget;
				GridLayout layout = (GridLayout)c.getLayout();
				Rectangle carea = c.getClientArea();
				GridData gd = (GridData)label.getLayoutData();
				gd.widthHint = carea.width - layout.marginWidth-layout.marginWidth;
				Point lsize = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				if (lsize.x< gd.widthHint)
					gd.widthHint = SWT.DEFAULT;
			}
		});

		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.Low16"), ILauncherInfo.WIN32_16_LOW)); //$NON-NLS-1$
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.High16"), ILauncherInfo.WIN32_16_HIGH)); //$NON-NLS-1$
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.32Low"), ILauncherInfo.WIN32_32_LOW)); //$NON-NLS-1$
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.32High"), ILauncherInfo.WIN32_32_HIGH)); //$NON-NLS-1$
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.48Low"), ILauncherInfo.WIN32_48_LOW)); //$NON-NLS-1$
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.48High"), ILauncherInfo.WIN32_48_HIGH)); //$NON-NLS-1$

		toolkit.paintBordersFor(comp);
	}
	
	private void createLabel(Composite parent, FormToolkit toolkit, String text, int span) {
		Label label = toolkit.createLabel(parent, text, SWT.WRAP);
		Layout layout = parent.getLayout();
		if (layout instanceof GridLayout) {
			GridData gd = new GridData();
			gd.horizontalSpan = span;
			label.setLayoutData(gd);				
		}
		else if (layout instanceof TableWrapLayout) {
			TableWrapData td = new TableWrapData();
			td.colspan = span;
			label.setLayoutData(td);			
		}
	}
	
	private void addLinuxSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit, "linux"); //$NON-NLS-1$
		createLabel(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.linuxLabel"), 3);	 //$NON-NLS-1$
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.icon"), ILauncherInfo.LINUX_ICON)); //$NON-NLS-1$
		toolkit.paintBordersFor(comp);
	}

	private void addSolarisSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit, "solaris"); //$NON-NLS-1$
		createLabel(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.solarisLabel"), 3); //$NON-NLS-1$

		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.large"), ILauncherInfo.SOLARIS_LARGE)); //$NON-NLS-1$
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.medium"), ILauncherInfo.SOLARIS_MEDIUM)); //$NON-NLS-1$
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.small"), ILauncherInfo.SOLARIS_SMALL)); //$NON-NLS-1$
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.tiny"), ILauncherInfo.SOLARIS_TINY)); //$NON-NLS-1$
		
		toolkit.paintBordersFor(comp);
	}
	
	private void addMacSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit, "macosx");		 //$NON-NLS-1$
		createLabel(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.macLabel"), 3);		 //$NON-NLS-1$
		fIcons.add(new IconEntry(comp, toolkit, PDEPlugin.getResourceString("LauncherSection.file"), ILauncherInfo.MACOSX_ICON)); //$NON-NLS-1$
		toolkit.paintBordersFor(comp);
	}
	
	private Composite createComposite(Composite parent, FormToolkit toolkit, String text) {
		ExpandableComposite ec = toolkit.createExpandableComposite(parent, ExpandableComposite.TWISTIE|ExpandableComposite.COMPACT);
		ec.setText(text);
		ec.setToggleColor(toolkit.getColors().getColor(FormColors.TB_TOGGLE));
		ec.setActiveToggleColor(toolkit.getHyperlinkGroup().getActiveForeground());
		
		TableWrapData gd = new TableWrapData(TableWrapData.FILL_GRAB);
		gd.colspan = 2;
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
	
	public void refresh() {
		ILauncherInfo info = getLauncherInfo();
		fNameEntry.setValue(info.getLauncherName(), true);
		fDirEntry.setValue(info.getRootDirectory(), true);
		boolean useIco = info.usesWinIcoFile();
		fIcoButton.setSelection(useIco);
		fBmpButton.setSelection(!useIco);
		
		for (int i = 0; i < fIcons.size(); i++) {
			IconEntry entry = (IconEntry)fIcons.get(i);
			entry.setValue(info.getIconPath(entry.getIconId()), true);
		}
		updateWinEntries(useIco);
		super.refresh();
	}
	
	private void updateWinEntries(boolean useIco) {
		for (int i = 0; i < fIcons.size(); i++) {
			IconEntry entry = (IconEntry)fIcons.get(i);
			String id = entry.getIconId();
			if (id.equals(ILauncherInfo.P_ICO_PATH)) {
				entry.setEditable(isEditable()&& useIco);
			} else if (id.equals(ILauncherInfo.WIN32_16_HIGH) 
					|| id.equals(ILauncherInfo.WIN32_16_LOW)
					|| id.equals(ILauncherInfo.WIN32_32_HIGH)
					|| id.equals(ILauncherInfo.WIN32_32_LOW)
					|| id.equals(ILauncherInfo.WIN32_48_HIGH)
					|| id.equals(ILauncherInfo.WIN32_48_LOW)) {
				entry.setEditable(isEditable() && !useIco);
			}
		}
	}
	
	private ILauncherInfo getLauncherInfo() {
		ILauncherInfo info = getProduct().getLauncherInfo();
		if (info == null) {
			info = getModel().getFactory().createLauncherInfo();
			getProduct().setLauncherInfo(info);
		}
		return info;
	}
	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	public void commit(boolean onSave) {
		fNameEntry.commit();
		fDirEntry.commit();
		for (int i = 0; i < fIcons.size(); i++)
			((FormEntry)fIcons.get(i)).commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fNameEntry.cancelEdit();
		fDirEntry.cancelEdit();
		for (int i = 0; i < fIcons.size(); i++)
			((FormEntry)fIcons.get(i)).commit();
		super.cancelEdit();
	}
	
	private void handleBrowse(IconEntry entry) {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getSection().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEPlugin.getResourceString("LauncherSection.dialogTitle"));  //$NON-NLS-1$
		String extension = getExtension(entry.getIconId());
		dialog.setMessage(PDEPlugin.getResourceString("LauncherSection.dialogMessage")); //$NON-NLS-1$
		dialog.addFilter(new FileExtensionFilter(extension)); 
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IFile file = (IFile)dialog.getFirstResult();
			entry.setValue(file.getFullPath().toString());
		}
	}
	
	private void openImage(String value) {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(value));
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile)resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getResourceString("WindowImagesSection.open"), PDEPlugin.getResourceString("WindowImagesSection.warning")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (PartInitException e) {
		}			
	}

	private String getExtension(String iconId) {
		if (iconId.equals(ILauncherInfo.LINUX_ICON))
			return "xpm"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.MACOSX_ICON))
			return "icns"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.SOLARIS_LARGE))
			return "l.pm"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.SOLARIS_MEDIUM))
			return "m.pm"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.SOLARIS_SMALL))
			return "s.pm"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.SOLARIS_TINY))
			return "t.pm"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.P_ICO_PATH))
			return "ico"; //$NON-NLS-1$
		return "bmp";	 //$NON-NLS-1$
	}
	
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

}
