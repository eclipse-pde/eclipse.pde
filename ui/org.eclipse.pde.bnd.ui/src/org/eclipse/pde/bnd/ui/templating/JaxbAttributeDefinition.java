/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.pde.bnd.ui.templating;

import org.eclipse.pde.osgi.xmlns.metatype.v1_4.Tad;
import org.eclipse.pde.osgi.xmlns.metatype.v1_4.Toption;
import org.osgi.service.metatype.AttributeDefinition;

class JaxbAttributeDefinition implements AttributeDefinition {

	private final Tad ad;

	public JaxbAttributeDefinition(Tad ad) {
		this.ad = ad;
	}

	@Override
	public String getName() {
		return ad.getName();
	}

	@Override
	public String getID() {
		return ad.getId();
	}

	@Override
	public String getDescription() {
		return ad.getDescription();
	}

	@Override
	public int getCardinality() {
		return ad.getCardinality();
	}

	@Override
	public int getType() {
		return ad.getType().ordinal() + 1;
	}

	@Override
	public String[] getOptionValues() {
		return ad.getOptionOrAny().stream().filter(Toption.class::isInstance).map(Toption.class::cast)
				.map(Toption::getValue).toArray(String[]::new);
	}

	@Override
	public String[] getOptionLabels() {
		return ad.getOptionOrAny().stream().filter(Toption.class::isInstance).map(Toption.class::cast)
				.map(Toption::getLabel).toArray(String[]::new);
	}

	@Override
	public String validate(String value) {
		return null;
	}

	@Override
	public String[] getDefaultValue() {
		return new String[] { ad.getDefault() };
	}
}