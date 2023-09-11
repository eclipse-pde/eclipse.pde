/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.ui.target.ITargetLocationHandler;

public class ToggleIncludeHandler<DescriptorType> implements ITargetLocationHandler {

	private final int uiMode;
	private final Function<ITargetDefinition, DescriptorType[]> descriptorAccessor;
	private final Function<DescriptorType, NameVersionDescriptor> mapper;
	private final Class<DescriptorType> type;

	public ToggleIncludeHandler(Class<DescriptorType> type, int uiMode,
			Function<ITargetDefinition, DescriptorType[]> descriptorAccessor,
			Function<DescriptorType, NameVersionDescriptor> mapper) {
		this.type = type;
		this.uiMode = uiMode;
		this.descriptorAccessor = descriptorAccessor;
		this.mapper = mapper;
	}

	@Override
	public boolean canDisable(ITargetDefinition target, TreePath treePath) {
		Object lastSegment = treePath.getLastSegment();
		if (type.isInstance(lastSegment)) {
			if (target instanceof TargetDefinition) {
				if (((TargetDefinition) target).getUIMode() != uiMode) {
					return false;
				}
			}
			NameVersionDescriptor[] included = target.getIncluded();
			return included == null || getIndex(mapper.apply(type.cast(lastSegment)), included) > -1;
		}
		return false;
	}

	@Override
	public boolean canEnable(ITargetDefinition target, TreePath treePath) {
		Object lastSegment = treePath.getLastSegment();
		if (type.isInstance(lastSegment)) {
			if (target instanceof TargetDefinition) {
				if (((TargetDefinition) target).getUIMode() != uiMode) {
					return false;
				}
			}
			NameVersionDescriptor[] included = target.getIncluded();
			return included != null && getIndex(mapper.apply(type.cast(lastSegment)), included) < 0;
		}
		return false;
	}

	@Override
	public IStatus toggle(ITargetDefinition target, TreePath[] treePath) {
		Set<NameVersionDescriptor> workingSet = Arrays.stream(treePath).map(TreePath::getLastSegment).filter(type::isInstance)
				.map(type::cast).map(mapper).collect(Collectors.toSet());
		if (workingSet.isEmpty()) {
			return Status.CANCEL_STATUS;
		}
		NameVersionDescriptor[] included = target.getIncluded();
		Stream<NameVersionDescriptor> stream;
		if (included == null) {
			DescriptorType[] all = descriptorAccessor.apply(target);
			if (all == null) {
				return Status.CANCEL_STATUS;
			}
			stream = Arrays.stream(all).map(mapper).filter(Predicate.not(workingSet::contains));
		} else {
			Map<Boolean, List<NameVersionDescriptor>> lists = workingSet.stream()
					.collect(Collectors.partitioningBy(t -> getIndex(t, included) > -1));
			Set<NameVersionDescriptor> exclude = new HashSet<>(lists.get(Boolean.TRUE));
			List<NameVersionDescriptor> include = lists.get(Boolean.FALSE);
			stream = Stream.concat(Arrays.stream(included).distinct().filter(Predicate.not(exclude::contains)),
					include.stream());
		}
		target.setIncluded(stream.toArray(NameVersionDescriptor[]::new));
		if (target instanceof TargetDefinition) {
			((TargetDefinition) target).incrementSequenceNumber();
		}
		return Status.OK_STATUS;
	}

	private static int getIndex(NameVersionDescriptor searchDescriptor, NameVersionDescriptor[] included) {
		if (included != null && searchDescriptor != null) {
			for (int i = 0; i < included.length; i++) {
				NameVersionDescriptor descriptor = included[i];
				if (searchDescriptor.equals(descriptor)) {
					return i;
				}
			}
		}
		return -1;
	}

}