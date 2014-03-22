package parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import choco.Choco;
import choco.kernel.model.variables.integer.IntegerConstantVariable;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;

public class ConvertFormula {
	final Map<Character, Integer> isp = new HashMap<>();
	final Map<Character, Integer> icp = new HashMap<>();
	
	public ConvertFormula() {
		isp.put('#', 0);
		isp.put('(', 1);
		isp.put('*', 5);
		isp.put('/', 5);
		isp.put('+', 3);
		isp.put('-', 3);
		isp.put(')', 6);
		icp.put('#', 0);
		icp.put('(', 6);
		icp.put('*', 4);
		icp.put('/', 4);
		icp.put('+', 2);
		icp.put('-', 2);
		icp.put(')', 1);
	}
	
	public IntegerExpressionVariable convertFormula(String expression, ArrayList<IntegerVariable> varsTable) {
		expression += "#";
		Stack<IntegerExpressionVariable> vars = new Stack<>();
		Stack<Character> s = new Stack<>();
		int index = 0;
		s.push('#');
		while(!s.isEmpty() && index < expression.length()) {
			char ch = expression.charAt(index);
			if(ch >= '0' && ch <= '9') {
				String temp = "" + ch;
				index++; ch = expression.charAt(index);
				while(index < expression.length() && ch >= '0' && ch <= '9') {
					temp += ch;
					index++; 
					if(index == expression.length()) continue;
					ch = expression.charAt(index);
				}
				IntegerConstantVariable tempInt = new IntegerConstantVariable(Integer.parseInt(temp));//++++++++++++++
				vars.push(tempInt);
			}
			else if(ch == 'i') {
				index++; ch = expression.charAt(index);
				String temp = "";
				while(index < expression.length() && ch >= '0' && ch <= '9') {
					temp += ch;
					index++; 
					if(index == expression.length()) continue;
					ch = expression.charAt(index);
				}
				IntegerVariable iv = varsTable.get(Integer.parseInt(temp)); //+++++++++++++++++++++
				vars.push(iv);
			}
			else if(ch == 'S' || ch == 's') {
				int startIndex = index + 4;	//"{"ºóÒ»¸ö
				int endIndex = expression.indexOf(")", startIndex); //"}"
				String temp = expression.substring(startIndex, endIndex);
				String[] items = temp.split(",");
				ArrayList<IntegerExpressionVariable> sumItems = new ArrayList<>();
				for(String item : items) {
					if(item.startsWith("i")) {
						int k = Integer.parseInt(item.substring(1));
						sumItems.add(varsTable.get(k));
					}
					else {
						sumItems.add(new IntegerConstantVariable(Integer.parseInt(item)));
					}
				}
				if(sumItems.size() == 1)
					vars.push(sumItems.get(0));
				else {
					IntegerExpressionVariable iev = Choco.plus(sumItems.get(0), sumItems.get(1));
					for(int i = 2 ; i < sumItems.size() ; i++) {
						iev = Choco.plus(iev, sumItems.get(i));
					}
					vars.push(iev);
				}
				index = endIndex + 1;
			}
			else {
				char top = s.lastElement();
				if(isp.get(top) < icp.get(ch)) {
					s.push(ch);
					index++; ch = expression.charAt(index);
				}
				else if(isp.get(top) > icp.get(ch)) {
					char op = s.pop();
					IntegerExpressionVariable op2 = vars.pop();
					IntegerExpressionVariable op1 = vars.pop();
					IntegerExpressionVariable result = null;
					if(op == '+') {
						result = Choco.plus(op1, op2);
					}
					else if(op == '-') {
						result = Choco.minus(op1, op2);
					}
					else if(op == '*') {
						result = Choco.mult(op1, op2);
					}
					if(result != null)
						vars.push(result);
				}
				else {
					char op = s.pop();
					if(op == '(') {
						index++; 
						if(index == expression.length()) continue;
						ch = expression.charAt(index);
					}
				}
			}
		}
		return vars.pop();
	}
}
