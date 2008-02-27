/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.commands;

import java.util.*;
import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.handlers.IHandlerService;

public class CommandDetails {

	private final HashMap fParameterToValue = new HashMap();
	private final ArrayList fObjectParamList = new ArrayList();
	private final ArrayList fValueParamList = new ArrayList();
	private final ArrayList fTextParamList = new ArrayList();

	private CommandComposerPart fCCP;
	private FormToolkit fToolkit;
	private Command fSelectedCommand;
	private ParameterizedCommand fPreSel;

	private Text fComIDT;
	private Text fComPrev;
	private Button fSurroundCopyText;
	private Combo fFilterCombo;
	private Composite fParamComposite;
	private Composite fParamParent;
	private Label fParamLabel;
	private ImageHyperlink fCopyLink;
	private ImageHyperlink fExecLink;

	public CommandDetails(CommandComposerPart cv, Composite parent) {
		fCCP = cv;
		fToolkit = cv.getToolkit();
		createCommandDetails(parent);
	}

	private void createCommandDetails(Composite parent) {
		Composite c = fCCP.createComposite(parent, GridData.FILL_BOTH, 1, true, 5);

		Section section = fToolkit.createSection(c, ExpandableComposite.TITLE_BAR);
		section.setText(PDEUIMessages.CommandDetails_groupName);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite comp = fCCP.createComposite(section);

		createBasicInfo(comp);

		if (fCCP.getFilterType() == CommandComposerPart.F_FILTER_NOT_SET)
			createPreviewLabelComp(comp);
		createParameters(comp);

		section.setClient(comp);

		createLinks(c);
	}

	private void createBasicInfo(Composite parent) {
		Composite comp = fCCP.createComposite(parent, GridData.FILL_HORIZONTAL, 2, false, 0);
		fToolkit.createLabel(comp, PDEUIMessages.CommandDetails_id);
		fComIDT = fToolkit.createText(comp, PDEUIMessages.CommandDetails_noComSelected, SWT.BORDER);
		fComIDT.setEditable(false);
		fComIDT.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void createParameters(Composite parent) {
		Composite comp = fCCP.createComposite(parent, GridData.FILL_HORIZONTAL, 1, false, 0);

		fParamLabel = fToolkit.createLabel(comp, PDEUIMessages.CommandDetails_noParameters);
		fParamLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fParamParent = parent;
		createBlankParamComp();
	}

	private void createLinks(Composite parent) {
		Composite comp = fCCP.createComposite(parent, GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END, 1, false, 0);

		fExecLink = fToolkit.createImageHyperlink(comp, SWT.NONE);
		final Image execImage = PDEPluginImages.DESC_RUN_EXC.createImage();
		fExecLink.setImage(execImage);
		fExecLink.setText(PDEUIMessages.CommandDetails_executeText);
		fExecLink.setToolTipText(PDEUIMessages.CommandDetails_execute);
		fExecLink.addHyperlinkListener(new ExecCommand());
		fExecLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fExecLink.setVisible(false);
		fExecLink.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				execImage.dispose();
			}
		});

		fCopyLink = fToolkit.createImageHyperlink(comp, SWT.NONE);
		final Image clipImage = PDEPluginImages.DESC_CLIPBOARD.createImage();
		fCopyLink.setImage(clipImage);
		fCopyLink.setText(PDEUIMessages.CommandDetails_copyToClipboard);
		fCopyLink.setToolTipText(PDEUIMessages.CommandDetails_copytooltip);
		fCopyLink.addHyperlinkListener(new CopyToClipboard());
		fCopyLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fCopyLink.setVisible(false);
		fCopyLink.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				clipImage.dispose();
			}
		});
	}

	private void createPreviewLabelComp(Composite parent) {
		Composite preLabelComp = fCCP.createComposite(parent, GridData.FILL_HORIZONTAL, 3, false, 0);
		fToolkit.createLabel(preLabelComp, PDEUIMessages.CommandDetails_preview, SWT.NONE).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fSurroundCopyText = fToolkit.createButton(preLabelComp, PDEUIMessages.CommandDetails_includeMarkup, SWT.CHECK);
		fSurroundCopyText.setToolTipText(PDEUIMessages.CommandDetails_markupTooltip);
		fSurroundCopyText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updatePreviewText();
			}
		});

		fFilterCombo = new Combo(preLabelComp, SWT.READ_ONLY | SWT.DROP_DOWN);
		CommandCopyFilter[] filters = CommandCopyFilter.getFilters();
		for (int i = 0; i < filters.length; i++)
			fFilterCombo.add(filters[i].getLabelText());
		fFilterCombo.select(CommandCopyFilter.indexOf(CommandCopyFilter.NONE));
		fFilterCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updatePreviewText();
			}
		});
		fToolkit.adapt(fFilterCombo, true, true);

		fComPrev = fToolkit.createText(parent, "", SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 60;
		fComPrev.setLayoutData(gd);
		fComPrev.setEditable(false);
	}

	protected ParameterizedCommand buildParameterizedCommand() {

		ArrayList list = new ArrayList();
		for (Iterator i = fParameterToValue.keySet().iterator(); i.hasNext();) {
			IParameter parameter = (IParameter) i.next();
			String value = (String) fParameterToValue.get(parameter);
			list.add(new Parameterization(parameter, value));
		}
		Parameterization[] parameterizations = (Parameterization[]) list.toArray(new Parameterization[list.size()]);

		ParameterizedCommand pCommand = new ParameterizedCommand(fSelectedCommand, parameterizations);
		return pCommand;
	}

	private class ExecCommand extends HyperlinkAdapter {
		public void linkActivated(HyperlinkEvent e) {
			ParameterizedCommand pCommand = buildParameterizedCommand();
			try {
				Object obj = null;
				IHandlerService service = getGlobalHandlerService();
				IEvaluationContext context = fCCP.getSnapshotContext();
				obj = service.executeCommandInContext(pCommand, null, context);
				String resultString = null;
				if (obj instanceof String) {
					resultString = (String) obj;
				} else {
					ParameterType returnType = pCommand.getCommand().getReturnType();
					if (returnType != null && returnType.getValueConverter() != null)
						resultString = returnType.getValueConverter().convertToString(obj);
				}
				if (resultString != null) {
					MessageDialog.openInformation(fComIDT.getShell(), PDEUIMessages.CommandDetails_commandResult, resultString);
				}
			} catch (CommandException ex) {
				MessageDialog.openError(fComIDT.getShell(), PDEUIMessages.CommandDetails_execError, ex.toString());
			}
		}
	}

	private String getFilteredCommand() {
		ParameterizedCommand pCommand = buildParameterizedCommand();
		String serializedCommand = pCommand.serialize();
		String markupLabel;
		try {
			markupLabel = fSelectedCommand.getName();
		} catch (NotDefinedException ex) {
			markupLabel = null;
		}
		int index;
		boolean surroundWithMarkup = false;
		if (fCCP.getFilterType() == CommandComposerPart.F_FILTER_NOT_SET) {
			surroundWithMarkup = fSurroundCopyText.getSelection();
			index = fFilterCombo.getSelectionIndex();
		} else {
			index = fCCP.getFilterType();
		}
		CommandCopyFilter ccf = CommandCopyFilter.getFilter(index);
		return ccf.filter(serializedCommand, surroundWithMarkup, markupLabel);
	}

	private class CopyToClipboard extends HyperlinkAdapter {
		public void linkActivated(HyperlinkEvent e) {
			String filteredCommand = getFilteredCommand();

			Object[] data = new Object[] {filteredCommand, /* htmlBuffer.toString() */};
			Transfer[] transfers = new Transfer[] {TextTransfer.getInstance(),/* HTMLTransfer.getInstance() */};

			Clipboard clipboard = new Clipboard(null);
			clipboard.setContents(data, transfers, DND.CLIPBOARD);
			clipboard.dispose();
		}
	}

	private class ValuesParameterControl extends SelectionAdapter implements ModifyListener {
		private final IParameter fParameter;
		private final Map fValues;
		private final Combo fValuesCombo;
		private final Button fClearButton;

		public ValuesParameterControl(IParameter parameter, Map values, Combo valuesCombo, Button clearButton) {
			fParameter = parameter;
			fValues = values;

			fValuesCombo = valuesCombo;
			fValuesCombo.addModifyListener(this);
			if (fPreSel != null && fValues != null) {
				Object obj = fPreSel.getParameterMap().get(parameter.getId());
				if (obj != null) {
					for (Iterator i = fValues.keySet().iterator(); i.hasNext();) {
						Object next = i.next();
						if (obj.equals(fValues.get(next))) {
							fValuesCombo.setText(next.toString());
							break;
						}
					}
				}
			}

			fClearButton = clearButton;
			fClearButton.addSelectionListener(this);
		}

		// clear button pressed
		public void widgetSelected(SelectionEvent e) {
			fValuesCombo.deselectAll();
			fParameterToValue.remove(fParameter);
		}

		// values combo changed
		public void modifyText(ModifyEvent e) {
			String key = fValuesCombo.getText();
			String value = (String) fValues.get(key);
			if (value == null)
				fParameterToValue.remove(fParameter);
			else
				fParameterToValue.put(fParameter, value);
			updatePreviewText();
		}

		protected void dispose() {
			if (!fValuesCombo.isDisposed())
				fValuesCombo.removeModifyListener(this);
			if (!fClearButton.isDisposed())
				fClearButton.removeSelectionListener(this);
		}
	}

	private class ObjectParameterControl implements ModifyListener {
		private final IParameter fParameter;
		private final AbstractParameterValueConverter fValueConverter;
		private final Text fParameterText;

		public ObjectParameterControl(IParameter parameter, AbstractParameterValueConverter valueConverter, Text parameterText, Object selectedObject) {
			fParameter = parameter;
			fValueConverter = valueConverter;

			fParameterText = parameterText;
			fParameterText.addModifyListener(this);

			if (selectedObject != null)
				setParameterText(selectedObject);

			if (fPreSel != null) {
				Object obj = fPreSel.getParameterMap().get(parameter.getId());
				if (obj != null)
					fParameterText.setText(obj.toString());
			}
		}

		public void modifyText(ModifyEvent e) {
			String text = fParameterText.getText();
			if ((text == null) || (text.trim().equals(""))) //$NON-NLS-1$
				fParameterToValue.remove(fParameter);
			else
				fParameterToValue.put(fParameter, text);
			updatePreviewText();
			validate();
		}

		private void setParameterText(Object selectedObject) {
			try {
				String converted = fValueConverter.convertToString(selectedObject);
				if (converted != null)
					fParameterText.setText(converted);
			} catch (ParameterValueConversionException ex) {
				//
			}
		}

		protected void dispose() {
			if (!fParameterText.isDisposed())
				fParameterText.removeModifyListener(this);
		}

		private void validate() {
			String text = fParameterText.getText();
			String error = null;
			if (text.length() > 0) {
				try {
					fValueConverter.convertToObject(text);
				} catch (ParameterValueConversionException e1) {
					error = e1.getMessage();
				}
			}
			if (error == null)
				fCCP.setMessage(null, IMessageProvider.NONE);
			else
				fCCP.setMessage(NLS.bind(PDEUIMessages.CommandDetails_paramValueMessage, fParameter.getName(), error), IMessageProvider.WARNING);
		}
	}

	private class TextParameterControl implements ModifyListener {
		private final IParameter fParameter;
		private final Text fParameterText;

		public TextParameterControl(IParameter parameter, Text parameterText) {
			fParameter = parameter;
			fParameterText = parameterText;
			fParameterText.addModifyListener(this);

			if (fPreSel != null) {
				Object obj = fPreSel.getParameterMap().get(parameter.getId());
				if (obj != null)
					fParameterText.setText(obj.toString());
			}
		}

		public void modifyText(ModifyEvent e) {
			String text = fParameterText.getText();
			if ((text == null) || (text.trim().equals(""))) //$NON-NLS-1$
				fParameterToValue.remove(fParameter);
			else
				fParameterToValue.put(fParameter, text);
			updatePreviewText();
		}

		public void dispose() {
			if (!fParameterText.isDisposed())
				fParameterText.removeModifyListener(this);
		}
	}

	protected void dispose() {
		for (int i = 0; i < fObjectParamList.size(); i++)
			((ObjectParameterControl) fObjectParamList.get(i)).dispose();
		for (int i = 0; i < fValueParamList.size(); i++)
			((ValuesParameterControl) fValueParamList.get(i)).dispose();
		for (int i = 0; i < fTextParamList.size(); i++)
			((TextParameterControl) fTextParamList.get(i)).dispose();
	}

	private void populateParams(Command command, Object selectedObject) throws NotDefinedException {

		createBlankParamComp();

		IParameter[] parameters = command.getParameters();
		if (parameters == null || parameters.length == 0) {
			fParamLabel.setText(PDEUIMessages.CommandDetails_noParameters);
		} else {
			fParamLabel.setText(PDEUIMessages.CommandDetails_numParams);
			Composite paramLine = fToolkit.createComposite(fParamComposite);

			GridLayout paramLineLayout = new GridLayout();
			paramLine.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			paramLineLayout.numColumns = 3;
			paramLineLayout.marginHeight = 0;
			paramLine.setLayout(paramLineLayout);
			for (int i = 0; i < parameters.length; i++) {
				IParameter parameter = parameters[i];

				String nameText = parameter.getName();
				if (!parameter.isOptional())
					nameText += '*';
				fToolkit.createLabel(paramLine, NLS.bind(PDEUIMessages.CommandDetails_param, nameText));

				IParameterValues parameterValues = getParameterValues(parameter);
				if (parameterValues != null) {
					Combo parameterValuesCombo = new Combo(paramLine, SWT.READ_ONLY | SWT.DROP_DOWN);
					parameterValuesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
					fToolkit.adapt(parameterValuesCombo, true, true);

					Map values = parameterValues.getParameterValues();
					for (Iterator keys = values.keySet().iterator(); keys.hasNext();)
						parameterValuesCombo.add((String) keys.next());

					Button clearButton = fToolkit.createButton(paramLine, PDEUIMessages.CommandDetails_clear, SWT.PUSH);

					fValueParamList.add(new ValuesParameterControl(parameter, values, parameterValuesCombo, clearButton));

					continue;
				}

				ParameterType parameterType = command.getParameterType(parameter.getId());
				if ((parameterType != null) && (parameterType.getValueConverter() != null)) {
					Text parameterText = fToolkit.createText(paramLine, "", SWT.SINGLE | SWT.BORDER); //$NON-NLS-1$
					GridData gd = new GridData(GridData.FILL_HORIZONTAL);
					gd.horizontalSpan = 2;
					parameterText.setLayoutData(gd);
					fObjectParamList.add(new ObjectParameterControl(parameter, parameterType.getValueConverter(), parameterText, selectedObject));

					continue;
				}

				Text parameterText = fToolkit.createText(paramLine, "", SWT.SINGLE | SWT.BORDER); //$NON-NLS-1$
				GridData gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = 2;
				parameterText.setLayoutData(gd);
				fTextParamList.add(new TextParameterControl(parameter, parameterText));
			}
		}
		// only use preselected on the first details showing
		fPreSel = null;
		fParamParent.layout();
	}

	private IParameterValues getParameterValues(IParameter parameter) {
		try {
			return parameter.getValues();
		} catch (ParameterValuesException ex) {
			return null;
		}
	}

	public void showDetailsFor(Object object) {
		if (object instanceof ParameterizedCommand)
			object = (fPreSel = (ParameterizedCommand) object).getCommand();

		if (!(object instanceof Command)) {
			resetAllFields();
			return;
		}
		fSelectedCommand = (Command) object;
		fComIDT.setText(fSelectedCommand.getId());

		fParameterToValue.clear();
		fObjectParamList.clear();
		fValueParamList.clear();

		fExecLink.setVisible(fSelectedCommand.isEnabled());
		fCopyLink.setVisible(true);
		try {
			populateParams(fSelectedCommand, object);
		} catch (NotDefinedException e) {
			createNoParamComp();
		}
		updatePreviewText();
	}

	private void resetAllFields() {
		fSelectedCommand = null;
		fComIDT.setText(PDEUIMessages.CommandDetails_noComSelected);
		fParamLabel.setText(PDEUIMessages.CommandDetails_noParameters);

		if (fComPrev != null)
			fComPrev.setText(""); //$NON-NLS-1$

		fExecLink.setVisible(false);
		fCopyLink.setVisible(false);

		fParameterToValue.clear();
		fObjectParamList.clear();
		fValueParamList.clear();

		createNoParamComp();
	}

	private void createNoParamComp() {
		createBlankParamComp();
		fParamParent.layout();
	}

	private void createBlankParamComp() {
		if (fParamComposite != null)
			fParamComposite.dispose();
		fParamComposite = fCCP.createComposite(fParamParent, GridData.FILL_BOTH, 1, true, 0);
	}

	private void updatePreviewText() {
		if (fComPrev != null)
			fComPrev.setText(getFilteredCommand());
	}

	protected Command getCommand() {
		return fSelectedCommand;
	}

	public String getCommandName() {
		if (fSelectedCommand != null)
			try {
				return fSelectedCommand.getName();
			} catch (NotDefinedException e) {
				return fSelectedCommand.getId();
			}
		return null;
	}

	public String getSerializedString() {
		if (fSelectedCommand != null)
			return getFilteredCommand();
		return null;
	}

	public HashMap getParameters() {
		if (fSelectedCommand != null)
			return fParameterToValue;

		return null;
	}

	private IHandlerService getGlobalHandlerService() {
		return (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
	}
}
