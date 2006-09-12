package org.eclipse.pde.internal.ui.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.AbstractParameterValueConverter;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.commands.ParameterType;
import org.eclipse.core.commands.ParameterValueConversionException;
import org.eclipse.core.commands.ParameterValuesException;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.commands.CommandComposerPart.IDialogButtonCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;

public class CommandDetails {
	
	private static String F_NO_SEL = PDEUIMessages.CommandDetails_noComSelected;
	private static String F_NO_PARAM = PDEUIMessages.CommandDetails_noParameters;
	
	private final HashMap fParameterToValue = new HashMap();
	private final ArrayList fObjectParamList = new ArrayList();
	private final ArrayList fValueParamList = new ArrayList();
	private final ArrayList fTextParamList = new ArrayList();
	
	private CommandComposerPart fCSP;
	private FormToolkit fToolkit;
	private Command fSelectedCommand;
	
	private Text fComIDT;
	private Text fComPrev;
	private Button fSurroundCopyText;
	private Combo fFilterCombo;
	private Composite fParamComposite;
	private Composite fParamParent;
	private Label fParamLabel;
	private ImageHyperlink fCopyLink;
	private ImageHyperlink fExecLink;

	public CommandDetails(CommandComposerPart cv, Composite parent, IDialogButtonCreator buttonCreator) {
		fCSP = cv;
		fToolkit = cv.getToolkit();
		createCommandDetails(parent, buttonCreator);
	}
	
	private void createCommandDetails(Composite parent, IDialogButtonCreator buttonCreator) {
		Composite c = fCSP.createComposite(parent);
		
		Section section = fToolkit.createSection(c, ExpandableComposite.SHORT_TITLE_BAR);
		section.setText(PDEUIMessages.CommandDetails_groupName);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite comp = fCSP.createComposite(section);
		
		createBasicInfo(comp);
		
		if (fCSP.getFilterType() == CommandComposerPart.F_FILTER_NOT_SET)
			createPreviewLabelComp(comp);
		createParameters(comp);
		
		section.setClient(comp);
		
		if (buttonCreator != null)
			buttonCreator.createButtons(c);
	}
	
	private void createBasicInfo(Composite parent) {
		Composite comp = fCSP.createComposite(parent, GridData.FILL_HORIZONTAL, 2, false);
		fToolkit.createLabel(comp, PDEUIMessages.CommandDetails_id);
		fComIDT = fToolkit.createText(comp, F_NO_SEL, SWT.BORDER);
		fComIDT.setEditable(false);
		fComIDT.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	
	private void createParameters(Composite parent) {
		Composite comp = fCSP.createComposite(parent, GridData.FILL_HORIZONTAL, 3, false);
		
		fParamLabel = fToolkit.createLabel(comp, F_NO_PARAM);
		fParamLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fCopyLink = fToolkit.createImageHyperlink(comp, SWT.NONE);
		final Image clipImage = PDEPluginImages.DESC_CLIPBOARD.createImage();
		fCopyLink.setImage(clipImage);
		fCopyLink.setText(PDEUIMessages.CommandDetails_copyToClipboard);
		fCopyLink.setToolTipText(PDEUIMessages.CommandDetails_copytooltip);
		fCopyLink.addHyperlinkListener(new CopyToClipboard());
		fCopyLink.setEnabled(false);
		fCopyLink.addDisposeListener(new DisposeListener()
			{ public void widgetDisposed(DisposeEvent e) { clipImage.dispose(); }} );
		
		fExecLink = fToolkit.createImageHyperlink(comp, SWT.NONE);
		final Image execImage = PDEPluginImages.DESC_RUN_EXC.createImage();
		fExecLink.setImage(execImage);
		fExecLink.setText(PDEUIMessages.CommandDetails_executeText);
		fExecLink.setToolTipText(PDEUIMessages.CommandDetails_execute);
		fExecLink.addHyperlinkListener(new ExecCommand());
		fExecLink.setEnabled(false);
		fExecLink.addDisposeListener(new DisposeListener()
			{ public void widgetDisposed(DisposeEvent e) { execImage.dispose(); } });
		
		fParamParent = parent;
		createBlankParamComp();
	}
	
	private void createPreviewLabelComp(Composite parent) {
		Composite preLabelComp = fCSP.createComposite(parent, GridData.FILL_HORIZONTAL, 3, false);
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
		for (Iterator i = fParameterToValue.keySet().iterator(); i.hasNext(); ) {
			IParameter parameter = (IParameter)i.next();
			String value = (String)fParameterToValue.get(parameter);
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
				Object obj = pCommand.executeWithChecks(null, null);
				String resultString = null;
				if (obj instanceof String) {
					resultString = (String)obj;
				} else {
					ParameterType returnType = pCommand.getCommand().getReturnType();
					if (returnType != null && returnType.getValueConverter() != null)
						resultString = returnType.getValueConverter().convertToString(obj);
				}
				if (resultString != null) {
					MessageDialog.openInformation(
							fComIDT.getShell(),
							PDEUIMessages.CommandDetails_commandResult,
							resultString);
				}
			} catch (CommandException ex) {
				MessageDialog.openError(
						fComIDT.getShell(),
						PDEUIMessages.CommandDetails_execError,
						ex.toString());
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
		if (fCSP.getFilterType() == CommandComposerPart.F_FILTER_NOT_SET) {
			surroundWithMarkup = fSurroundCopyText.getSelection();
			index = fFilterCombo.getSelectionIndex();
		} else {
			index = fCSP.getFilterType();
		}
		CommandCopyFilter ccf = CommandCopyFilter.getFilter(index);
		return ccf.filter(serializedCommand, surroundWithMarkup, markupLabel);
	}
	
	private class CopyToClipboard extends HyperlinkAdapter {
		public void linkActivated(HyperlinkEvent e) {
			String filteredCommand = getFilteredCommand();
			
			Object[] data = new Object[] { filteredCommand, /* htmlBuffer.toString() */ };
			Transfer[] transfers = new Transfer[] { TextTransfer.getInstance(),/* HTMLTransfer.getInstance() */ };
			
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
			String value = (String)fValues.get(key);
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
	
	private class ObjectParameterControl extends SelectionAdapter implements ModifyListener {
		private final IParameter fParameter;
		private final AbstractParameterValueConverter fValueConverter;
		private final Text fParameterText;
		private final Button fTrackSelectionButton;
		private SelectionTracker fSelectionTracker;
		public ObjectParameterControl(IParameter parameter, AbstractParameterValueConverter valueConverter, Text parameterText, Button trackSelectionButton, Object selectedObject) {
			fParameter = parameter;
			fValueConverter = valueConverter;
			
			fParameterText = parameterText;
			fParameterText.addModifyListener(this);
			
			fTrackSelectionButton = trackSelectionButton;
			fTrackSelectionButton.addSelectionListener(this);
			
			if (selectedObject != null)
				setParameterText(selectedObject);
		}
		
		private ISelectionService getSelectionService() {
			IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (activeWindow == null) return null;
			return activeWindow.getSelectionService();
		}
		
		// track selection button pressed
		public void widgetSelected(SelectionEvent e) {
			ISelectionService selectionService = getSelectionService();
			if (selectionService == null) return;
			if (fTrackSelectionButton.getSelection()) {
				fSelectionTracker = new SelectionTracker(selectionService);
			} else {
				if (fSelectionTracker != null) {
					fSelectionTracker.dispose();
					fSelectionTracker = null;
				}
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
		
		private void setParameterText(Object selectedObject) {
			try {
				String converted = fValueConverter.convertToString(selectedObject);
				if (converted != null)
					fParameterText.setText(converted);
			} catch (ParameterValueConversionException ex) {
				//
			}
		}
		
		private class SelectionTracker implements ISelectionListener {
			private final ISelectionService fSelectionService;
			public SelectionTracker(ISelectionService selectionService) {
				fSelectionService = selectionService;
				fSelectionService.addSelectionListener(this);
			}
			
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (selection instanceof IStructuredSelection) {
					Object selectedObject = ((IStructuredSelection)selection).getFirstElement();
					setParameterText(selectedObject);
				}
			}
			
			public void dispose() {
				fSelectionService.removeSelectionListener(this);
			}
		}
		
		protected void dispose() {
			if (!fParameterText.isDisposed())
				fParameterText.removeModifyListener(this);
			if (!fTrackSelectionButton.isDisposed())
				fTrackSelectionButton.removeSelectionListener(this);
			fSelectionTracker = null;
		}
	}
	
	private class TextParameterControl implements ModifyListener {
		private final IParameter fParameter;
		private final Text fParameterText;
		public TextParameterControl(IParameter parameter, Text parameterText) {
			fParameter = parameter;
			fParameterText = parameterText;
			fParameterText.addModifyListener(this);
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
			((ObjectParameterControl)fObjectParamList.get(i)).dispose();
		for (int i = 0; i < fValueParamList.size(); i++)
			((ValuesParameterControl)fValueParamList.get(i)).dispose();
		for (int i = 0; i < fTextParamList.size(); i++)
			((TextParameterControl)fTextParamList.get(i)).dispose();
	}
	
	private void populateParams(Command command, Object selectedObject) throws NotDefinedException {
		
		createBlankParamComp();
		
		IParameter[] parameters = command.getParameters();
		if (parameters == null || parameters.length == 0) {
			fParamLabel.setText(F_NO_PARAM);
		} else {
			fParamLabel.setText(NLS.bind(PDEUIMessages.CommandDetails_numParams, Integer.toString(parameters.length)));
			Composite paramLine = fToolkit.createComposite(fParamComposite);
			
			GridLayout paramLineLayout = new GridLayout();
			paramLine.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			paramLineLayout.numColumns = 3;
			paramLineLayout.marginHeight = 0;
			paramLine.setLayout(paramLineLayout);
			for (int i = 0; i < parameters.length; i++) {
				IParameter parameter = parameters[i];
				
				String nameText = parameter.getName();
				if (!parameter.isOptional()) nameText += '*';
				fToolkit.createLabel(paramLine, NLS.bind(PDEUIMessages.CommandDetails_param, nameText));
			
				IParameterValues parameterValues = getParameterValues(parameter);
				if (parameterValues != null) {
					Combo parameterValuesCombo = new Combo(paramLine, SWT.READ_ONLY	| SWT.DROP_DOWN);
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
					parameterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
					Button trackSelectionButton = fToolkit.createButton(paramLine, PDEUIMessages.CommandDetails_track, SWT.CHECK);
					fObjectParamList.add(new ObjectParameterControl(parameter, parameterType.getValueConverter(), parameterText, trackSelectionButton, selectedObject));
			
					continue;
				}
			
				Text parameterText = fToolkit.createText(paramLine, "", SWT.SINGLE | SWT.BORDER); //$NON-NLS-1$
				GridData gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = 2;
				parameterText.setLayoutData(gd);
				fTextParamList.add(new TextParameterControl(parameter, parameterText));
			}
		}
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
		if (!(object instanceof Command)) {
			resetAllFields();
			return;
		}
		fSelectedCommand = (Command)object;
		fComIDT.setText(fSelectedCommand.getId());
		fExecLink.setEnabled(true);
		fCopyLink.setEnabled(true);
		try {
			populateParams(fSelectedCommand, object);
		} catch (NotDefinedException e) {
			createNoParamComp();
		}
		updatePreviewText();
	}
	
	private void resetAllFields() {
		fSelectedCommand = null;
		fComIDT.setText(F_NO_SEL);
		
		if (fComPrev != null)
			fComPrev.setText(""); //$NON-NLS-1$
		
		fExecLink.setEnabled(false);
		fCopyLink.setEnabled(false);
		
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
		fParamComposite = fCSP.createComposite(fParamParent, GridData.FILL_BOTH, 1, true);
	}
	
	private void updatePreviewText() {
		if (fComPrev != null)
			fComPrev.setText(getFilteredCommand());
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
}
