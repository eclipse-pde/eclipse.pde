package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class ConditionalListSelectionDialog extends ElementListSelectionDialog {

	private String fButtonText;
	private Object[] fElements;
	private Object[] fConditionalElements;
	
	public ConditionalListSelectionDialog(Shell parent, ILabelProvider renderer, String buttonText) {
		super(parent, renderer);
		fButtonText = buttonText;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		int size =	((fElements != null) ? fElements.length : 0) +
					((fConditionalElements != null) ? fConditionalElements.length : 0);
		final Object[] allElements = new Object[size];
		int conditionalStart = 0;
		if (fElements != null) {
			System.arraycopy(fElements, 0, allElements, 0, fElements.length);
			conditionalStart = fElements.length;
		}
		if (fConditionalElements != null)
			System.arraycopy(fConditionalElements, 0, allElements, conditionalStart, fConditionalElements.length);
		
		final Button button = new Button(comp, SWT.CHECK);
		Assert.isNotNull(fButtonText);
		button.setText(fButtonText);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (button.getSelection())
					setListElements(allElements);
				else 
					setListElements(fElements);
			}
		});
		return comp;
	}
	
	public void setElements(Object[] elements) {
		super.setElements(elements);
		fElements = elements;
	}

	public void setConditionalElements(Object[] elements) {
		fConditionalElements = elements;
	}
	
}
