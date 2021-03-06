package hari.jsfactor.scanner;

import hari.jsfactor.jsobjects.IJSObject;
import hari.jsfactor.jsobjects.JSComment;
import hari.jsfactor.jsobjects.JSFile;
import hari.jsfactor.jsobjects.JSFunction;
import hari.jsfactor.jsobjects.JSVariable;
import hari.jsfactor.ui.contants.IJSFactorTokens;

import java.util.ArrayList;
import java.util.Stack;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;

public class JSDocumentScanner {

	private JSRules rules;
	private JSFile jsFile;
	private Stack<IJSObject> programStack;
	private ArrayList<JSFunction> functionList;
	private ArrayList<JSVariable> variableList;

	public JSDocumentScanner() {
		rules = new JSRules();
	}

	public void updateModel(IDocument document) {

		jsFile = new JSFile();
		functionList = new ArrayList<JSFunction>();
		variableList = new ArrayList<JSVariable>();
		programStack = new Stack<IJSObject>();
		programStack.push(jsFile);

		rules.setRange(document, 0, document.getLength());

		IToken nextToken = rules.nextToken();
		IJSObject previousAssignableJSObject = jsFile;

		try {
			String sequenceToFind = "";
			while (!nextToken.isEOF()) {
				String content = getContent(document);

				if (nextToken.equals(IJSFactorTokens.COMMENT_TOKEN)) {
					sequenceToFind = "";
					JSComment comment = new JSComment(content);
					programStack.lastElement().addContainingObjects(comment);
				} else if (nextToken.equals(IJSFactorTokens.JS_FUNCTION_TOKEN)) {
					sequenceToFind = "";
					JSFunction function = JSFunction.getJSFunction(content,
							rules.getTokenOffset(), rules.getTokenLength());
					programStack.lastElement().addContainingObjects(function);
					functionList.add(function);
					previousAssignableJSObject = function;
				} else if (nextToken
						.equals(IJSFactorTokens.OPEN_CURLY_BRACES_TOKEN)) {
					sequenceToFind = "";
					programStack.push(previousAssignableJSObject);
				} else if (nextToken
						.equals(IJSFactorTokens.JS_SINGLE_LINE_VARIABLE_TOKEN)) {
					sequenceToFind = "";
					IJSObject jsVariable = JSVariable.getJSVariable(content,
							rules.getTokenOffset(), rules.getTokenLength());
					previousAssignableJSObject.addContainingObjects(jsVariable);
					if (jsVariable instanceof JSFunction) {
						functionList.add((JSFunction) jsVariable);
						previousAssignableJSObject = jsVariable;
					} else {
						variableList.add((JSVariable) jsVariable);
					}
				} else {
					sequenceToFind = sequenceToFind + content;
					if (content.equals("\n") || content.equals(";")
							|| content.equals(" ") || content.equals("\t")
							|| content.trim().length() == 0)
					{
						sequenceToFind = "";
						nextToken = rules.nextToken();
						continue;
					}
					
					
					if (content.equals("(")) {
						boolean foundFunctionUsage = findAndAddForFunctionUsage(
								sequenceToFind, rules.getTokenOffset());

						if(foundFunctionUsage)  sequenceToFind = "";
					} else {
						boolean foundVariableUsage = findAndAddVariableUsage(sequenceToFind,
								rules.getTokenOffset());
						if(foundVariableUsage) sequenceToFind ="";
					}
				}

				nextToken = rules.nextToken();
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

	}

	private boolean findAndAddVariableUsage(String sequenceToFind, int offset) {
		boolean found = false;
		String variableName = sequenceToFind.substring(0,
				sequenceToFind.length());
		for (JSVariable variable : variableList) {
			if (variable.getVariableName().equals(variableName.trim())) {
				int usageOffset = offset - variableName.trim().length();
				variable.addUsage(usageOffset);
				found = true; 
			}
		}
		return found;
	}

	private boolean findAndAddForFunctionUsage(String sequenceToFind, int offset) {
		boolean found = false;
		String functionName = sequenceToFind.substring(0,
				sequenceToFind.length() - 1);
		for (JSFunction function : functionList) {
			if (function.getFunctionName().equals(functionName.trim())) {
				int usageOffset = offset - functionName.trim().length();
				function.addUsage(usageOffset);
				found = true;
			}
		}
		return found;
	}

	private String getContent(IDocument document) throws BadLocationException {
		return document.get(rules.getTokenOffset(), rules.getTokenLength());
	}

	public JSFile getJSFile() {
		return jsFile;
	}

	public JSFunction[] getJSFunctions() {
		JSFunction[] functions = new JSFunction[functionList.size()];
		return functionList.toArray(functions);
	}

	public JSVariable[] getJSVariables() {
		JSVariable[] variables = new JSVariable[variableList.size()];
		return variableList.toArray(variables);
	}
}
