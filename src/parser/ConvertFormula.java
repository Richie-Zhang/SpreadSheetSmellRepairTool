package parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;


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
	
	public ArithExpr convertFormula(String expression, ArrayList<ArithExpr> varsTable, Context ctx) throws Z3Exception {
		expression += "#";
		Stack<ArithExpr> vars = new Stack<>();
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
				ArithExpr tempInt = ctx.mkInt(Integer.parseInt(temp));
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
				ArithExpr iv = varsTable.get(Integer.parseInt(temp)); //+++++++++++++++++++++
				vars.push(iv);
			}
			else if(ch == 'S' || ch == 's') {
				int startIndex = index + 4;	//"{"ºóÒ»¸ö
				int endIndex = expression.indexOf(")", startIndex); //"}"
				String temp = expression.substring(startIndex, endIndex);
				String[] items = temp.split(",");
				ArrayList<ArithExpr> sumItems = new ArrayList<>();
				for(String item : items) {
					if(item.startsWith("i")) {
						int k = Integer.parseInt(item.substring(1));
						sumItems.add(varsTable.get(k));
					}
					else {
						sumItems.add(ctx.mkInt(Integer.parseInt(temp)));
					}
				}
				if(sumItems.size() == 1)
					vars.push(sumItems.get(0));
				else {
					ArithExpr iev = ctx.mkAdd(sumItems.get(0), sumItems.get(1));
					for(int i = 2 ; i < sumItems.size() ; i++) {
						iev = ctx.mkAdd(iev, sumItems.get(i));
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
					ArithExpr op2 = vars.pop();
					ArithExpr op1 = vars.pop();
					ArithExpr result = null;
					if(op == '+') {
						result = ctx.mkAdd(op1, op2);
					}
					else if(op == '-') {
						result = ctx.mkSub(op1, op2);
					}
					else if(op == '*') {
						result = ctx.mkMul(op1, op2);
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
