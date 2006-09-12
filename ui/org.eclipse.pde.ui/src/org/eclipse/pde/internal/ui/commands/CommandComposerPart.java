package org.eclipse.pde.internal.ui.commands;

import java.util.HashMap;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class CommandComposerPart implements ISelectionChangedListener {
	
	public static final int F_FILTER_NOT_SET = CommandCopyFilter.indexOf(CommandCopyFilter.NONE);
	public static final int F_HELP_FILTER = CommandCopyFilter.indexOf(CommandCopyFilter.HELP);
	public static final int F_CHEATSHEET_FILTER = CommandCopyFilter.indexOf(CommandCopyFilter.CHEATSHEET);
	public static final int F_INTRO_FILTER = CommandCopyFilter.indexOf(CommandCopyFilter.INTRO);
	
	private static final ICommandService fCommandService = initCommandService();
	
	private final TagManager fTagManager = new TagManager();
	private FormToolkit fToolkit;
	private CommandList fCommandList;
	private CommandDetails fCommandDetails;
	private IDialogButtonCreator fCreator;
	private int fFilterType = F_FILTER_NOT_SET;
	
	/**
	 * Any Dialogs that use this UI Part should implement this interface.
	 */
	protected interface IDialogButtonCreator {
		/**
		 * Create the neccesary buttons for the dialog, the parent Composite
		 * will be right below the details group.
		 * @param parent The parent composite of the buttons
		 */
		public void createButtons(Composite parent);
		/**
		 * Returns a listener which sets the enabled state of the buttons 
		 * created.  This listener will be hooked to the command list tree.
		 * @return an ISelectionChangedListener
		 */
		public ISelectionChangedListener getButtonEnablementListener();
	}
	
	private static ICommandService initCommandService() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		Object serviceObject = workbench.getAdapter(ICommandService.class);
		if (serviceObject instanceof ICommandService)
			return (ICommandService) serviceObject;
		return null;
	}
	
	public void setFilterType(int filterType) {
		fFilterType = filterType;
	}
	
	public int getFilterType() {
		return fFilterType;
	}
	
	public void setButtonCreator(IDialogButtonCreator creator) {
		fCreator = creator;
	}
	
	
	public Composite createPartControl(Composite parent) {
		fToolkit = new FormToolkit(parent.getDisplay());
		
		Composite body = createComposite(parent);
		
		body.setLayout(new GridLayout());
		body.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		SashForm sashForm = new SashForm(body, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fCommandList = new CommandList(this, sashForm, fCreator);
		fCommandDetails = new CommandDetails(this, sashForm, fCreator);
		
		sashForm.setWeights(new int[] {4,5});
		fToolkit.adapt(sashForm, true, true);
		return body;
	}
	
	public FormToolkit getToolkit() {
		return fToolkit;
	}
	
	public ICommandService getCommandService() {
		return fCommandService;
	}
	
	public TagManager getTagManager() {
		return fTagManager;
	}
	
	public void setFocus() {
		fCommandList.setFocus();
	}
	
	public void dispose() {
		fToolkit.dispose();
		fCommandDetails.dispose();
	}
	
	protected String getSelectedCommandName() {
		return fCommandDetails.getCommandName();
	}
	protected String getSelectedSerializedString() {
		return fCommandDetails.getSerializedString();
	}
	protected HashMap getSelectedCommandsParameters() {
		return fCommandDetails.getParameters();
	}
	
	protected Composite createComposite(Composite parent) {
		return createComposite(parent, GridData.FILL_BOTH, 1, true);
	}
	
	protected Composite createComposite(Composite parent, int gdStyle, int numCol, boolean colEqual) {
		Composite comp = fToolkit.createComposite(parent);
		GridLayout layout = new GridLayout(numCol, colEqual);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(gdStyle));
		return comp;
	}

	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection)
			fCommandDetails.showDetailsFor(((IStructuredSelection)selection).getFirstElement());
		else
			fCommandDetails.showDetailsFor(null);
	}

	public ParameterizedCommand getParameterizedCommand() {
		return fCommandDetails.buildParameterizedCommand();
	}
	
}
