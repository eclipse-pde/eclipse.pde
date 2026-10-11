/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * vogella GmbH - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.component.virtual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.e4.tools.emf.ui.common.IEditorFeature.FeatureClass;
import org.eclipse.e4.tools.emf.ui.common.component.AbstractComponentEditor;
import org.eclipse.e4.tools.emf.ui.internal.Messages;
import org.eclipse.e4.tools.emf.ui.internal.common.AbstractPickList;
import org.eclipse.e4.tools.emf.ui.internal.common.AbstractPickList.PickListFeatures;
import org.eclipse.e4.tools.emf.ui.internal.common.E4PickList;
import org.eclipse.e4.tools.emf.ui.internal.common.EClassLabelProvider;
import org.eclipse.e4.tools.emf.ui.internal.common.VirtualEntry;
import org.eclipse.e4.tools.emf.ui.internal.imp.ModelImportWizard;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import jakarta.annotation.PostConstruct;

/**
 * Editor of a virtual tree node that lists the elements of one containment
 * feature and creates new ones of the types named by its {@link Spec}.
 */
public class VirtualListEditor extends AbstractComponentEditor<EObject> {

	/** A type the user can create, with the label and icon of its "add" action. */
	public record Creatable(EClass eClass, Function<Messages, String> label, String iconKey) {
	}

	/** An action of the import menu. */
	public record ImportAction(Function<Messages, String> label, String iconKey, String hint) {
		public ImportAction(Function<Messages, String> label, String iconKey) {
			this(label, iconKey, ""); //$NON-NLS-1$
		}
	}

	/**
	 * Describes one virtual list. A single creatable type has no type picker, several have one.
	 */
	public record Spec(Function<Messages, String> label, Function<Messages, String> description, EReference feature,
			List<Creatable> creatables, EClass initialType, boolean noGroup, boolean sortActions,
			Class<? extends MApplicationElement> importType, List<ImportAction> imports, EClass contributionsFor) {

		public static Spec of(Function<Messages, String> label, Function<Messages, String> description,
				EReference feature, Creatable... creatables) {
			return new Spec(label, description, feature, List.of(creatables), creatables[0].eClass(), false, false,
					null, List.of(), null);
		}

		/** Hides the group button of the list. */
		public Spec withNoGroup() {
			return new Spec(label, description, feature, creatables, initialType, true, sortActions, importType,
					imports, contributionsFor);
		}

		/** Sorts the create actions by their label. */
		public Spec withSortedActions() {
			return new Spec(label, description, feature, creatables, initialType, noGroup, true, importType, imports,
					contributionsFor);
		}

		/** Preselects the given type in the picker. */
		public Spec withInitialType(EClass type) {
			return new Spec(label, description, feature, creatables, type, noGroup, sortActions, importType, imports,
					contributionsFor);
		}

		/** Adds import actions that create elements of the given type through the import wizard. */
		public Spec withImports(Class<? extends MApplicationElement> type, ImportAction... actions) {
			return new Spec(label, description, feature, creatables, initialType, noGroup, sortActions, type,
					List.of(actions), contributionsFor);
		}

		/** Adds the create actions that extensions contribute for children of the given type. */
		public Spec withContributionsFor(EClass type) {
			return new Spec(label, description, feature, creatables, initialType, noGroup, sortActions, importType,
					imports, type);
		}

		public List<EClass> eClasses() {
			return creatables.stream().map(Creatable::eClass).toList();
		}
	}

	private final Spec spec;
	private final List<Action> actions = new ArrayList<>();
	private final List<Action> actionsImport = new ArrayList<>();
	private Composite composite;
	private TableViewer viewer;

	public VirtualListEditor(Spec spec) {
		this.spec = spec;
	}

	@PostConstruct
	void init() {
		for (Creatable c : spec.creatables()) {
			actions.add(new Action(c.label().apply(Messages), createImageDescriptor(c.iconKey())) {
				@Override
				public void run() {
					handleAdd(c.eClass());
				}
			});
		}
		if (spec.sortActions()) {
			actions.sort(Comparator.comparing(Action::getText));
		}
		if (spec.contributionsFor() != null) {
			for (FeatureClass c : getEditor().getFeatureClasses(spec.contributionsFor(), spec.feature())) {
				actions.add(new Action(c.label, createImageDescriptor(c.iconId)) {
					@Override
					public void run() {
						handleAdd(c.eClass);
					}
				});
			}
		}
		for (ImportAction a : spec.imports()) {
			actionsImport.add(new Action(a.label().apply(Messages), createImageDescriptor(a.iconKey())) {
				@Override
				public void run() {
					handleImport(a.hint());
				}
			});
		}
	}

	@Override
	public String getLabel(Object element) {
		return spec.label().apply(Messages);
	}

	@Override
	public String getDetailLabel(Object element) {
		return null;
	}

	@Override
	public String getDescription(Object element) {
		return spec.description().apply(Messages);
	}

	@Override
	public Composite doGetEditor(Composite parent, Object object) {
		if (composite == null) {
			composite = createForm(parent);
		}
		@SuppressWarnings("unchecked")
		VirtualEntry<EObject, ?> o = (VirtualEntry<EObject, ?>) object;
		viewer.setInput(o.getList());
		getMaster().setValue(o.getOriginalParent());
		return composite;
	}

	private Composite createForm(Composite parent) {
		CTabFolder folder = new CTabFolder(parent, SWT.BOTTOM);

		CTabItem item = new CTabItem(folder, SWT.NONE);
		item.setText(Messages.ModelTooling_Common_TabDefault);

		Composite container = createScrollableContainer(folder);
		item.setControl(container.getParent());

		boolean multiple = spec.creatables().size() > 1;
		List<PickListFeatures> features = new ArrayList<>();
		if (!multiple) {
			features.add(PickListFeatures.NO_PICKER);
		}
		if (spec.noGroup()) {
			features.add(PickListFeatures.NO_GROUP);
		}

		AbstractPickList pickList = new E4PickList(container, SWT.NONE, features, this, spec.feature()) {
			@Override
			protected void addPressed() {
				EClass eClass = multiple ? (EClass) getSelection().getFirstElement()
						: spec.creatables().get(0).eClass();
				if (eClass != null) {
					handleAdd(eClass);
				}
			}

			@Override
			protected List<?> getContainerChildren(Object master) {
				return master instanceof EObject eObject ? (List<?>) eObject.eGet(spec.feature()) : null;
			}
		};
		pickList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		viewer = pickList.getList();
		if (multiple) {
			pickList.setLabelProvider(new EClassLabelProvider(getEditor()));
			pickList.setInput(spec.eClasses().toArray(EClass[]::new));
			pickList.setSelection(new StructuredSelection(spec.initialType()));
		}

		folder.setSelection(0);
		return folder;
	}

	private void handleAdd(EClass eClass) {
		addToModel(EcoreUtil.create(eClass));
	}

	private void addToModel(EObject eObject) {
		setElementId(eObject);

		Command cmd = AddCommand.create(getEditingDomain(), getMaster().getValue(), spec.feature(), eObject);
		if (cmd.canExecute()) {
			getEditingDomain().getCommandStack().execute(cmd);
			getEditor().setSelection(eObject);
		}
	}

	private void handleImport(String hint) {
		ModelImportWizard wizard = new ModelImportWizard(spec.importType(), this, hint, resourcePool);
		WizardDialog wizardDialog = new WizardDialog(viewer.getControl().getShell(), wizard);
		if (wizardDialog.open() == Window.OK) {
			for (MApplicationElement element : wizard.getElements(spec.importType())) {
				addToModel((EObject) element);
			}
		}
	}

	@Override
	public IObservableList<?> getChildList(Object element) {
		return null;
	}

	@Override
	public List<Action> getActions(Object element) {
		return new ArrayList<>(actions);
	}

	@Override
	public List<Action> getActionsImport(Object element) {
		return new ArrayList<>(actionsImport);
	}
}
