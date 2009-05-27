/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.commands;

import java.util.HashMap;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * This class is meant to be used to integrate the Command Composer into a UI (View, Dialog)
 * <ul>
 * <li>setFilterType(int)</li>
 * <li>setButtonCreator(IDialogButtonCreator)</li>
 * <li>setPresetCommand(ParameterizedCommand)</li>
 * <li>setSnapshotContext(IEvaluationContext) - optional</li>
 * </ul>
 * 
 * should all be called before creating the actual control:
 * 
 * Scrolled form = CommandComposerPart#createForm(Composite)
 * CommandComposerPart#createPartControl(form)
 * 
 */
public class CommandComposerPart implements ISelectionChangedListener {

	public static final int F_FILTER_NOT_SET = CommandCopyFilter.indexOf(CommandCopyFilter.NONE);
	public static final int F_HELP_FILTER = CommandCopyFilter.indexOf(CommandCopyFilter.HELP);
	public static final int F_CHEATSHEET_FILTER = CommandCopyFilter.indexOf(CommandCopyFilter.CHEATSHEET);
	public static final int F_INTRO_FILTER = CommandCopyFilter.indexOf(CommandCopyFilter.INTRO);

	private static final ICommandService fCommandService = initCommandService();

	private final TagManager fTagManager = new TagManager();
	private FormToolkit fToolkit;
	private ScrolledForm fScrolledForm;
	private CommandList fCommandList;
	private CommandDetails fCommandDetails;
	private int fFilterType = F_FILTER_NOT_SET;
	private ParameterizedCommand fPC;
	private Image fCommandImage;
	private IEvaluationContext fSnapshotContext;

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

	/**
	 * Set a snapshot context to be used by the command details section of this
	 * part.
	 * 
	 * @param context
	 *            the context to use. May be <code>null</code>.
	 * @since 3.3
	 */
	public void setSnapshotContext(IEvaluationContext context) {
		fSnapshotContext = context;
	}

	public IEvaluationContext getSnapshotContext() {
		return fSnapshotContext;
	}

	protected void createCC(ScrolledForm form, FormToolkit toolkit, ISelectionChangedListener listener) {
		fToolkit = toolkit;
		fScrolledForm = form;
		fScrolledForm.setText(PDEUIMessages.CommandComposerPart_formTitle);
		fCommandImage = PDEPluginImages.DESC_BUILD_VAR_OBJ.createImage();
		fScrolledForm.setImage(fCommandImage);
		Composite body = fScrolledForm.getBody();

		GridLayout layout = new GridLayout();
		layout.marginTop = 10;
		body.setLayout(layout);
		body.setLayoutData(new GridData(GridData.FILL_BOTH));

		SashForm sashForm = new SashForm(body, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		fCommandList = new CommandList(this, sashForm);
		if (listener != null)
			fCommandList.addTreeSelectionListener(listener);

		fCommandDetails = new CommandDetails(this, sashForm);

		sashForm.setWeights(new int[] {4, 5});
		fToolkit.adapt(sashForm, true, true);

		if (fPC != null)
			fCommandList.setSelection(fPC.getCommand());

		fPC = null;
	}

	protected void setMessage(String message, int newType) {
		fScrolledForm.getForm().setMessage(message, newType);
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
		fCommandDetails.dispose();
		if (fCommandImage != null) {
			fCommandImage.dispose();
			fCommandImage = null;
		}
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
		return createComposite(parent, GridData.FILL_BOTH, 1, true, 0);
	}

	protected Composite createComposite(Composite parent, int gdStyle, int numCol, boolean colEqual, int margin) {
		Composite comp = fToolkit.createComposite(parent);
		GridLayout layout = new GridLayout(numCol, colEqual);
		layout.marginHeight = layout.marginWidth = margin;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(gdStyle));
		return comp;
	}

	public void selectionChanged(SelectionChangedEvent event) {
		// Clear the previous error message (if any)
		// Field input value is lost on selection any way
		setMessage(null, IMessageProvider.NONE);

		Object selectionObject = null;
		// if preselection exists use that
		if (fPC != null)
			selectionObject = fPC;
		else if (event.getSelection() instanceof IStructuredSelection)
			selectionObject = (((IStructuredSelection) event.getSelection()).getFirstElement());
		if (selectionObject != null && selectionObject.equals(fCommandDetails.getCommand()))
			return;
		fCommandDetails.showDetailsFor(selectionObject);
	}

	public ParameterizedCommand getParameterizedCommand() {
		return fCommandDetails.buildParameterizedCommand();
	}

	protected void setPresetCommand(ParameterizedCommand pc) {
		fPC = pc;
	}

	protected ParameterizedCommand getPresetCommand() {
		return fPC;
	}

	public CommandList getCommandList() {
		return fCommandList;
	}
}
