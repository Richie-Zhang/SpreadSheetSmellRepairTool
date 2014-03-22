package parser;

import java.util.ArrayList;
import java.util.Collections;

import choco.Choco;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.kernel.model.Model;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;
import core.Formula;
import core.StructDefine;

public class FormulaParser {

	public static boolean isEquivalent(Formula formula1, Formula formula2, ArrayList<StructDefine.R1C1Relative> IV) {
		String for1 = preProcessFormula(formula1.getR1C1Formula(), IV);
		String for2 = preProcessFormula(formula2.getR1C1Formula(), IV);
		
		if(!validFormula(for1) || !validFormula(for2))
			return false;
		
		return isEquivalent(for1, for2, IV);
	}
	
	public static boolean isEquivalent(String for1, String for2, ArrayList<StructDefine.R1C1Relative> IV) {
		ArrayList<IntegerVariable> varsTable = new ArrayList<>();
		for(int i = 0 ; i < IV.size() ; i++) {
			varsTable.add(Choco.makeIntVar("i"+i));
		}
		
		ConvertFormula convertFormula = new ConvertFormula();
		IntegerExpressionVariable iev1 = convertFormula.convertFormula(for1, varsTable);
		IntegerExpressionVariable iev2 = convertFormula.convertFormula(for2, varsTable);
		
		Constraint cons = Choco.neq(iev1, iev2);
		
		Model model = new CPModel();
		Solver solver = new CPSolver();
		
		model.addConstraint(cons);
		
		solver.read(model);
		solver.setTimeLimit(180000);
		solver.solve();
		
		if (solver.getSolutionCount() > 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public static String preProcessFormula(String r1c1Formula, ArrayList<StructDefine.R1C1Relative> IV) {
		//处理Sum
		int index1 = r1c1Formula.indexOf("{");
		while(index1 != -1) {
			int index2 = r1c1Formula.indexOf("}", index1);
			String head = r1c1Formula.substring(0, index1);
			String tail = r1c1Formula.substring(index2+1);
			
			String sum = r1c1Formula.substring(index1, index2+1);
			if(!sum.startsWith("{SUM") && !sum.startsWith("{sum")) {
				r1c1Formula = head + sum.substring(1, sum.length()-1) + tail;
				index1 = r1c1Formula.indexOf("{");
				continue;
			}
			String sumItemsString = sum.substring(5, sum.length()-2);
			ArrayList<StructDefine.R1C1Relative> sumItemsList = new ArrayList<>();
			String[] sumItems = sumItemsString.split(",");
			for(String s1 : sumItems) {
				String []s2 = s1.split(":");
				if(s2.length == 1) {
					sumItemsList.add(StructDefine.R1C1Relative.convertStringToR1C1Relative(s1));
				}
				else {
					StructDefine.R1C1Relative start = StructDefine.R1C1Relative.convertStringToR1C1Relative(s2[0]);
					StructDefine.R1C1Relative end = StructDefine.R1C1Relative.convertStringToR1C1Relative(s2[1]);
					for(int i = start.GetRow() ; i <= end.GetRow() ; i++)
						for(int j = start.GetColumn() ; j <= end.GetColumn() ; j++)
							sumItemsList.add(new StructDefine.R1C1Relative(i, j));
				}
			}
			Collections.sort(sumItemsList);
			
			String sumResult = "(";
			for(StructDefine.R1C1Relative r1c1 : sumItemsList) {
				for(int i = 0 ; i < IV.size() ; i++) {
					if(IV.get(i).equal(r1c1))
						sumResult = sumResult + "i" + i + "+";
				}
			}
			sumResult = sumResult.substring(0, sumResult.length() - 1);
			sumResult = sumResult + ")";
			r1c1Formula = head + sumResult + tail;
			index1 = r1c1Formula.indexOf("{");
		}
		
		//替换变量
		index1 = r1c1Formula.indexOf("<");
		while(index1 != -1) {
			int index2 = r1c1Formula.indexOf(">");
			String head = r1c1Formula.substring(0, index1);
			String tail = r1c1Formula.substring(index2+1);
			
			String pos = r1c1Formula.substring(index1, index2+1);
			StructDefine.R1C1Relative r1c1 = StructDefine.R1C1Relative.convertStringToR1C1Relative(pos);

			String temp = null;
			for(int i = 0 ; i < IV.size() ; i++) {
				if(IV.get(i).equal(r1c1))
					temp = "i" + i;
			}
			r1c1Formula = head + temp + tail;
			index1 = r1c1Formula.indexOf("<");
		}
		
		return r1c1Formula;
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
		s = s.replaceAll("/", "");
		for(int i = 0 ; i <= 9 ; i++)
			s = s.replaceAll((char)('0'+i)+"", "");
		if(s.length() != 0)
			return false;
		else
			return true;
	}
}
