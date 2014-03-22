package test;

import java.util.ArrayList;

public class testConstant {
	
	public static void main(String[] args) {
		testConstant tConstant = new testConstant();
		ArrayList<Integer> result = tConstant.getConstantNum("i1+2+3*i2+245");
		for(Integer integer : result)
			System.out.println(integer);
	}
	
	public ArrayList<Integer> getConstantNum(String func) {
		ArrayList<Integer> constants = new ArrayList<>();
		String temp = func;
		for(int i = 0 ; i < 5 ; i++) {
			temp = temp.replaceAll("i"+i, "");
			System.out.println("i"+i);
		}
		int index = 0, length = temp.length();
		while(index < length) {
			int value = 0;
			while(index < length && !(temp.charAt(index) > '0' && temp.charAt(index) < '9'))
				index++;
			while(index < length && (temp.charAt(index) > '0' && temp.charAt(index) < '9')) {
				value = 10*value + (int)(temp.charAt(index)-'0');
				index++;
			}
			constants.add(value);
		}
		return constants;
	}
}
