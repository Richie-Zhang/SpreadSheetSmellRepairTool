package test;

import java.util.ArrayList;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

import parser.ConvertFormula;
import synthesis.util.Z3Util;

public class TestEqui {
	public static void main(String[] args) throws Z3Exception {
		Context ctx = Z3Util.getContext();
		Solver solver = ctx.mkSolver();
		
		ArithExpr varX1 = ctx.mkIntConst("i0");
		ArithExpr varX2 = ctx.mkIntConst("i1");
		
		ArrayList<ArithExpr> table = new ArrayList<>();
	
		table.add(varX1);
		table.add(varX2);
		
		ConvertFormula cf = new ConvertFormula();
		ArithExpr iev = cf.convertFormula("i0+SUM(i0,i1)+i1", table, ctx);
		
		solver.add(ctx.mkNot(ctx.mkEq(iev, ctx.mkAdd(table.get(0), table.get(1), table.get(1)))));
		
		if (Status.SATISFIABLE == solver.check()) {
			System.out.println("semantically non-equivalent!");
		} else {
			System.out.println("semantically equivalent!");
		}
	}
}
