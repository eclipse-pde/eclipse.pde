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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.pde.osgi.xmlns.metatype.v1_4.Tad;
import org.eclipse.pde.osgi.xmlns.metatype.v1_4.Ticon;
import org.eclipse.pde.osgi.xmlns.metatype.v1_4.Tocd;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

class JaxbObjectClassDefinition implements ObjectClassDefinition {

	private final Tocd ocd;

	public JaxbObjectClassDefinition(Tocd ocd) {
		if (ocd == null) {
			throw new NullPointerException();
		}
		this.ocd = ocd;
	}

	@Override
	public String getName() {
		return ocd.getName();
	}

	@Override
	public String getID() {
		return ocd.getId();
	}

	@Override
	public String getDescription() {
		return ocd.getDescription();
	}

	@Override
	public AttributeDefinition[] getAttributeDefinitions(int filter) {
		Stream<Tad> stream = ocd.getADOrIconOrAny().stream().filter(Tad.class::isInstance).map(Tad.class::cast);
		if (filter == ObjectClassDefinition.OPTIONAL) {
			stream = stream.filter(Predicate.not(Tad::isRequired));
		} else if (filter == ObjectClassDefinition.REQUIRED) {
			stream = stream.filter(Tad::isRequired);
		} else if (filter != ObjectClassDefinition.ALL) {
			return null;
		}
		AttributeDefinition[] definitions = stream.toArray(AttributeDefinition[]::new);
		if (definitions.length == 0) {
			return null;
		}
		return definitions;
	}

	@Override
	public InputStream getIcon(int size) throws IOException {
		List<Ticon> icons = ocd.getADOrIconOrAny().stream().filter(Ticon.class::isInstance).map(Ticon.class::cast)
				.toList();
		if (icons.isEmpty()) {
			throw new FileNotFoundException();
		}
		for (Ticon icon : icons) {
			if (icon.getSize().intValue() == size) {
				return new URL(icon.getResource()).openStream();
			}
		}
		return new URL(icons.get(0).getResource()).openStream();
	}

}