package org.eclipse.pde.internal.wizards.templates;

import java.util.*;

public class TemplateControlStack {
	private Stack stack;
	private PreprocessorParser parser;
	
	class Entry {
		boolean value;
	}
	
	public TemplateControlStack() {
		stack = new Stack();
		parser = new PreprocessorParser();
	}
	
	public void setValueProvider(IVariableProvider provider) {
		parser.setVariableProvider(provider);
	}
	
	public void processLine(String line) {
		if (line.startsWith("if")) {
			String expression = line.substring(2).trim();
			boolean result = false;
			try {
				result = parser.parseAndEvaluate(expression);
			}
			catch (Exception e) {
			}
			Entry entry = new Entry();
			entry.value = result;
			stack.push(entry);
		}
		else if (line.startsWith("else")) {
			if (stack.isEmpty()==false) {
				Entry entry = (Entry)stack.peek();
				entry.value = !entry.value;
			}
		}
		else if (line.startsWith("endif")) {
			// pop the stack
			if (!stack.isEmpty())
				stack.pop();
		}
	}
	
	public boolean getCurrentState() {
		if (stack.isEmpty()) return true;
		// All control levels must evaluate to true to
		// return result==true
		for (Iterator iter = stack.iterator(); iter.hasNext();) {
			Entry entry = (Entry)iter.next();
			if (!entry.value) return false;
		}
		return true;
	}
}