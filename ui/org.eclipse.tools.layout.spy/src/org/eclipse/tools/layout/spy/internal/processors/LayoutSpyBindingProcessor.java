/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.tools.layout.spy.internal.processors;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MBindingContext;
import org.eclipse.e4.ui.model.application.commands.MBindingTable;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MKeyBinding;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

/**
 * Contributes the {@code M1+M2+M3+F9} key binding for the layout spy command to
 * the {@code org.eclipse.ui.contexts.dialogAndWindow} binding context.
 * <p>
 * The binding is added programmatically rather than through the model fragment
 * so that the binding context is resolved by object rather than by an
 * {@code <imports>} element. Importing the context by id is ambiguous because
 * the legacy binding table generated for that context shares its element id,
 * which makes the fragment import resolve to the wrong element.
 */
public class LayoutSpyBindingProcessor {

	private static final String COMMAND_ID = "org.eclipse.tools.layout.spy.commands.layoutSpyCommand"; //$NON-NLS-1$
	private static final String BINDING_TABLE_ID = "org.eclipse.tools.layout.spy.bindingtable"; //$NON-NLS-1$
	private static final String KEY_BINDING_ID = "org.eclipse.tools.layout.spy.keybinding.layoutSpy"; //$NON-NLS-1$
	private static final String KEY_SEQUENCE = "M1+M2+M3+F9"; //$NON-NLS-1$
	private static final String DIALOG_AND_WINDOW_CONTEXT_ID = "org.eclipse.ui.contexts.dialogAndWindow"; //$NON-NLS-1$

	@Execute
	public void process(MApplication application, EModelService modelService) {
		MCommand command = findCommand(application);
		if (command == null) {
			// The command is contributed by fragment.e4xmi; without it there is nothing to bind.
			return;
		}
		MBindingContext context = findContext(application.getRootContext());
		if (context == null) {
			context = findContext(application.getBindingContexts());
		}
		if (context == null) {
			return;
		}
		MBindingTable table = getOrCreateBindingTable(application, modelService, context);
		addKeyBinding(table, command, modelService);
	}

	private static MCommand findCommand(MApplication application) {
		for (MCommand command : application.getCommands()) {
			if (COMMAND_ID.equals(command.getElementId())) {
				return command;
			}
		}
		return null;
	}

	private static MBindingContext findContext(List<MBindingContext> contexts) {
		for (MBindingContext context : contexts) {
			if (DIALOG_AND_WINDOW_CONTEXT_ID.equals(context.getElementId())) {
				return context;
			}
			MBindingContext child = findContext(context.getChildren());
			if (child != null) {
				return child;
			}
		}
		return null;
	}

	private static MBindingTable getOrCreateBindingTable(MApplication application, EModelService modelService,
			MBindingContext context) {
		for (MBindingTable table : application.getBindingTables()) {
			if (BINDING_TABLE_ID.equals(table.getElementId())) {
				return table;
			}
		}
		MBindingTable table = modelService.createModelElement(MBindingTable.class);
		table.setElementId(BINDING_TABLE_ID);
		table.setBindingContext(context);
		table.getPersistedState().put(IWorkbench.PERSIST_STATE, Boolean.FALSE.toString());
		application.getBindingTables().add(table);
		return table;
	}

	private static void addKeyBinding(MBindingTable table, MCommand command, EModelService modelService) {
		for (MKeyBinding binding : table.getBindings()) {
			if (KEY_BINDING_ID.equals(binding.getElementId())) {
				return;
			}
		}
		MKeyBinding binding = modelService.createModelElement(MKeyBinding.class);
		binding.setElementId(KEY_BINDING_ID);
		binding.setKeySequence(KEY_SEQUENCE);
		binding.setCommand(command);
		binding.setContributorURI(command.getContributorURI());
		binding.getPersistedState().put(IWorkbench.PERSIST_STATE, Boolean.FALSE.toString());
		table.getBindings().add(binding);
	}
}
