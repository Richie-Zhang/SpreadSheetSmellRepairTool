package parser;

import java.util.ArrayList;
import java.util.Stack;

public class Calculate {

	public ArrayList<String> getStringList(String str){
		ArrayList<String> result = new ArrayList<String>();
		String num = "";
		for (int i = 0; i < str.length(); i++) {
			if(Character.isDigit(str.charAt(i))){
				num = num + str.charAt(i);
			}else{
				if(num != ""){
					result.add(num);
				}
				result.add(str.charAt(i) + "");
				num = "";
			}
		}
		if(num != ""){
			result.add(num);
		}
		return result;
	}

	public ArrayList<String> getPostOrder(ArrayList<String> inOrderList){
		
		ArrayList<String> result = new ArrayList<String>();
		Stack<String> stack = new Stack<String>();
		for (int i = 0; i < inOrderList.size(); i++) {
			if(Character.isDigit(inOrderList.get(i).charAt(0))){
				result.add(inOrderList.get(i));
			}else{
				switch (inOrderList.get(i).charAt(0)) {
				case '(':
					stack.push(inOrderList.get(i));
					break;
				case ')':
					while (!stack.peek().equals("(")) {
						result.add(stack.pop());
					}
					stack.pop();
					break;
				default:
					while (!stack.isEmpty() && compare(stack.peek(), inOrderList.get(i))){
						result.add(stack.pop());
					}
					stack.push(inOrderList.get(i));
					break;
				}
			}
		}
		while(!stack.isEmpty()){
			result.add(stack.pop());
		}
		return result;
	}

	public Integer calculate(ArrayList<String> postOrder){
		Stack<Integer> stack = new Stack<Integer>();
		for (int i = 0; i < postOrder.size(); i++) {
			if(Character.isDigit(postOrder.get(i).charAt(0))){
				stack.push(Integer.parseInt(postOrder.get(i)));
			}else{
				Integer back = (Integer)stack.pop();
				Integer front = (Integer)stack.pop();
				Integer res = 0;
				switch (postOrder.get(i).charAt(0)) {
				case '+':
					res = front + back;
					break;
				case '-':
					res = front - back;
					break;
				case '*':
					res = front * back;
					break;
				case '/':
					res = front / back;
					break;
				}
				stack.push(res);
			}
		}
		return (Integer)stack.pop();
	}

	public static boolean compare(String peek, String cur){
		if("*".equals(peek) && ("/".equals(cur) || "*".equals(cur) ||"+".equals(cur) ||"-".equals(cur))){
			return true;
		}else if("/".equals(peek) && ("/".equals(cur) || "*".equals(cur) ||"+".equals(cur) ||"-".equals(cur))){
			return true;
		}else if("+".equals(peek) && ("+".equals(cur) || "-".equals(cur))){
			return true;
		}else if("-".equals(peek) && ("+".equals(cur) || "-".equals(cur))){
			return true;
		}
		return false;
	}
	
	public static void main(String[] args) {
		Calculate calculate = new Calculate();
		String s = "3+6*(2+1)";
		ArrayList<String> result = calculate.getStringList(s);  //String转换为List
		result = calculate.getPostOrder(result);   //中缀变后缀
		int i = calculate.calculate(result);   //计算
		System.out.println(i);

	}

}
