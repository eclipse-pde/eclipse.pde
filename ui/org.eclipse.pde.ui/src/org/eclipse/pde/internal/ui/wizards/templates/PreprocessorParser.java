/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import java.util.Hashtable;
import java.util.Stack;
import org.eclipse.pde.ui.templates.IVariableProvider;

public class PreprocessorParser {
	private static final int T_VAR = 1;
	private static final int T_LBR = 2;
	private static final int T_RBR = 3;
	private static final int T_NOT = 4;
	private static final int T_AND = 5;
	private static final int T_OR = 6;
	private static final int T_EQ = 7;
	private static final int T_NEQ = 8;
	private static final int T_STRING = 9;
	private static final int T_TRUE = 22;
	private static final int T_FALSE = 23;
	private static final int T_ERROR = 99;
	private static final int T_EOF = 10;

	//private static final int OP_LEAF = -1;
	private static final int OP_AND = 1;
	private static final int OP_OR = 2;
	private static final int OP_EQ = 3;
	private static final int OP_NEQ = 4;
	private static final int OP_NOT = 5;
	//private static final int OP_DEFER = 55;

	private IVariableProvider provider;
	private String line;
	private Stack exprStack;
	private int loc;
	private String tvalue;

	abstract class Node {
		abstract Object getValue();
	}

	class LeafNode extends Node {
		Object value;

		LeafNode(Object value) {
			this.value = value;
		}

		public Object getValue() {
			return value;
		}

		public String toString() {
			if (value != null)
				return "leaf[" + value.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			return "leaf[null]"; //$NON-NLS-1$
		}
	}

	class ExpressionNode extends Node {
		int opcode;
		Node left;
		Node right;

		public ExpressionNode(Node left, Node right, int opcode) {
			this.opcode = opcode;
			this.left = left;
			this.right = right;
		}

		public Object getValue() {
			boolean result = false;
			Object leftValue = left != null ? left.getValue() : Boolean.FALSE;
			Object rightValue = right != null ? right.getValue() : Boolean.FALSE;

			if (opcode == OP_NOT && rightValue instanceof Boolean) {
				result = rightValue.equals(Boolean.TRUE) ? false : true;
			} else {

				if (leftValue instanceof Boolean && rightValue instanceof Boolean) {
					boolean bleft = ((Boolean) leftValue).booleanValue();
					boolean bright = ((Boolean) rightValue).booleanValue();

					switch (opcode) {
						case OP_AND :
							result = bleft && bright;
							break;
						case OP_OR :
							result = bleft || bright;
							break;
						case OP_EQ :
							result = bleft == bright;
							break;
						case OP_NEQ :
							result = bleft != bright;
							break;
					}
				}
				if (leftValue instanceof String && rightValue instanceof String) {
					switch (opcode) {
						case OP_EQ :
							result = leftValue.equals(rightValue);
							break;
						case OP_NEQ :
							result = leftValue.equals(rightValue);
							break;
					}
				}
			}
			return result ? Boolean.TRUE : Boolean.FALSE;
		}

		public String toString() {
			String lstring = left != null ? left.toString() : "*"; //$NON-NLS-1$
			String rstring = right != null ? right.toString() : "*"; //$NON-NLS-1$
			return "(" + lstring + "<" + opcode + ">" + rstring + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	class RootEntry {
		Node root;
	}

	public PreprocessorParser() {
		this(null);
	}

	public PreprocessorParser(IVariableProvider provider) {
		this.provider = provider;
		exprStack = new Stack();
	}

	public void setVariableProvider(IVariableProvider provider) {
		this.provider = provider;
	}

	public static void main(String[] args) {
		final Hashtable vars = new Hashtable();
		vars.put("a", Boolean.FALSE); //$NON-NLS-1$
		vars.put("b", "3"); //$NON-NLS-1$ //$NON-NLS-2$
		vars.put("c", Boolean.TRUE); //$NON-NLS-1$
		PreprocessorParser parser = new PreprocessorParser(new IVariableProvider() {
			public Object getValue(String variable) {
				return vars.get(variable);
			}
		});
		try {
			boolean value = parser.parseAndEvaluate("!a || (b==\"2\" && c)"); //$NON-NLS-1$
			System.out.println("Result: " + value); //$NON-NLS-1$
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public boolean parseAndEvaluate(String line) throws Exception {
		reset();
		this.line = line;
		//System.out.println("Line: " + line);
		parse();
		//printExpression();
		return evaluate();
	}

	private boolean evaluate() {
		boolean result = false;
		if (exprStack.isEmpty() == false) {
			RootEntry entry = (RootEntry) exprStack.peek();
			if (entry.root != null) {
				Object value = entry.root.getValue();
				if (value != null && value instanceof Boolean) {
					if (((Boolean) value).equals(Boolean.TRUE))
						result = true;
				}
			}
		}
		return result;
	}

	private void reset() {
		loc = 0;
		tvalue = null;
		exprStack.clear();
	}

	private void parse() throws Exception {
		for (;;) {
			int token = getNextToken();
			//System.out.println("Token: " + token + ", val=\"" + tvalue+"\"");
			if (token == T_EOF)
				break;

			if (token == T_VAR) {
				Node node = new LeafNode(provider.getValue(tvalue.toString()));
				pushNode(node);
				continue;
			}
			if (token == T_TRUE || token == T_FALSE) {
				Object value = token == T_TRUE ? Boolean.TRUE : Boolean.FALSE;
				Node node = new LeafNode(value);
				pushNode(node);
				continue;
			}
			if (token == T_STRING) {
				Node node = new LeafNode(tvalue);
				pushNode(node);
				continue;
			}

			if (token == T_NOT) {
				pushNode(OP_NOT);
				continue;
			}

			int opcode = 0;

			switch (token) {
				case T_AND :
					opcode = OP_AND;
					break;
				case T_OR :
					opcode = OP_OR;
					break;
				case T_EQ :
					opcode = OP_EQ;
					break;
				case T_NEQ :
					opcode = OP_NEQ;
					break;
			}
			if (opcode != 0) {
				pushNode(opcode);
				continue;
			}
			if (token == T_LBR) {
				pushRoot();
				continue;
			}
			if (token == T_RBR) {
				if (exprStack.isEmpty())
					throwUnexpectedToken("not )", token); //$NON-NLS-1$
				popRoot();
				continue;
			}
		}
	}

	private RootEntry getCurrentRoot() {
		if (exprStack.isEmpty()) {
			RootEntry entry = new RootEntry();
			exprStack.push(entry);
		}
		return (RootEntry) exprStack.peek();
	}

	private void replaceRoot(ExpressionNode newRoot) {
		RootEntry entry = getCurrentRoot();
		if (entry.root != null)
			newRoot.left = entry.root;
		entry.root = newRoot;
	}

	private void pushNode(Node node) {
		RootEntry entry = getCurrentRoot();
		if (entry.root == null)
			entry.root = node;
		else {
			ExpressionNode enode = (ExpressionNode) entry.root;
			if (enode.opcode == OP_NOT)
				enode.right = node;
			else {
				if (enode.left == null)
					enode.left = node;
				else
					enode.right = node;
			}
		}
	}

	private void pushNode(int opcode) {
		ExpressionNode node = new ExpressionNode(null, null, opcode);
		replaceRoot(node);
	}

	private void pushRoot() {
		exprStack.push(new RootEntry());
	}

	private void popRoot() {
		RootEntry entry = getCurrentRoot();
		exprStack.pop();
		pushNode(entry.root);
	}

	private void throwUnexpectedToken(String expected, int token) throws Exception {
		String message = "Expected " + expected + ", found " + token; //$NON-NLS-1$ //$NON-NLS-2$
		throw new Exception(message);
	}

	private int getNextToken() {
		boolean string = false;
		boolean variable = false;
		int vloc = loc;
		tvalue = null;
		for (;;) {
			if (loc == line.length()) {
				// check if we have panding identifier
				if (variable) {
					tvalue = line.substring(vloc, loc);
					variable = false;
					if (tvalue.equalsIgnoreCase("false")) //$NON-NLS-1$
						return T_FALSE;
					if (tvalue.equalsIgnoreCase("true")) //$NON-NLS-1$
						return T_TRUE;
					return T_VAR;
				}
				if (string) {
					// EOF in string
					string = false;
					return T_ERROR;
				}
				// regular end of line
				tvalue = "EOF"; //$NON-NLS-1$
				return T_EOF;
			}
			char c = line.charAt(loc++);

			if (c == '\"') {
				if (string) {
					tvalue = line.substring(vloc, loc - 1);
					string = false;
					return T_STRING;
				}
				vloc = loc;
				string = true;
				continue;
			} else if (string)
				continue;

			if (!variable && Character.isJavaIdentifierStart(c)) {
				variable = true;
				vloc = loc - 1;
				continue;
			}
			if (variable) {
				if (!Character.isJavaIdentifierPart(c)) {
					loc--;
					tvalue = line.substring(vloc, loc);
					variable = false;
					if (tvalue.equalsIgnoreCase("false")) //$NON-NLS-1$
						return T_FALSE;
					if (tvalue.equalsIgnoreCase("true")) //$NON-NLS-1$
						return T_TRUE;
					return T_VAR;
				}
				continue;
			}

			if (testDoubleToken(c, "!=")) //$NON-NLS-1$
				return T_NEQ;
			if (testDoubleToken(c, "==")) //$NON-NLS-1$
				return T_EQ;
			if (testDoubleToken(c, "&&")) //$NON-NLS-1$
				return T_AND;
			if (testDoubleToken(c, "||")) //$NON-NLS-1$
				return T_OR;
			if (testSingleToken(c, '!'))
				return T_NOT;
			if (testSingleToken(c, '('))
				return T_LBR;
			if (testSingleToken(c, ')'))
				return T_RBR;
			if (c == ' ' || c == '\t' || c == '\n')
				continue;
			tvalue = "" + c; //$NON-NLS-1$
			return T_ERROR;
		}
	}

	private boolean testSingleToken(char c, char expected) {
		if (c == expected) {
			tvalue = "" + expected; //$NON-NLS-1$
			return true;
		}
		return false;
	}

	private boolean testDoubleToken(char c1, String pattern) {
		if (c1 != pattern.charAt(0))
			return false;
		char c2 = line.charAt(loc);
		if (c2 == pattern.charAt(1)) {
			loc++;
			tvalue = pattern;
			return true;
		}
		return false;
	}
}
