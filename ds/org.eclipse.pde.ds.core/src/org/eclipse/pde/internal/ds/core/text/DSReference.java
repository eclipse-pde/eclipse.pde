/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSReference;

public class DSReference extends DSObject implements IDSReference {

	private static final long serialVersionUID = 1L;

	public DSReference(DSModel model) {
		super(model, ELEMENT_REFERENCE);

		// set default values
		this
				.setReferenceCardinality(IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_ONE);
		this.setReferencePolicy(IDSConstants.VALUE_REFERENCE_POLICY_STATIC);

		// set generic values
		int ref_count = model.getDSComponent().getReferences().length + 1;
		this
				.setReferenceName(IDSConstants.ELEMENT_REFERENCE
						+ ref_count);
		this.setReferenceInterface(IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE
				+ ref_count);
	}
	@Override
	public boolean canAddChild(int objectType) {
		return false;
	}

	@Override
	public boolean canBeParent() {
		return false;
	}

	@Override
	public String getName() {
		return getReferenceName();
	}

	@Override
	public int getType() {
		return TYPE_REFERENCE;
	}

	@Override
	public void setReferenceName(String name){
		setXMLAttribute(ATTRIBUTE_REFERENCE_NAME, name);
	}

	@Override
	public String getReferenceName(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_NAME);
	}

	@Override
	public void setReferenceInterface(String interfaceName){
		setXMLAttribute(ATTRIBUTE_REFERENCE_INTERFACE, interfaceName);
	}

	@Override
	public String getReferenceInterface(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_INTERFACE);
	}

	@Override
	public void setReferenceCardinality(String cardinality){
		setXMLAttribute(ATTRIBUTE_REFERENCE_CARDINALITY, cardinality);
	}

	@Override
	public String getReferenceCardinality(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_CARDINALITY);
	}


	@Override
	public void setReferencePolicy(String policy){
		setXMLAttribute(ATTRIBUTE_REFERENCE_POLICY, policy);
	}

	@Override
	public String getReferencePolicy(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_POLICY);
	}


	@Override
	public void setReferenceTarget(String target){
		setXMLAttribute(ATTRIBUTE_REFERENCE_TARGET, target);
	}

	@Override
	public String getReferenceTarget(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_TARGET);
	}


	@Override
	public void setReferenceBind(String bind){
		setXMLAttribute(ATTRIBUTE_REFERENCE_BIND, bind);
	}

	@Override
	public String getReferenceBind(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_BIND);
	}


	@Override
	public void setReferenceUnbind(String unbind){
		setXMLAttribute(ATTRIBUTE_REFERENCE_UNBIND, unbind);
	}

	@Override
	public String getReferenceUnbind(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_UNBIND);
	}
	@Override
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_REFERENCE_BIND,
				IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY,
				IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE,
				IDSConstants.ATTRIBUTE_REFERENCE_NAME,
				IDSConstants.ATTRIBUTE_REFERENCE_POLICY,
				IDSConstants.ATTRIBUTE_REFERENCE_TARGET,
				IDSConstants.ATTRIBUTE_REFERENCE_UNBIND };
	}

	@Override
	public boolean isLeafNode() {
		return true;
	}

}
