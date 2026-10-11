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

import static org.eclipse.e4.ui.model.application.descriptor.basic.impl.BasicPackageImpl.Literals.PART_DESCRIPTOR;
import static org.eclipse.e4.ui.model.application.descriptor.basic.impl.BasicPackageImpl.Literals.PART_DESCRIPTOR_CONTAINER__DESCRIPTORS;
import static org.eclipse.e4.ui.model.application.descriptor.basic.impl.BasicPackageImpl.Literals.PART_DESCRIPTOR__TRIM_BARS;

import java.util.function.Function;

import org.eclipse.e4.tools.emf.ui.internal.Messages;
import org.eclipse.e4.tools.emf.ui.internal.ResourceProvider;
import org.eclipse.e4.tools.emf.ui.internal.common.component.virtual.VirtualListEditor.Creatable;
import org.eclipse.e4.tools.emf.ui.internal.common.component.virtual.VirtualListEditor.ImportAction;
import org.eclipse.e4.tools.emf.ui.internal.common.component.virtual.VirtualListEditor.Spec;
import org.eclipse.e4.tools.emf.ui.internal.imp.RegistryUtil;
import org.eclipse.e4.ui.model.application.commands.MCategory;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MHandler;
import org.eclipse.e4.ui.model.application.commands.impl.CommandsPackageImpl;
import org.eclipse.e4.ui.model.application.descriptor.basic.MPartDescriptor;
import org.eclipse.e4.ui.model.application.impl.ApplicationPackageImpl;
import org.eclipse.e4.ui.model.application.ui.advanced.impl.AdvancedPackageImpl;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.impl.BasicPackageImpl;
import org.eclipse.e4.ui.model.application.ui.impl.UiPackageImpl;
import org.eclipse.e4.ui.model.application.ui.menu.impl.MenuPackageImpl;
import org.eclipse.e4.ui.model.fragment.impl.FragmentPackageImpl;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;

/** The {@link Spec}s of the virtual list nodes of the model editor tree. */
public final class VirtualListSpecs {

	public static final Spec ADDONS = Spec.of(m -> m.VApplicationAddons_TreeLabel,
			m -> m.VApplicationAddons_TreeLabelDescription, ApplicationPackageImpl.Literals.APPLICATION__ADDONS,
			create(ApplicationPackageImpl.Literals.ADDON, m -> m.VApplicationAddons_AddAddon,
					ResourceProvider.IMG_Addons));

	public static final Spec CATEGORIES = Spec
			.of(m -> m.VApplicationCategoriesEditor_TreeLabel, m -> m.VApplicationCategoriesEditor_TreeLabelDescription,
					ApplicationPackageImpl.Literals.APPLICATION__CATEGORIES,
					create(CommandsPackageImpl.Literals.CATEGORY, m -> m.VApplicationCategoriesEditor_AddCategory,
							ResourceProvider.IMG_Category))
			.withImports(MCategory.class,
					new ImportAction(m -> m.VApplicationCategoriesEditor_AddCategory, ResourceProvider.IMG_Category));

	public static final Spec COMMANDS = Spec
			.of(m -> m.VCommandEditor_TreeLabel, m -> m.VCommandEditor_TreeLabelDescription,
					ApplicationPackageImpl.Literals.APPLICATION__COMMANDS,
					create(CommandsPackageImpl.Literals.COMMAND, m -> m.VCommandEditor_AddCommand,
							ResourceProvider.IMG_Command))
			.withImports(MCommand.class, new ImportAction(m -> m.VCommandEditor_ImportCommands,
					ResourceProvider.IMG_Command));

	public static final Spec HANDLERS = Spec
			.of(m -> m.VHandlerEditor_TreeLabel, m -> m.VHandlerEditor_TreeLabelDescription,
					CommandsPackageImpl.Literals.HANDLER_CONTAINER__HANDLERS,
					create(CommandsPackageImpl.Literals.HANDLER, m -> m.VHandlerEditor_AddHandler,
							ResourceProvider.IMG_Handler))
			.withNoGroup()
			.withImports(MHandler.class,
					new ImportAction(m -> m.VHandlerEditor_AddHandler, ResourceProvider.IMG_Handler));

	public static final Spec BINDING_TABLES = Spec.of(m -> m.VBindingTableEditor_Label,
			m -> m.VBindingTableEditor_Description, CommandsPackageImpl.Literals.BINDING_TABLE_CONTAINER__BINDING_TABLES,
			create(CommandsPackageImpl.Literals.BINDING_TABLE, m -> m.VBindingTableEditor_AddBindingTable,
					ResourceProvider.IMG_BindingTable));

	public static final Spec ROOT_CONTEXTS = Spec.of(m -> m.VRootBindingContexts_TreeLabel,
			m -> m.VRootBindingContexts_TreeLabelDescription,
			CommandsPackageImpl.Literals.BINDING_TABLE_CONTAINER__ROOT_CONTEXT,
			create(CommandsPackageImpl.Literals.BINDING_CONTEXT, m -> m.VRootBindingContexts_AddContext,
					ResourceProvider.IMG_BindingContext));

	public static final Spec PARAMETERS = Spec.of(m -> m.VItemParametersEditor_TreeLabel,
			m -> m.VItemParametersEditor_TreeLabelDescription, MenuPackageImpl.Literals.HANDLED_ITEM__PARAMETERS,
			create(CommandsPackageImpl.Literals.PARAMETER, m -> m.VItemParametersEditor_AddParameter,
					ResourceProvider.IMG_Parameter)).withNoGroup();

	public static final Spec MODEL_FRAGMENTS = Spec.of(m -> m.VModelFragmentsEditor_TreeLabel,
			m -> m.VModelFragmentsEditor_TreeLabelDescription, FragmentPackageImpl.Literals.MODEL_FRAGMENTS__FRAGMENTS,
			create(FragmentPackageImpl.Literals.STRING_MODEL_FRAGMENT, m -> m.VModelFragmentsEditor_AddFragment,
					ResourceProvider.IMG_ModelFragments)).withNoGroup();

	public static final Spec MENU_CONTRIBUTIONS = Spec.of(m -> m.VMenuContributionsEditor_TreeLabel,
			m -> m.VMenuContributionsEditor_TreeLabelDescription,
			MenuPackageImpl.Literals.MENU_CONTRIBUTIONS__MENU_CONTRIBUTIONS,
			create(MenuPackageImpl.Literals.MENU_CONTRIBUTION, m -> m.VMenuContributionsEditor_AddMenuContribution,
					ResourceProvider.IMG_MenuContribution));

	public static final Spec TOOLBAR_CONTRIBUTIONS = Spec.of(m -> m.VToolBarContributionsEditor_TreeLabel,
			m -> m.VToolBarContributionsEditor_TreeLabelDescription,
			MenuPackageImpl.Literals.TOOL_BAR_CONTRIBUTIONS__TOOL_BAR_CONTRIBUTIONS,
			create(MenuPackageImpl.Literals.TOOL_BAR_CONTRIBUTION,
					m -> m.VToolBarContributionsEditor_AddToolBarContribution,
					ResourceProvider.IMG_ToolBarContribution));

	public static final Spec TRIM_CONTRIBUTIONS = Spec.of(m -> m.VTrimContributionsEditor_TreeLabel,
			m -> m.VTrimContributionsEditor_TreeLabelDescription,
			MenuPackageImpl.Literals.TRIM_CONTRIBUTIONS__TRIM_CONTRIBUTIONS,
			create(MenuPackageImpl.Literals.TRIM_CONTRIBUTION, m -> m.VTrimContributionsEditor_AddTrimContribution,
					ResourceProvider.IMG_TrimContribution));

	public static final Spec PART_DESCRIPTORS = Spec
			.of(m -> m.VPartDescriptor_TreeLabel, m -> m.VPartDescriptor_TreeLabelDescription,
					PART_DESCRIPTOR_CONTAINER__DESCRIPTORS,
					create(PART_DESCRIPTOR, m -> m.VPartDescriptor_AddPartDescriptor,
							ResourceProvider.IMG_PartDescriptor))
			.withImports(MPartDescriptor.class,
					new ImportAction(m -> m.VPartDescriptor_Views, ResourceProvider.IMG_Part, RegistryUtil.HINT_VIEW),
					new ImportAction(m -> m.VPartDescriptor_Editors, ResourceProvider.IMG_Part,
							RegistryUtil.HINT_EDITOR),
					new ImportAction(m -> m.VPartDescriptor_ViewAsCompatibilityView, ResourceProvider.IMG_Part,
							RegistryUtil.HINT_COMPAT_VIEW));

	public static final Spec PART_TRIMS = trims(m -> m.VPartTrimEditor_TreeLabel,
			m -> m.VPartTrimEditor_TreeLabelDescription, m -> m.VPartTrimEditor_AddTrim,
			BasicPackageImpl.Literals.PART__TRIM_BARS);

	public static final Spec PART_DESCRIPTOR_TRIMS = trims(m -> m.VPartDescriptorTrimEditor_TreeLabel,
			m -> m.VPartDescriptorTrimEditor_TreeLabelDescription, m -> m.VPartDescriptorTrimEditor_AddTrim,
			PART_DESCRIPTOR__TRIM_BARS);

	public static final Spec PERSPECTIVE_TRIMS = trims(m -> m.VPerspectiveTrimEditor_TreeLabel,
			m -> m.VPerspectiveTrimEditor_TreeLabelDescription, m -> m.VPerspectiveTrimEditor_AddTrim,
			AdvancedPackageImpl.Literals.PERSPECTIVE__TRIM_BARS);

	public static final Spec WINDOW_TRIMS = trims(m -> m.VWindowTrimEditor_TreeLabel,
			m -> m.VWindowTrimEditor_TreeLabelDescription, m -> m.VWindowTrimEditor_AddTrim,
			BasicPackageImpl.Literals.TRIMMED_WINDOW__TRIM_BARS);

	public static final Spec APPLICATION_WINDOWS = windows(UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN);

	public static final Spec WINDOW_WINDOWS = windows(BasicPackageImpl.Literals.WINDOW__WINDOWS);

	public static final Spec PERSPECTIVE_WINDOWS = windows(AdvancedPackageImpl.Literals.PERSPECTIVE__WINDOWS);

	public static final Spec WINDOW_CONTROLS = Spec
			.of(m -> m.VWindowControlEditor_TreeLabel, m -> m.VWindowControlEditor_TreeLabelDescription,
					UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN,
					create(AdvancedPackageImpl.Literals.PERSPECTIVE_STACK, m -> m.VWindowControlEditor_AddPerspectiveStack,
							ResourceProvider.IMG_PerspectiveStack),
					create(BasicPackageImpl.Literals.PART_SASH_CONTAINER, m -> m.VWindowControlEditor_AddPartSashContainer,
							ResourceProvider.IMG_PartSashContainer_vertical),
					create(BasicPackageImpl.Literals.PART_STACK, m -> m.VWindowControlEditor_AddPartStack,
							ResourceProvider.IMG_PartStack),
					create(BasicPackageImpl.Literals.PART, m -> m.VWindowControlEditor_AddPart,
							ResourceProvider.IMG_Part),
					create(AdvancedPackageImpl.Literals.AREA, m -> m.VWindowControlEditor_AddArea,
							ResourceProvider.IMG_Area_vertical))
			.withNoGroup().withSortedActions();

	public static final Spec PERSPECTIVE_CONTROLS = Spec
			.of(m -> m.VWindowControlEditor_TreeLabel, m -> m.VWindowControlEditor_TreeLabelDescription,
					UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN,
					create(BasicPackageImpl.Literals.PART_SASH_CONTAINER, m -> m.VWindowControlEditor_AddPartSashContainer,
							ResourceProvider.IMG_PartSashContainer_vertical),
					create(BasicPackageImpl.Literals.PART_STACK, m -> m.VWindowControlEditor_AddPartStack,
							ResourceProvider.IMG_PartStack),
					create(BasicPackageImpl.Literals.PART, m -> m.VWindowControlEditor_AddPart,
							ResourceProvider.IMG_Part),
					create(AdvancedPackageImpl.Literals.AREA, m -> m.VWindowControlEditor_AddArea,
							ResourceProvider.IMG_Area_vertical),
					create(AdvancedPackageImpl.Literals.PLACEHOLDER, m -> m.PerspectiveEditor_AddPlaceholder,
							ResourceProvider.IMG_Placeholder))
			.withNoGroup();

	public static final Spec CONTROLS = Spec
			.of(m -> m.VControlsEditor_TreeLabel, m -> m.VControlsEditor_TreeLabelDescription,
					UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN,
					create(BasicPackageImpl.Literals.PART_SASH_CONTAINER, m -> m.CompositePartEditor_AddPartSashContainer,
							ResourceProvider.IMG_PartSashContainer),
					create(BasicPackageImpl.Literals.PART_STACK, m -> m.CompositePartEditor_AddPartStack,
							ResourceProvider.IMG_PartStack),
					create(BasicPackageImpl.Literals.PART, m -> m.CompositePartEditor_AddPart,
							ResourceProvider.IMG_Part),
					create(AdvancedPackageImpl.Literals.AREA, m -> m.CompositePartEditor_AddArea,
							ResourceProvider.IMG_Area),
					create(AdvancedPackageImpl.Literals.PLACEHOLDER, m -> m.CompositePartEditor_AddPlaceholder,
							ResourceProvider.IMG_Placeholder))
			.withNoGroup().withInitialType(BasicPackageImpl.Literals.PART)
			.withContributionsFor(BasicPackageImpl.Literals.PART_STACK)
			.withImports(MPart.class,
					new ImportAction(m -> "Views", ResourceProvider.IMG_Part, RegistryUtil.HINT_VIEW), //$NON-NLS-1$
					new ImportAction(m -> "Editors", ResourceProvider.IMG_Part, RegistryUtil.HINT_EDITOR)); //$NON-NLS-1$

	public static final Spec SHARED_ELEMENTS = Spec
			.of(m -> m.VWindowSharedElementsEditor_TreeLabel, m -> m.VWindowSharedElementsEditor_TreeLabelDescription,
					BasicPackageImpl.Literals.WINDOW__SHARED_ELEMENTS,
					create(BasicPackageImpl.Literals.PART_SASH_CONTAINER,
							m -> m.VWindowSharedElementsEditor_AddPartSashContainer,
							ResourceProvider.IMG_PartSashContainer),
					create(BasicPackageImpl.Literals.PART, m -> m.VWindowSharedElementsEditor_AddPart,
							ResourceProvider.IMG_Part),
					create(BasicPackageImpl.Literals.PART_STACK, m -> m.VWindowSharedElementsEditor_AddPartStack,
							ResourceProvider.IMG_PartStack),
					create(AdvancedPackageImpl.Literals.AREA, m -> m.VWindowSharedElementsEditor_Area,
							ResourceProvider.IMG_Area))
			.withNoGroup().withInitialType(BasicPackageImpl.Literals.PART)
			.withImports(MPart.class,
					new ImportAction(m -> m.VWindowSharedElementsEditor_Views, ResourceProvider.IMG_Part,
							RegistryUtil.HINT_VIEW),
					new ImportAction(m -> m.VWindowSharedElementsEditor_Editors, ResourceProvider.IMG_Part,
							RegistryUtil.HINT_EDITOR),
					new ImportAction(m -> m.VWindowSharedElementsEditor_ViewAsCompatibilityView,
							ResourceProvider.IMG_Part, RegistryUtil.HINT_COMPAT_VIEW));

	public static final Spec SNIPPETS = Spec
			.of(m -> m.VSnippetsEditor_Snippets, m -> m.VSnippetsEditor_Snippets,
					UiPackageImpl.Literals.SNIPPET_CONTAINER__SNIPPETS,
					create(AdvancedPackageImpl.Literals.AREA, m -> m.VWindowControlEditor_AddArea,
							ResourceProvider.IMG_Area_vertical),
					create(BasicPackageImpl.Literals.PART, m -> m.VWindowControlEditor_AddPart,
							ResourceProvider.IMG_Part),
					create(BasicPackageImpl.Literals.PART_SASH_CONTAINER, m -> m.VWindowControlEditor_AddPartSashContainer,
							ResourceProvider.IMG_PartSashContainer_vertical),
					create(BasicPackageImpl.Literals.PART_STACK, m -> m.VWindowControlEditor_AddPartStack,
							ResourceProvider.IMG_PartStack),
					create(BasicPackageImpl.Literals.COMPOSITE_PART, m -> m.VWindowControlEditor_AddCompositePart,
							ResourceProvider.IMG_PartSashContainer),
					create(AdvancedPackageImpl.Literals.PERSPECTIVE, m -> m.PerspectiveStackEditor_AddPerspective,
							ResourceProvider.IMG_Perspective),
					create(AdvancedPackageImpl.Literals.PERSPECTIVE_STACK, m -> m.VWindowControlEditor_AddPerspectiveStack,
							ResourceProvider.IMG_PerspectiveStack),
					create(MenuPackageImpl.Literals.TRIM_CONTRIBUTION, m -> m.VTrimContributionsEditor_AddTrimContribution,
							ResourceProvider.IMG_TrimContribution),
					create(BasicPackageImpl.Literals.TRIMMED_WINDOW, m -> m.VWindowEditor_AddTrimmedWindow,
							ResourceProvider.IMG_Window),
					create(BasicPackageImpl.Literals.WINDOW, m -> m.VWindowEditor_AddWindow,
							ResourceProvider.IMG_Window),
					create(BasicPackageImpl.Literals.TRIM_BAR, m -> m.VWindowTrimEditor_AddTrim,
							ResourceProvider.IMG_WindowTrim))
			.withNoGroup().withSortedActions().withInitialType(AdvancedPackageImpl.Literals.PERSPECTIVE);

	private VirtualListSpecs() {
	}

	private static Creatable create(EClass eClass, Function<Messages, String> label, String iconKey) {
		return new Creatable(eClass, label, iconKey);
	}

	private static Spec trims(Function<Messages, String> label, Function<Messages, String> description,
			Function<Messages, String> addLabel, EReference feature) {
		return Spec.of(label, description, feature,
				create(BasicPackageImpl.Literals.TRIM_BAR, addLabel, ResourceProvider.IMG_WindowTrim)).withNoGroup();
	}

	private static Spec windows(EReference feature) {
		return Spec.of(m -> m.VWindowEditor_TreeLabel, m -> m.VWindowEditor_TreeLabelDescription, feature,
				create(BasicPackageImpl.Literals.TRIMMED_WINDOW, m -> m.VWindowEditor_AddTrimmedWindow,
						ResourceProvider.IMG_Window),
				create(BasicPackageImpl.Literals.WINDOW, m -> m.VWindowEditor_AddWindow, ResourceProvider.IMG_Window))
				.withNoGroup();
	}
}
