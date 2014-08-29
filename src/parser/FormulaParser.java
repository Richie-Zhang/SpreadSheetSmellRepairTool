package parser;

import java.util.ArrayList;
import java.util.Collections;

import synthesis.util.Z3Util;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

import core.Formula;
import core.StructDefine;

public class FormulaParser {
	public static Context ctx = Z3Util.getContext();

	public static boolean isEquivalent(Formula formula1, Formula formula2, ArrayList<StructDefine.R1C1Relative> IV) throws Z3Exception {
		String for1 = preProcessFormula(formula1.getR1C1Formula(), IV);
		String for2 = preProcessFormula(formula2.getR1C1Formula(), IV);
		
		if(!validFormula(for1) || !validFormula(for2))
			return false;
		
		return isEquivalent(for1, for2, IV);
	}
	
	public static boolean isEquivalent(String for1, String for2, ArrayList<StructDefine.R1C1Relative> IV) throws Z3Exception {
		ArrayList<ArithExpr> varsTable = new ArrayList<>();
		for(int i = 0 ; i < IV.size() ; i++) {
			varsTable.add(ctx.mkIntConst("i"+i));
		}
		
		ConvertFormula convertFormula = new ConvertFormula();
		ArithExpr iev1 = convertFormula.convertFormula(for1, varsTable, ctx);
		ArithExpr iev2 = convertFormula.convertFormula(for2, varsTable, ctx);
		
		Solver solver = ctx.mkSolver();
		solver.add(ctx.mkNot(ctx.mkEq(iev1, iev2)));
		
		if (Status.SATISFIABLE == solver.check()) {
			return false;
		} else {
			return true;
		}
	}
	
	public static String preProcessFormula(String r1c1Formula, ArrayList<StructDefine.R1C1Relative> IV) {
		//Ìæ»»±äÁ¿
		r1c1Formula = preProcessDevide(r1c1Formula, IV);
		
		int index1 = r1c1Formula.indexOf("<");
		while(index1 != -1) {
			int index2 = r1c1Formula.indexOf(">");
			String head = r1c1Formula.substring(0, index1);
			String tail = r1c1Formula.substring(index2+1);
			
			String pos = r1c1Formula.substring(index1, index2+1);
			StructDefine.R1C1Relative r1c1 = StructDefine.R1C1Relative.convertStringToR1C1Relative(pos);

			String temp = null;
			for(int i = 0 ; i < IV.size() ; i++) {
				if(IV.get(i).equal(r1c1))
					temp = "[i" + i + "]";
			}
			r1c1Formula = head + temp + tail;
			index1 = r1c1Formula.indexOf("<");
		}
		
		//System.err.println(r1c1Formula);
		return r1c1Formula;
	}

	private static String preProcessDevide(String formula, ArrayList<StructDefine.R1C1Relative> IV) {
		int index = 0;
		while(index < formula.length() && formula.charAt(index) != '{')  {
			index++;
		}
		if(index == formula.length()) return formula;
		int count = 1; 
		index++;
		while(index < formula.length() && count != 0) {
			if(formula.charAt(index) == '{') count++;
			else if(formula.charAt(index) == '}') count--;
			index++;
		}
		return preProcessNest(formula.substring(0, index), IV) + preProcessDevide(formula.substring(index), IV);
	}
	
	private static String preProcessNest(String formula, ArrayList<StructDefine.R1C1Relative> IV) {
		int index1 = formula.indexOf("{");
		int index2 = formula.lastIndexOf("}");
		String head = formula.substring(0, index1+1);
		String tail = formula.substring(index2);
		String body = formula.substring(index1+1, index2);
		if(!body.contains("{"))
			return formula.substring(0, index1) + preProcessOneFunc(body,IV) + formula.substring(index2+1);
		else {
			return preProcessDevide(head + preProcessNest(body, IV) + tail, IV);
		}
	}
	
	private static String preProcessOneFunc(String func, ArrayList<StructDefine.R1C1Relative> IV) {
		if(!func.startsWith("SUM") && !func.startsWith("sum")) {
			return func;
		}
		String sumItemsString = func.substring(4, func.length()-1);
		ArrayList<String> sumItemsList = new ArrayList<>();
		String[] sumItems = sumItemsString.split(",");
		for(String s1 : sumItems) {
			String []s2 = s1.split(":");
			if(s2.length == 1) {
				String tempItem = preProcessFormula(s1, IV);
				while(tempItem.startsWith("+") || tempItem.startsWith("-"))
					tempItem = tempItem.substring(1);
				sumItemsList.add(tempItem);
				
			}
			else {
				StructDefine.R1C1Relative start = StructDefine.R1C1Relative.convertStringToR1C1Relative(s2[0]);
				StructDefine.R1C1Relative end = StructDefine.R1C1Relative.convertStringToR1C1Relative(s2[1]);
				
				for(int i = start.GetRow() ; i <= end.GetRow() ; i++)
					for(int j = start.GetColumn() ; j <= end.GetColumn() ; j++) {
						StructDefine.R1C1Relative r1c1 = new StructDefine.R1C1Relative(i, j);
						String temp = null;
						for(int k = 0 ; k < IV.size() ; k++) {
							if(IV.get(k).equal(r1c1))
								temp = "[i" + k + "]";
						}
						sumItemsList.add(temp);
					}	
			}
		}
		Collections.sort(sumItemsList);
		
		String sumResult = "(";
		for(String r1c1 : sumItemsList) {
			sumResult = sumResult + r1c1 + "+";
		}
		sumResult = sumResult.substring(0, sumResult.length() - 1);
		sumResult = sumResult + ")";
		return sumResult;
	}
	
	public static boolean validFormula(String string) {
		String s = string;
		s = s.replaceAll("i", "");
		s = s.replaceAll("SUM", "");
		s = s.replaceAll("sum", "");
		s = s.replaceAll("\\(", "");
		s = s.replaceAll("\\)", "");
		s = s.replaceAll(",", "");
		s = s.replaceAll("\\+", "");
		s = s.replaceAll("-", "");
		s = s.replaceAll("\\*", "");
		s = s.replaceAll("\\[", "");
		s = s.replaceAll("]", "");
		//s = s.replaceAll("/", "");
		for(int i = 0 ; i <= 9 ; i++)
			s = s.replaceAll((char)('0'+i)+"", "");
		
		if(s.length() != 0)
			return false;
		else
			return true;
	}
}
