package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.parts.StatusDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

	/**
	 * A dialog to attach source to a jar file.
	 *
	 * copied from org.eclipse.jdt.internal.ui.wizards.buildpaths.LibrariesWorkbookPage.SourceAttachmentDialog.
	 */
	public class SourceAttachmentDialog extends StatusDialog implements IStatusChangeListener {
		private SourceAttachmentBlock fSourceAttachmentBlock;
		private IClasspathEntry oldEntry;
		private IClasspathEntry newEntry;
				
		public SourceAttachmentDialog(Shell parent, IClasspathEntry entry) {
			super(parent);
			this.oldEntry = entry;
			
			String title = PDEPlugin.getFormattedMessage("SourceAttachmentDialog.title", entry.getPath().toString());
			setTitle(title);
			fSourceAttachmentBlock= new SourceAttachmentBlock(ResourcesPlugin.getWorkspace().getRoot(), this, entry);
		}
		
		/*
		 * @see Windows#configureShell
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			WorkbenchHelp.setHelp(newShell, IHelpContextIds.SOURCE_ATTACHMENT);
		}		
				
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite)super.createDialogArea(parent);
						
			Control inner= fSourceAttachmentBlock.createControl(composite);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			return composite;
		}
		
		public void statusChanged(IStatus status) {
			updateStatus(status);
		}
		
		public IPath getSourceAttachmentPath() {
			return fSourceAttachmentBlock.getSourceAttachmentPath();
		}
		
		public IPath getSourceAttachmentRootPath() {
			return fSourceAttachmentBlock.getSourceAttachmentRootPath();
		}
	
		public IClasspathEntry getNewEntry() {
			return newEntry;
		}
	
		protected void okPressed() {
			super.okPressed();
			
			newEntry = JavaCore.newLibraryEntry(oldEntry.getPath(), getSourceAttachmentPath(), getSourceAttachmentRootPath(), oldEntry.isExported());
		}
	}