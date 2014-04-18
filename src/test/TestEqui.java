package test;

import java.util.ArrayList;

import parser.ConvertFormula;
import choco.Choco;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.kernel.model.Model;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;

public class TestEqui {
	public static void main(String[] args) {
		Model model = new CPModel();
		Solver solver = new CPSolver();
		
		IntegerVariable varX1 = Choco.makeIntVar("i0");
		IntegerVariable varX2 = Choco.makeIntVar("i1");
		
		ArrayList<IntegerVariable> table = new ArrayList<>();
	
		table.add(varX1);
		table.add(varX2);
		
		ConvertFormula cf = new ConvertFormula();
		IntegerExpressionVariable iev = cf.convertFormula("i0+SUM(i0,i1)+i1", table);
		
		Constraint cons = Choco.neq(Choco.plus(table.get(0),Choco.plus(table.get(1),Choco.plus(table.get(0),table.get(1)))), iev);
		
		model.addConstraint(cons);
		
		solver.read(model);
		solver.setTimeLimit(180000);
		solver.solve();
		
		if (solver.getSolutionCount() > 0) {
			System.out.println("semantically non-equivalent!");
		} else {
			System.out.println("semantically equivalent!");
		}
	}
}
