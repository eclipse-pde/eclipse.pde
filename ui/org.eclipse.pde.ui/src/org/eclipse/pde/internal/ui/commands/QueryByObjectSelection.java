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

import java.util.ArrayList;
import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

public class QueryByObjectSelection extends QueryControl {

	private Label fObjectSelectionLabel;
	private Label fLabel;
	private SelectionTracker fSelectionTracker;
	private Object fObjectSelection;

	public QueryByObjectSelection(CommandComposerPart csp, Composite comp) {
		super(csp, comp);
	}

	protected void createGroupContents(Group parent) {
		Composite comp = fToolkit.createComposite(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fLabel = fToolkit.createLabel(comp, "selection: "); //$NON-NLS-1$
		fObjectSelectionLabel = fToolkit.createLabel(comp, "<no selection>", SWT.BORDER); //$NON-NLS-1$
		fObjectSelectionLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWindow != null) {
			ISelectionService selectionService = activeWindow.getSelectionService();
			if (selectionService != null) {
				fSelectionTracker = new SelectionTracker(selectionService);
			}
		}

		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (fSelectionTracker != null) {
					fSelectionTracker.dispose();
				}
			}
		});
	}

	protected String getName() {
		return "Query Commands by selected object"; //$NON-NLS-1$
	}

	private class SelectionTracker implements ISelectionListener {

		private final ISelectionService _selectionService;

		public SelectionTracker(ISelectionService selectionService) {
			_selectionService = selectionService;
			_selectionService.addSelectionListener(this);
		}

		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				Object selected = ((IStructuredSelection) selection).getFirstElement();

				if (selected != null) {
					fObjectSelection = selected;
					String typeName = selected.getClass().getName();
					fObjectSelectionLabel.setToolTipText(typeName);

					int dotPosition = typeName.lastIndexOf('.');
					if (dotPosition != -1) {
						typeName = typeName.substring(dotPosition + 1);
					}
					fObjectSelectionLabel.setText(typeName);
				}
			}
		}

		public void dispose() {
			_selectionService.removeSelectionListener(this);
		}
	}

	private boolean hasTypedParameterMatch(Command command, Object object) throws CommandException {
		IParameter[] params = command.getParameters();
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				IParameter param = params[i];
				ParameterType parameterType = command.getParameterType(param.getId());
				if (parameterType != null) {
					if (parameterType.isCompatible(object))
						return true;
				}
			}
		}

		return false;
	}

	protected Command[] getCommands() {
		Object objectSelection = fObjectSelection;
		if (objectSelection == null)
			return null;

		ArrayList hitList = new ArrayList();
		Command[] commands = getCommandService().getDefinedCommands();
		for (int i = 0; i < commands.length; i++) {
			Command command = commands[i];
			try {
				if (hasTypedParameterMatch(command, objectSelection))
					hitList.add(command);
			} catch (CommandException ex) {
			}
		}

		return (Command[]) hitList.toArray(new Command[hitList.size()]);
	}

	protected void enable(boolean enable) {
		fGroup.setEnabled(enable);
		fLabel.setEnabled(enable);
		fObjectSelectionLabel.setEnabled(enable);
	}
}
