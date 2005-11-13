/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public abstract class AbstractJavaScanner extends BufferedRuleBasedScanner {


	private IColorManager fColorManager;
	private IPreferenceStore fPreferenceStore;

	private Map fTokenMap= new HashMap();
	private String[] fPropertyNamesColor;
	private String[] fPropertyNamesBold;
	private String[] fPropertyNamesItalic;
	private String[] fPropertyNamesStrikethrough;
	private String[] fPropertyNamesUnderline;
	private boolean fNeedsLazyColorLoading;


	abstract protected String[] getTokenProperties();

	abstract protected List createRules();

	public AbstractJavaScanner(IColorManager manager, IPreferenceStore store) {
		super();
		fColorManager= manager;
		fPreferenceStore= store;
	}

	public final void initialize() {

		fPropertyNamesColor= getTokenProperties();
		int length= fPropertyNamesColor.length;
		fPropertyNamesBold= new String[length];
		fPropertyNamesItalic= new String[length];
		fPropertyNamesStrikethrough= new String[length];
		fPropertyNamesUnderline= new String[length];

		for (int i= 0; i < length; i++) {
			fPropertyNamesBold[i]= getBoldKey(fPropertyNamesColor[i]);
			fPropertyNamesItalic[i]= getItalicKey(fPropertyNamesColor[i]);
			fPropertyNamesStrikethrough[i]= getStrikethroughKey(fPropertyNamesColor[i]);
			fPropertyNamesUnderline[i]= getUnderlineKey(fPropertyNamesColor[i]);
		}
		
		fNeedsLazyColorLoading= Display.getCurrent() == null;
		for (int i= 0; i < length; i++) {
			if (fNeedsLazyColorLoading)
				addTokenWithProxyAttribute(fPropertyNamesColor[i], fPropertyNamesBold[i], fPropertyNamesItalic[i], fPropertyNamesStrikethrough[i], fPropertyNamesUnderline[i]);
			else
				addToken(fPropertyNamesColor[i], fPropertyNamesBold[i], fPropertyNamesItalic[i], fPropertyNamesStrikethrough[i], fPropertyNamesUnderline[i]);
		}

		initializeRules();
	}
	
	protected String getBoldKey(String colorKey) {
		return colorKey + PreferenceConstants.EDITOR_BOLD_SUFFIX;
	}

	protected String getItalicKey(String colorKey) {
		return colorKey + PreferenceConstants.EDITOR_ITALIC_SUFFIX;
	}
	
	protected String getStrikethroughKey(String colorKey) {
		return colorKey + PreferenceConstants.EDITOR_STRIKETHROUGH_SUFFIX;
	}
	
	protected String getUnderlineKey(String colorKey) {
		return colorKey + PreferenceConstants.EDITOR_UNDERLINE_SUFFIX;
	}
	
	public IToken nextToken() {
		if (fNeedsLazyColorLoading)
			resolveProxyAttributes();
		return super.nextToken();
	}

	private void resolveProxyAttributes() {
		if (fNeedsLazyColorLoading && Display.getCurrent() != null) {
			for (int i= 0; i < fPropertyNamesColor.length; i++) {
				addToken(fPropertyNamesColor[i], fPropertyNamesBold[i], fPropertyNamesItalic[i], fPropertyNamesStrikethrough[i], fPropertyNamesUnderline[i]);
			}
			fNeedsLazyColorLoading= false;
		}
	}

	private void addTokenWithProxyAttribute(String colorKey, String boldKey, String italicKey, String strikethroughKey, String underlineKey) {
		fTokenMap.put(colorKey, new Token(createTextAttribute(null, boldKey, italicKey, strikethroughKey, underlineKey)));
	}

	private void addToken(String colorKey, String boldKey, String italicKey, String strikethroughKey, String underlineKey) {
		if (!fNeedsLazyColorLoading)
			fTokenMap.put(colorKey, new Token(createTextAttribute(colorKey, boldKey, italicKey, strikethroughKey, underlineKey)));
		else {
			Token token= ((Token)fTokenMap.get(colorKey));
			if (token != null)
				token.setData(createTextAttribute(colorKey, boldKey, italicKey, strikethroughKey, underlineKey));
		}
	}

	private TextAttribute createTextAttribute(String colorKey, String boldKey, String italicKey, String strikethroughKey, String underlineKey) {
		Color color= null;
		if (colorKey != null)
			color= fColorManager.getColor(colorKey);

		int style= fPreferenceStore.getBoolean(boldKey) ? SWT.BOLD : SWT.NORMAL;
		if (fPreferenceStore.getBoolean(italicKey))
			style |= SWT.ITALIC;

		if (fPreferenceStore.getBoolean(strikethroughKey))
			style |= TextAttribute.STRIKETHROUGH;

		if (fPreferenceStore.getBoolean(underlineKey))
			style |= TextAttribute.UNDERLINE;

		return new TextAttribute(color, null, style);
	}

	protected Token getToken(String key) {
		if (fNeedsLazyColorLoading)
			resolveProxyAttributes();
		return (Token) fTokenMap.get(key);
	}

	private void initializeRules() {
		List rules= createRules();
		if (rules != null) {
			IRule[] result= new IRule[rules.size()];
			rules.toArray(result);
			setRules(result);
		}
	}
	
	private int indexOf(String property) {
		if (property != null) {
			int length= fPropertyNamesColor.length;
			for (int i= 0; i < length; i++) {
				if (property.equals(fPropertyNamesColor[i]) || property.equals(fPropertyNamesBold[i]) || property.equals(fPropertyNamesItalic[i]) || property.equals(fPropertyNamesStrikethrough[i]) || property.equals(fPropertyNamesUnderline[i]))
					return i;
			}
		}
		return -1;
	}
	
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		String p= event.getProperty();
		int index= indexOf(p);
		Token token= getToken(fPropertyNamesColor[index]);
		if (fPropertyNamesColor[index].equals(p))
			adaptToColorChange(token, event);
		else if (fPropertyNamesBold[index].equals(p))
			adaptToStyleChange(token, event, SWT.BOLD);
		else if (fPropertyNamesItalic[index].equals(p))
			adaptToStyleChange(token, event, SWT.ITALIC);
		else if (fPropertyNamesStrikethrough[index].equals(p))
			adaptToStyleChange(token, event, TextAttribute.STRIKETHROUGH);
		else if (fPropertyNamesUnderline[index].equals(p))
			adaptToStyleChange(token, event, TextAttribute.UNDERLINE);
	}

	private void adaptToColorChange(Token token, PropertyChangeEvent event) {
		RGB rgb= null;

		Object value= event.getNewValue();
		if (value instanceof RGB)
			rgb= (RGB) value;
		else if (value instanceof String)
			rgb= StringConverter.asRGB((String) value);

		if (rgb != null) {

			String property= event.getProperty();
			Color color= fColorManager.getColor(property);

			Object data= token.getData();
			if (data instanceof TextAttribute) {
				TextAttribute oldAttr= (TextAttribute) data;
				token.setData(new TextAttribute(color, oldAttr.getBackground(), oldAttr.getStyle()));
			}
		}
	}

	private void adaptToStyleChange(Token token, PropertyChangeEvent event, int styleAttribute) {
		boolean eventValue= false;
		Object value= event.getNewValue();
		if (value instanceof Boolean)
			eventValue= ((Boolean) value).booleanValue();
		else if (IPreferenceStore.TRUE.equals(value))
			eventValue= true;

		Object data= token.getData();
		if (data instanceof TextAttribute) {
			TextAttribute oldAttr= (TextAttribute) data;
			boolean activeValue= (oldAttr.getStyle() & styleAttribute) == styleAttribute;
			if (activeValue != eventValue)
				token.setData(new TextAttribute(oldAttr.getForeground(), oldAttr.getBackground(), eventValue ? oldAttr.getStyle() | styleAttribute : oldAttr.getStyle() & ~styleAttribute));
		}
	}
	
	public boolean affectsBehavior(PropertyChangeEvent event) {
		return indexOf(event.getProperty()) >= 0;
	}
}
