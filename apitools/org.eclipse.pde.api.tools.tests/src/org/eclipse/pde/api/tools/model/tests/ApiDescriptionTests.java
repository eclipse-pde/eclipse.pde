/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.model.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.ApiDescriptionProcessor;
import org.eclipse.pde.api.tools.internal.ApiDescriptionXmlCreator;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.junit.Test;

/**
 * Tests API manifest implementation.
 *
 * @since 1.0.0
 */
public class ApiDescriptionTests {

	private IApiDescription fManifest = buildManifest();

	/**
	 * Wraps an element with its API description
	 */
	class ElementDescription {

		public IElementDescriptor fElement;
		public int fVis, fRes;
		public String fComponent = null;

		public ElementDescription(IElementDescriptor element, int visibility, int restrictions) {
			fElement = element;
			fVis = visibility;
			fRes = restrictions;
		}

		public ElementDescription(String componentContext, IElementDescriptor element, int visibility, int restrictions) {
			this(element, visibility, restrictions);
			fComponent = componentContext;
		}

		@Override
		public String toString() {
			return fElement.toString();
		}
	}

	/**
	 * Creates and returns a container for an element and expected API settings.
	 *
	 * @param element
	 * @param visibility
	 * @param restrictions
	 * @return
	 */
	public ElementDescription newDescription(IElementDescriptor element, int visibility, int restrictions) {
		return new ElementDescription(element, visibility, restrictions);
	}

	/**
	 * Creates a new empty API component description, not owned by any component.
	 *
	 * @return
	 */
	protected IApiDescription newDescription() {
		return new ApiDescription(null);
	}

	/**
	 * Builds a test manifest with the following information:
	 *
	 * default package: API
	 * 		class A
	 * 		class B 		- @noinstantiate
	 * 			method m1 	- @noextend
	 * 		class C 		- @noinstantiate @noextend
	 * 		class D 		- @noreference
	 * 			field f1 	- @noreference
	 * 		interface IA
	 * 		interface IB 	- @noimplement
	 * package a.b.c: API
	 * 		class A 		- @noinstantiate @noextend
	 * 			method m2 	- @noreference
	 * 		class B
	 * 		class C			- @noextend
	 * 		class D 		- @noinstantiate
	 * 			field f2 	- @noreference
	 * 		interface IC 	- @noimplement
	 * 		interface ID
	 * package a.b.c.spi: API
	 * 		class SpiA
	 * 		class SpiB 		- @noextend
	 * 			method m3
	 * 		class SpiC 		- @noinstantiate
	 * 			field f4	- @noreference
	 * 			method m4	- @noextend
	 * 		class SpiD 		- @noextend @noinstantiate
	 * 		class SpiE 		- @noreference
	 * 			field f3
	 * 		interface ISpiA
	 * 		interface ISpiB - @noimplement
	 * package a.b.c.internal: PRIVATE
	 * 		class PA
	 * 		class PB
	 * 		class PC
	 * 		class PD
	 *
	 *
	 * @return
	 */
	protected IApiDescription buildManifest() {
		IApiDescription manifest = newDescription();
		// add packages to the manifest with default rules - public API
		manifest.setVisibility(Factory.packageDescriptor(""), VisibilityModifiers.API); //$NON-NLS-1$
		manifest.setVisibility(Factory.packageDescriptor("a.b.c"), VisibilityModifiers.API); //$NON-NLS-1$
		manifest.setVisibility(Factory.packageDescriptor("a.b.c.spi"), VisibilityModifiers.SPI); //$NON-NLS-1$
		manifest.setVisibility(Factory.packageDescriptor("a.b.c.internal"), VisibilityModifiers.PRIVATE); //$NON-NLS-1$

		// add type specific settings
		manifest.setRestrictions(Factory.typeDescriptor("B"), RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
		manifest.setRestrictions(Factory.typeDescriptor("C"), RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
		manifest.setRestrictions(Factory.typeDescriptor("D"), RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		manifest.setRestrictions(Factory.typeDescriptor("IB"), RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$

		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.A"), RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.C"), RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.D"), RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.IC"), RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$

		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.spi.SpiB"), RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.spi.SpiC"), RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.spi.SpiD"), RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.spi.SpiE"), RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.spi.ISpiB"), RestrictionModifiers.NO_IMPLEMENT);		 //$NON-NLS-1$

		//add method specific settings
		manifest.setRestrictions(Factory.methodDescriptor("B", "m1", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.setRestrictions(Factory.methodDescriptor("a.b.c.A","m2", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.setRestrictions(Factory.methodDescriptor("a.b.c.spi.SpiB","m3", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.setRestrictions(Factory.methodDescriptor("a.b.c.spi.SpiC", "m4", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$ //$NON-NLS-2$

		//add field specific settings
		manifest.setRestrictions(Factory.fieldDescriptor("D", "f1"), RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.setRestrictions(Factory.fieldDescriptor("a.b.c.D","f2"), RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.setRestrictions(Factory.fieldDescriptor("a.b.c.spi.SpiD","f3"), RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.setRestrictions(Factory.fieldDescriptor("a.b.c.spi.SpiC", "f4"), RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$

		return manifest;
	}

	/**
	 * used to build a manifest that can be changed
	 */
	protected IApiDescription buildModifiableManifest() {
		IApiDescription desc = newDescription();
		desc.setVisibility(Factory.packageDescriptor("a.b.c"), VisibilityModifiers.API); //$NON-NLS-1$
		desc.setVisibility(Factory.packageDescriptor(""), VisibilityModifiers.SPI); //$NON-NLS-1$
		IElementDescriptor element = Factory.typeDescriptor("C");  //$NON-NLS-1$
		desc.setRestrictions(element, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);

		element = Factory.typeDescriptor("a.b.c.D"); //$NON-NLS-1$
		desc.setRestrictions(element, RestrictionModifiers.NO_REFERENCE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);

		element = Factory.methodDescriptor("C", "m1", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)); //$NON-NLS-1$ //$NON-NLS-2$
		desc.setRestrictions(element, RestrictionModifiers.NO_OVERRIDE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);

		element = Factory.methodDescriptor("a.b.c.A","m2", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)); //$NON-NLS-1$ //$NON-NLS-2$
		desc.setRestrictions(element, RestrictionModifiers.NO_REFERENCE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);

		element = Factory.fieldDescriptor("D", "f1"); //$NON-NLS-1$ //$NON-NLS-2$
		desc.setRestrictions(element, RestrictionModifiers.NO_REFERENCE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);

		element = Factory.fieldDescriptor("a.b.c","f2"); //$NON-NLS-1$ //$NON-NLS-2$
		desc.setRestrictions(element, RestrictionModifiers.NO_REFERENCE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);
		return desc;
	}

	/**
	 * Tests visiting types in the manually created manifest
	 */
	@Test
	public void testVisitTypes() {
		IApiDescription manifest = buildManifest();
		doVisitTypes(manifest);
	}

	/**
	 * Tests restoring API settings from component XML. These settings are not
	 * quite as rich as we have in the usual baseline (no notion of SPI package,
	 * etc).
	 *
	 * We expect a component with the following information:
	 *
	 * default package: API class A class B - @noinstantiate method m1
	 * - @noextend class C - @noinstantiate @noextend class D - @noreference
	 * field f1 - @noreference interface IA interface IB - @noimplement package
	 * a.b.c: API class A - @noinstantiate @noextend method m2 - @noreference
	 * class B class C - @noextend class D - @noinstantiate field f2
	 * - @noreference interface IC - @noimplement interface ID package
	 * a.b.c.spi: API class SpiA class SpiB - @noextend method m3 class SpiC
	 * - @noinstantiate field f4 - @noreference method m4 - @noextend class SpiD
	 * - @noextend @noinstantiate class SpiE - @noreference field f3 interface
	 * ISpiA interface ISpiB - @noimplement package a.b.c.internal: PRIVATE
	 * class PA class PB class PC class PD
	 *
	 * package a.b.c.internal has API visibility for component "a.friend" class
	 * D has SPI visibility for component "a.friend"
	 *
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws CoreException
	 */
	@Test
	public void testRestoreFromXML() throws FileNotFoundException, IOException, CoreException {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-xml"); //$NON-NLS-1$
		File file = path.toFile();
		assertTrue("Missing xml directory", file.exists()); //$NON-NLS-1$
		IApiBaseline baseline = TestSuiteHelper.newApiBaseline("test", TestSuiteHelper.getEEDescriptionFile()); //$NON-NLS-1$
		IApiComponent component = ApiModelFactory.newApiComponent(baseline, file.getAbsolutePath());
		baseline.addApiComponents(new IApiComponent[] { component });

		IPackageDescriptor defPkgDesc = Factory.packageDescriptor(""); //$NON-NLS-1$
		ElementDescription defPkg = new ElementDescription(defPkgDesc, VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription B = new ElementDescription(defPkgDesc.getType("B"), VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
				ElementDescription m1 = new ElementDescription(defPkgDesc.getType("B").getMethod("m1", Signature.createMethodSignature(new String[0],Signature.SIG_VOID)), VisibilityModifiers.API, RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription C = new ElementDescription(defPkgDesc.getType("C"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
			ElementDescription D = new ElementDescription(defPkgDesc.getType("D"), VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
				ElementDescription f1 = new ElementDescription(defPkgDesc.getType("D").getField("f1"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription IB = new ElementDescription(defPkgDesc.getType("IB"), VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
		IPackageDescriptor abcPkgDesc = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		ElementDescription abcPkg = new ElementDescription(abcPkgDesc, VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription abcA = new ElementDescription(abcPkgDesc.getType("A"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
				ElementDescription abcAm2 = new ElementDescription(abcPkgDesc.getType("A").getMethod("m2", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription abcC = new ElementDescription(abcPkgDesc.getType("C"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
			ElementDescription abcD = new ElementDescription(abcPkgDesc.getType("D"), VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
				ElementDescription abcDf2 = new ElementDescription(abcPkgDesc.getType("D").getField("f2"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription abcIC = new ElementDescription(abcPkgDesc.getType("IC"), VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
		IPackageDescriptor spiPkgDesc = Factory.packageDescriptor("a.b.c.spi"); //$NON-NLS-1$
		ElementDescription spiPkg = new ElementDescription(spiPkgDesc, VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription spiB = new ElementDescription(spiPkgDesc.getType("SpiB"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
			ElementDescription spiC = new ElementDescription(spiPkgDesc.getType("SpiC"), VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
				ElementDescription spiCf4 = new ElementDescription(spiPkgDesc.getType("SpiC").getField("f4"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
				ElementDescription spiCm4 = new ElementDescription(spiPkgDesc.getType("SpiC").getMethod("m4", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.API, RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription spiD = new ElementDescription(spiPkgDesc.getType("SpiD"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
			ElementDescription spiE = new ElementDescription(spiPkgDesc.getType("SpiE"), VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
			ElementDescription IspiB = new ElementDescription(spiPkgDesc.getType("ISpiB"), VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
		IPackageDescriptor intPkgDesc = Factory.packageDescriptor("a.b.c.internal"); //$NON-NLS-1$
		ElementDescription intPkg = new ElementDescription(intPkgDesc, VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);

		final List<ElementDescription> visitOrder = new ArrayList<>();
		visitOrder.add(defPkg); // start def
			visitOrder.add(B); //start B
				visitOrder.add(m1); visitOrder.add(m1); //start / end m1
			visitOrder.add(B); // end B
			visitOrder.add(C); visitOrder.add(C); // start/end C
			visitOrder.add(D); // start D
				visitOrder.add(f1); visitOrder.add(f1); //start / end f1
			visitOrder.add(D); // end D
			visitOrder.add(IB); visitOrder.add(IB); // start/end IB
		visitOrder.add(defPkg); // end def
		visitOrder.add(abcPkg); // start a.b.c
			visitOrder.add(abcA); //a.b.c.start A
				visitOrder.add(abcAm2); visitOrder.add(abcAm2); //start / end m2
			visitOrder.add(abcA); // end a.b.c.A
			visitOrder.add(abcC); visitOrder.add(abcC); // start/end a.b.c.C
			visitOrder.add(abcD); //start a.b.c.D
				visitOrder.add(abcDf2); visitOrder.add(abcDf2); //start /end f2
			visitOrder.add(abcD); // end a.b.c.D
			visitOrder.add(abcIC); visitOrder.add(abcIC); // start/end a.b.c.IC
		visitOrder.add(abcPkg); // end a.b.c
		visitOrder.add(intPkg); // start a.b.c.internal
		visitOrder.add(intPkg); // end a.b.c.internal
		visitOrder.add(spiPkg); // start a.b.c.spi
			visitOrder.add(IspiB); visitOrder.add(IspiB); // start/end ISpiB
			visitOrder.add(spiB); //start spiB
			visitOrder.add(spiB); // end SpiB
			visitOrder.add(spiC); //start SpiC
				visitOrder.add(spiCf4); visitOrder.add(spiCf4); //start/ end f4
				visitOrder.add(spiCm4); visitOrder.add(spiCm4); //start / end m4
			visitOrder.add(spiC); // end SpiC
			visitOrder.add(spiD); visitOrder.add(spiD); // start / end SpiD
			visitOrder.add(spiE);
			visitOrder.add(spiE); // start/end SpiE
		visitOrder.add(spiPkg); // end a.b.c.spi

		ApiDescriptionVisitor visitor = new ApiDescriptionVisitor() {
			@Override
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong begin visit element", expected.fElement, element); //$NON-NLS-1$
				assertEquals("Wrong begin visit visibility", expected.fVis, description.getVisibility()); //$NON-NLS-1$
				assertEquals("Wrong begin visit restrictions for ", expected.fRes, description.getRestrictions()); //$NON-NLS-1$
				return true;
			}

			@Override
			public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong end visit element", expected.fElement, element); //$NON-NLS-1$
				assertEquals("Wrong end visit visibility", expected.fVis, description.getVisibility()); //$NON-NLS-1$
				assertEquals("Wrong end visit restrictions", expected.fRes, description.getRestrictions()); //$NON-NLS-1$
			}
		};

		component.getApiDescription().accept(visitor, null);

		assertEquals("Visit incomplete", 0, visitOrder.size()); //$NON-NLS-1$
		baseline.dispose();
	}

	/**
	 * Returns XML for the component's current API description.
	 *
	 * @param apiComponent API component
	 * @return XML for the API description
	 * @throws CoreException if something goes terribly wrong
	 */
	private String getApiDescriptionXML(IApiComponent apiComponent) throws CoreException {
		ApiDescriptionXmlCreator xmlVisitor = new ApiDescriptionXmlCreator(apiComponent);
		apiComponent.getApiDescription().accept(xmlVisitor, null);
		return xmlVisitor.getXML();
	}

	/**
	 * Reads XML from disk, annotates settings, then persists and re-creates
	 * settings to ensure we read/write equivalent XML.
	 *
	 * @throws CoreException
	 * @throws IOException
	 */
	@Test
	public void testPersistRestoreXML() throws CoreException, IOException {
		// read XML into API settings
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-xml"); //$NON-NLS-1$
		File file = path.toFile();
		assertTrue("Missing xml directory", file.exists()); //$NON-NLS-1$
		File descfile = new File(file, IApiCoreConstants.API_DESCRIPTION_XML_NAME);
		String readXML = null;
		if (descfile.exists()) {
			try (FileInputStream stream = new FileInputStream(descfile)) {
				char[] charArray = Util.getInputStreamAsCharArray(stream, -1, StandardCharsets.UTF_8);
				 readXML = new String(charArray);
			}
		}
		IApiDescription settings = new ApiDescription(null);
		ApiDescriptionProcessor.annotateApiSettings(null, settings, readXML);

		// write back to XML and then re-create
		IApiComponent component = TestSuiteHelper.createTestingApiComponent("test", "test", settings); //$NON-NLS-1$ //$NON-NLS-2$
		String writeXML = getApiDescriptionXML(component);

		IApiDescription restored = new ApiDescription(null);
		ApiDescriptionProcessor.annotateApiSettings(null, restored, writeXML);

		// compare the original and restore settings

		// build expected visit order from original
		final List<ElementDescription> visitOrder = new ArrayList<>();
		ApiDescriptionVisitor visitor = new ApiDescriptionVisitor() {
			@Override
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				visitOrder.add(new ElementDescription(null, element, description.getVisibility(), description.getRestrictions()));
				return super.visitElement(element, description);
			}
			@Override
			public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
				visitOrder.add(new ElementDescription(null, element, description.getVisibility(), description.getRestrictions()));
				super.endVisitElement(element, description);
			}
		};
		settings.accept(visitor, null);

		// now visit the restored version and compare order
		visitor = new ApiDescriptionVisitor() {
			@Override
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong begin visit element", expected.fElement, element); //$NON-NLS-1$
				assertEquals("Wrong begin visit component", expected.fComponent, null); //$NON-NLS-1$
				assertEquals("Wrong begin visit visibility", expected.fVis, description.getVisibility()); //$NON-NLS-1$
				assertEquals("Wrong begin visit restrictions", expected.fRes, description.getRestrictions()); //$NON-NLS-1$
				return true;
			}

			@Override
			public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong end visit element", expected.fElement, element); //$NON-NLS-1$
				assertEquals("Wrong end visit component", expected.fComponent, null); //$NON-NLS-1$
				assertEquals("Wrong end visit visibility", expected.fVis, description.getVisibility()); //$NON-NLS-1$
				assertEquals("Wrong end visit restrictions", expected.fRes, description.getRestrictions()); //$NON-NLS-1$
			}
		};

		restored.accept(visitor, null);

		assertEquals("Visit incomplete", 0, visitOrder.size());		 //$NON-NLS-1$
	}

	/**
	 * Test visiting types
	 */
	protected void doVisitTypes(IApiDescription manifest) {
		IPackageDescriptor defPkgDesc= Factory.packageDescriptor(""); //$NON-NLS-1$
		ElementDescription defPkg = new ElementDescription(defPkgDesc, VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription B = new ElementDescription(defPkgDesc.getType("B"), VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
			ElementDescription m1 = new ElementDescription(defPkgDesc.getType("B").getMethod("m1", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.API, RestrictionModifiers.NO_OVERRIDE);  //$NON-NLS-1$//$NON-NLS-2$
			ElementDescription C = new ElementDescription(defPkgDesc.getType("C"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
			ElementDescription D = new ElementDescription(defPkgDesc.getType("D"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
				ElementDescription f1 = new ElementDescription(defPkgDesc.getType("D").getField("f1"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription IB = new ElementDescription(defPkgDesc.getType("IB"), VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
		IPackageDescriptor abcPkgDesc = Factory.packageDescriptor("a.b.c"); //$NON-NLS-1$
		ElementDescription abcPkg = new ElementDescription(abcPkgDesc, VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription abcA = new ElementDescription(abcPkgDesc.getType("A"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
			ElementDescription abcAm2 = new ElementDescription(abcPkgDesc.getType("A").getMethod("m2", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription abcC = new ElementDescription(abcPkgDesc.getType("C"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
			ElementDescription abcD = new ElementDescription(abcPkgDesc.getType("D"), VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
				ElementDescription abcDf2 = new ElementDescription(abcPkgDesc.getType("D").getField("f2"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription abcIC = new ElementDescription(abcPkgDesc.getType("IC"), VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
		IPackageDescriptor spiPkgDesc = Factory.packageDescriptor("a.b.c.spi"); //$NON-NLS-1$
		ElementDescription spiPkg = new ElementDescription(spiPkgDesc, VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription spiB = new ElementDescription(spiPkgDesc.getType("SpiB"), VisibilityModifiers.SPI, RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
			ElementDescription spiBm3 = new ElementDescription(spiPkgDesc.getType("SpiB").getMethod("m3", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription spiC = new ElementDescription(spiPkgDesc.getType("SpiC"), VisibilityModifiers.SPI, RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
				ElementDescription spiCf4 = new ElementDescription(spiPkgDesc.getType("SpiC").getField("f4"), VisibilityModifiers.SPI, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$ //$NON-NLS-2$
				ElementDescription spiCm4 = new ElementDescription(spiPkgDesc.getType("SpiC").getMethod("m4", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.SPI, RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription spiD = new ElementDescription(spiPkgDesc.getType("SpiD"), VisibilityModifiers.SPI, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
				ElementDescription spiDf3 = new ElementDescription(spiPkgDesc.getType("SpiD").getField("f3"), VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$ //$NON-NLS-2$
			ElementDescription spiE = new ElementDescription(spiPkgDesc.getType("SpiE"), VisibilityModifiers.SPI, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
			ElementDescription IspiB = new ElementDescription(spiPkgDesc.getType("ISpiB"), VisibilityModifiers.SPI, RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
		IPackageDescriptor intPkgDesc = Factory.packageDescriptor("a.b.c.internal"); //$NON-NLS-1$
		ElementDescription intPkg = new ElementDescription(intPkgDesc, VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);

		final List<ElementDescription> visitOrder = new ArrayList<>();
		visitOrder.add(defPkg); // start def
			visitOrder.add(B); //start B
				visitOrder.add(m1); visitOrder.add(m1); //start / end m1
			visitOrder.add(B); // end B
			visitOrder.add(C); visitOrder.add(C); // start/end C
			visitOrder.add(D); // start D
				visitOrder.add(f1); visitOrder.add(f1); //start / end f1
			visitOrder.add(D); // end D
			visitOrder.add(IB); visitOrder.add(IB); // start/end IB
		visitOrder.add(defPkg); // end def
		visitOrder.add(abcPkg); // start a.b.c
			visitOrder.add(abcA); //a.b.c.start A
				visitOrder.add(abcAm2); visitOrder.add(abcAm2); //start /end m2
			visitOrder.add(abcA); // end a.b.c.A
			visitOrder.add(abcC); visitOrder.add(abcC); // start/end a.b.c.C
			visitOrder.add(abcD); //start a.b.c.D
				visitOrder.add(abcDf2); visitOrder.add(abcDf2); //start / end f2
			visitOrder.add(abcD); // end a.b.c.D
			visitOrder.add(abcIC); visitOrder.add(abcIC); // start/end a.b.c.IC
		visitOrder.add(abcPkg); // end a.b.c
		visitOrder.add(intPkg); // start a.b.c.internal
		visitOrder.add(intPkg); // end a.b.c.internal
		visitOrder.add(spiPkg); // start a.b.c.spi
			visitOrder.add(IspiB); visitOrder.add(IspiB); // start/end ISpiB
			visitOrder.add(spiB); //start spiB
				visitOrder.add(spiBm3); visitOrder.add(spiBm3); //start / end m3
			visitOrder.add(spiB); // end SpiB
			visitOrder.add(spiC); //start SpiC
				visitOrder.add(spiCf4); visitOrder.add(spiCf4); //start / end f4
				visitOrder.add(spiCm4); visitOrder.add(spiCm4); //start / end f4
			visitOrder.add(spiC); // end SpiC
			visitOrder.add(spiD); //start SpiD
				visitOrder.add(spiDf3); visitOrder.add(spiDf3); //start / end f3
			visitOrder.add(spiD); // end SpiD
			visitOrder.add(spiE); visitOrder.add(spiE); // start/end SpiE
		visitOrder.add(spiPkg); // end a.b.c.spi

		ApiDescriptionVisitor visitor = new ApiDescriptionVisitor() {
			@Override
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong begin visit element", expected.fElement, element); //$NON-NLS-1$
				assertEquals("Wrong begin visit visibility", expected.fVis, description.getVisibility()); //$NON-NLS-1$
				assertEquals("Wrong begin visit restrictions", expected.fRes, description.getRestrictions()); //$NON-NLS-1$
				return true;
			}

			@Override
			public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong end visit element", expected.fElement, element); //$NON-NLS-1$
				assertEquals("Wrong end visit visibility", expected.fVis, description.getVisibility()); //$NON-NLS-1$
				assertEquals("Wrong end visit restrictions", expected.fRes, description.getRestrictions()); //$NON-NLS-1$
			}
		};

		manifest.accept(visitor, null);

		assertEquals("Visit incomplete", 0, visitOrder.size()); //$NON-NLS-1$
	}

	/**
	 * Tests visiting packages
	 */
	@Test
	public void testVisitPackages() {
		ElementDescription defPkg = new ElementDescription(Factory.packageDescriptor(""), VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		ElementDescription abcPkg = new ElementDescription(Factory.packageDescriptor("a.b.c"), VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		ElementDescription spiPkg = new ElementDescription(Factory.packageDescriptor("a.b.c.spi"), VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		//ElementDescription spiPkgForNoFriend = new ElementDescription("no.friend", Factory.packageDescriptor("a.b.c.spi"), VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);
		ElementDescription intPkg = new ElementDescription(Factory.packageDescriptor("a.b.c.internal"), VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		//ElementDescription intPkgForFriend = new ElementDescription("a.friend", Factory.packageDescriptor("a.b.c.internal"), VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);

		final List<ElementDescription> visitOrder = new ArrayList<>();
		visitOrder.add(defPkg); // start def
		visitOrder.add(defPkg); // end def
		visitOrder.add(abcPkg); // start a.b.c
		visitOrder.add(abcPkg); // end a.b.c
		visitOrder.add(intPkg); // start a.b.c.internal
		visitOrder.add(intPkg); // end a.b.c.internal
		visitOrder.add(spiPkg); // start a.b.c.spi
		visitOrder.add(spiPkg); // end a.b.c.spi

		ApiDescriptionVisitor visitor = new ApiDescriptionVisitor() {
			@Override
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong begin visit element", expected.fElement, element); //$NON-NLS-1$
				assertEquals("Wrong begin visit visibility", expected.fVis, description.getVisibility()); //$NON-NLS-1$
				assertEquals("Wrong begin visit restrictions", expected.fRes, description.getRestrictions()); //$NON-NLS-1$
				return false;
			}

			@Override
			public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong end visit element", expected.fElement, element); //$NON-NLS-1$
				assertEquals("Wrong end visit visibility", expected.fVis, description.getVisibility()); //$NON-NLS-1$
				assertEquals("Wrong end visit restrictions", expected.fRes, description.getRestrictions()); //$NON-NLS-1$
			}
		};

		IApiDescription manifest = buildManifest();
		manifest.accept(visitor, null);

		assertEquals("Visit incomplete", 0, visitOrder.size()); //$NON-NLS-1$
	}

	/**
	 * Test for bug 209335, where an element is not in the component map for an
	 * {@link ApiDescription}, and not performing an insertion for missing
	 * elements throws an NPE
	 */
	@Test
	public void test209335() {
		String typename = "x.y.z.209335"; //$NON-NLS-1$
		String packageName = Signatures.getPackageName(typename);
		String tName = Signatures.getTypeName(typename);
		IReferenceTypeDescriptor type = Factory.packageDescriptor(packageName).getType(tName);
		IApiAnnotations description = fManifest.resolveAnnotations(type);
		assertNull("The description must be null", description); //$NON-NLS-1$
	}

	/**
	 * Resolves API description for a type with the given name.
	 *
	 * @param typeName fully qualified name of referenced type
	 * @param expectedVisibility expected visibility modifiers
	 * @param expectedRestrictions expected visibility restriction modifiers
	 */
	protected void resolveType(String typeName, int expectedVisibility, int expectedRestrictions) {
		String packageName = Signatures.getPackageName(typeName);
		String tName = Signatures.getTypeName(typeName);
		IReferenceTypeDescriptor type = Factory.packageDescriptor(packageName).getType(tName);
		IApiAnnotations description = fManifest.resolveAnnotations(type);
		assertEquals("Wrong visibility", expectedVisibility, description.getVisibility()); //$NON-NLS-1$
		assertEquals("Wrong restrictions", expectedRestrictions, description.getRestrictions()); //$NON-NLS-1$
	}

	/**
	 * Tests API description: A = API with no restrictions. Note that 'A' has
	 * not been added to the manifest
	 */
	@Test
	public void testADefPkg() {
		resolveType("A", VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests API description: B = API with no instantiate.
	 */
	@Test
	public void testBDefPkg() {
		resolveType("B", VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
	}

	/**
	 * Tests API description: C = API with no instantiate, no subclass.
	 */
	@Test
	public void testCDefPkg() {
		resolveType("C", VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
	}

	/**
	 * Tests API description: D = API with no reference.
	 */
	@Test
	public void testDDefPkg() {
		resolveType("D", VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests API description: IA = API with no restrictions. Note that this type
	 * is not explicitly in the manifest.
	 */
	@Test
	public void testIADefPkg() {
		resolveType("IA", VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests API description: IB = API with no implement.
	 */
	@Test
	public void testIBDefPkg() {
		resolveType("IB", VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.A = API with no instantiate, no subclass.
	 */
	@Test
	public void testAApiPkg() {
		resolveType("a.b.c.A", VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE | RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.B = API with no restrictions. Note that this
	 * type is not explicitly in the manifest.
	 */
	@Test
	public void testBApiPkg() {
		resolveType("a.b.c.B", VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.C = API with no subclass.
	 */
	@Test
	public void testCApiPkg() {
		resolveType("a.b.c.C", VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.D = API with no instantiate.
	 */
	@Test
	public void testDApiPkg() {
		resolveType("a.b.c.D", VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.IC = API with no implement.
	 */
	@Test
	public void testICApiPkg() {
		resolveType("a.b.c.IC", VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.ID = API with no restrictions. Note that
	 * this type is not explicitly in the manifest.
	 */
	@Test
	public void testIDApiPkg() {
		resolveType("a.b.c.ID", VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.spi.SpiA = SPI with no restrictions. Note
	 * that this type is not explicitly in the manifest.
	 */
	@Test
	public void testASpiPkg() {
		resolveType("a.b.c.spi.SpiA", VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.spi.SpiB = SPI with no subclass.
	 */
	@Test
	public void testBSpiPkg() {
		resolveType("a.b.c.spi.SpiB", VisibilityModifiers.SPI, RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.spi.SpiC = SPI with no instantiate.
	 */
	@Test
	public void testCSpiPkg() {
		resolveType("a.b.c.spi.SpiC", VisibilityModifiers.SPI, RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.spi.SpiD = SPI with no instantiate, no
	 * subclass.
	 */
	@Test
	public void testDSpiPkg() {
		resolveType("a.b.c.spi.SpiD", VisibilityModifiers.SPI, RestrictionModifiers.NO_INSTANTIATE | RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.spi.SpiD = SPI with no reference.
	 */
	@Test
	public void testESpiPkg() {
		resolveType("a.b.c.spi.SpiE", VisibilityModifiers.SPI, RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.spi.ISpiA = SPI with no restrictions. Note
	 * this type is not explicitly in the manifest.
	 */
	@Test
	public void testIASpiPkg() {
		resolveType("a.b.c.spi.ISpiA", VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.spi.ISpiB = SPI with no implement.
	 */
	@Test
	public void testIBSpiPkg() {
		resolveType("a.b.c.spi.ISpiB", VisibilityModifiers.SPI, RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.internal.A = Private with no restrictions.
	 * Note this type is not in the manifest explicitly.
	 */
	@Test
	public void testAInternalPkg() {
		resolveType("a.b.c.internal.PA", VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests API description: a.b.c.internal.B = Private with no restrictions.
	 * Note this type is not in the manifest explicitly.
	 */
	@Test
	public void testBInternalPkg() {
		resolveType("a.b.c.internal.PB", VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * tests that a binary bundle with no .api_description file has no API
	 * description
	 */
	@Test
	public void testBinaryHasNoApiDescription() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingBaseline("test-plugins"); //$NON-NLS-1$
		IApiComponent componentA = profile.getApiComponent("component.a"); //$NON-NLS-1$
		assertFalse("Should have no .api_description file", componentA.hasApiDescription()); //$NON-NLS-1$
	}

	/**
	 * tests that a binary bundle with an .api_description file has an API
	 * description
	 */
	@Test
	public void testBinaryHasApiDescription() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingBaseline("test-plugins-with-desc"); //$NON-NLS-1$
		IApiComponent componentA = profile.getApiComponent("component.a"); //$NON-NLS-1$
		assertTrue("Should have an .api_description file", componentA.hasApiDescription()); //$NON-NLS-1$
	}
}
