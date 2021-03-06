package test;

import java.util.ArrayList;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

import parser.ConvertFormula;
import parser.FormulaParser;
import synthesis.util.Z3Util;
import core.Formula;
import core.StructDefine;

public class testCompatible {
	private ArrayList<StructDefine.R1C1Relative> IV;
	private ArrayList<StructDefine.Function> FUNC;
	
	public static void main(String [] args) throws Z3Exception {
		ArrayList<StructDefine.R1C1Relative> iv = new ArrayList<>();
		ArrayList<StructDefine.Function> func = new ArrayList<>();
		iv.add(new StructDefine.R1C1Relative(0, -2));
		iv.add(new StructDefine.R1C1Relative(0, -1));
		
		
		Formula formula1 = new Formula(new StructDefine.Position(0, 2), "A1+B1");
		StructDefine.Function func1 = new StructDefine.Function(formula1);
		func1.setFunc(FormulaParser.preProcessFormula(formula1.getR1C1Formula(), iv));
		func.add(func1);

		Formula formula2 = new Formula(new StructDefine.Position(0, 2), "A1-B1");
		StructDefine.Function func2 = new StructDefine.Function(formula2);
		func2.setFunc(FormulaParser.preProcessFormula(formula2.getR1C1Formula(), iv));
		func.add(func2);
		
		Formula formula3 = new Formula(new StructDefine.Position(0, 2), "A1");
		StructDefine.Function func3 = new StructDefine.Function(formula3);
		func3.setFunc(FormulaParser.preProcessFormula(formula3.getR1C1Formula(), iv));
		func.add(func3);
		
		Formula formula4 = new Formula(new StructDefine.Position(0, 2), "B1");
		StructDefine.Function func4 = new StructDefine.Function(formula4);
		func4.setFunc(FormulaParser.preProcessFormula(formula4.getR1C1Formula(), iv));
		func.add(func4);
		
		testCompatible tc = new testCompatible(iv, func);
		ArrayList<ArrayList<StructDefine.Function>> res = tc.classify();
		
		System.out.println(res.size() + "," + res.get(0).size() + "," + res.get(1).size());
	}
	
	public testCompatible(ArrayList<StructDefine.R1C1Relative> iv, ArrayList<StructDefine.Function> func) {
		IV = iv;
		FUNC = func;
	}
	
	public ArrayList<ArrayList<StructDefine.Function>> classify() throws Z3Exception {
		ArrayList<ArrayList<StructDefine.Function>> groups = new ArrayList<>();
		int index = 0;
		while(index != -1) {
			ArrayList<StructDefine.Function> newGroup = new ArrayList<>();
			newGroup.add(FUNC.get(index));
			FUNC.get(index).setClassified();
			for(StructDefine.Function func : FUNC){
				boolean existed = false, compatible = true;
				for(StructDefine.Function function : newGroup) {
					if(func == function) existed = true;
					if(!isCompatible(function, func)) compatible = false;
				}
				if(existed || !compatible) continue;
				newGroup.add(func);
				func.setClassified();
			}
			groups.add(newGroup);
			
			index = -1;
			for(int i = 0 ; i < FUNC.size() ; i++) {
				if(!FUNC.get(i).classified()){
					index = i; break;
				}
			}
		}
		return groups;
	}
	
	private boolean isCompatible(StructDefine.Function func1, StructDefine.Function func2) throws Z3Exception {
		Context ctx = Z3Util.getContext();
		Solver solver = ctx.mkSolver();
		
		ArrayList<ArithExpr> varsTable = new ArrayList<>();
		for(int i = 0 ; i < IV.size() ; i++) {
			varsTable.add(ctx.mkIntConst("i"+i));
		}
		
		System.out.println(func1.getFunc() + func2.getFunc());
		
		ConvertFormula convertFormula = new ConvertFormula();
		ArithExpr iev1 = convertFormula.convertFormula(func1.getFunc(), varsTable, ctx);
		ArithExpr iev2 = convertFormula.convertFormula(func2.getFunc(), varsTable, ctx);
		
		solver.add(ctx.mkNot(ctx.mkEq(iev1, iev2)));;
		
		for(int i = 0 ; i < IV.size() ; i++) {
			if(!func1.getFunc().contains("i"+i) || !func2.getFunc().contains("i"+i))
				solver.add(ctx.mkEq(ctx.mkInt(0), varsTable.get(i)));
		}
		
		if (Status.SATISFIABLE == solver.check()) {
			return false;
		} else {
			return true;
		}
	}
}
