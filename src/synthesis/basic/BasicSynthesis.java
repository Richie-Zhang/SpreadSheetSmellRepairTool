package synthesis.basic;

import java.util.ArrayList;
import java.util.List;

import synthesis.component.Component;
import synthesis.util.Z3Util;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Z3Exception;

public class BasicSynthesis {

	public static Context ctx = Z3Util.getContext();

	// well formedness constrains
	public static void addWellFormConstraints(Solver solver,
			ProgramAbstract program) throws Z3Exception {

		// well formedness constraints
		int psize = program.getSize();
		List<IntExpr> locs = program.locVars;
		List<Type> types = program.types;

		int funInputIndex = 0;
		for (int i = 0; i < locs.size(); i++) {
			IntExpr loc = locs.get(i);
			Type type = types.get(i);
			if (type.ioType == IOType.FUN_INPUT) {
				// constraints for the input of the function
				solver.add(ctx.mkEq(loc, ctx.mkInt(funInputIndex)));
				funInputIndex++;
			} else if (type.ioType == IOType.FUN_OUTPUT) {
				// constraints for the output of the function
				solver.add(ctx.mkGe(loc, ctx.mkInt(0)),
						ctx.mkLt(loc, ctx.mkInt(psize)));
			} else if (type.ioType == IOType.COMP_INPUT) {
				// constraints for all the input of the components
				solver.add(ctx.mkGe(loc, ctx.mkInt(0)),
						ctx.mkLt(loc, ctx.mkInt(psize)));
			} else if (type.ioType == IOType.COMP_OUTPUT) {
				// constraints for the locations of the components
				solver.add(ctx.mkGe(loc, ctx.mkInt(program.inputTypes.size())),
						ctx.mkLt(loc, ctx.mkInt(psize)));
			}
		}

		// constraints for one component in each line
		for (int i = 0; i < locs.size(); i++) {
			if (types.get(i).ioType == IOType.COMP_OUTPUT) {
				IntExpr iLoc = locs.get(i);
				for (int j = i + 1; j < locs.size(); j++) {
					if (types.get(j).ioType == IOType.COMP_OUTPUT) {
						IntExpr jLoc = locs.get(j);
						solver.add(ctx.mkNot(ctx.mkEq(iLoc, jLoc)));
					}
				}
			}
		}

		// constraints for inputs of each component are defined before used
		for (int i = 0; i < locs.size(); i++) {
			if (types.get(i).ioType != IOType.COMP_INPUT) {
				continue;
			}
			if (i < locs.size()) {
				int j = i + 1;
				for (; j < locs.size(); j++) {
					if (types.get(j).ioType == IOType.COMP_OUTPUT) {
						break;
					}
				}
				for (int k = i; k < j; k++) {
					solver.add(ctx.mkLt(locs.get(k), locs.get(j)));
				}

				// compute the next component
				i = j;
			}
		}
	}

	// function constraints
	public static void addFuncConstraints(Solver solver, ProgramInstance pi)
			throws Z3Exception {

		List<IntExpr> locs = pi.program.locVars;
		List<Expr> allVars = pi.allVars;

		// the relation between location variables and program variables
		for (int i = 0; i < allVars.size(); i++) {
			for (int j = i + 1; j < allVars.size(); j++) {
				BoolExpr c = null;
				if (allVars.get(i).getClass() != allVars.get(j).getClass()) {
					// the type constraints
					c = ctx.mkNot(ctx.mkEq(locs.get(i), locs.get(j)));
				} else {
					// data flow constraints
					c = ctx.mkImplies(ctx.mkEq(locs.get(i), locs.get(j)),
							ctx.mkEq(allVars.get(i), allVars.get(j)));
				}
				solver.add(c);
			}
		}

		// the specifications for the components
		for (Component comp : pi.components) {
			solver.add(comp.getSpecification());
		}
	}

	// transform the result from solver to int array for location variables
	public static Result resolveResult(Solver solver, ProgramAbstract program)
			throws Z3Exception {

		Model model = solver.getModel();

		List<Integer> ls = new ArrayList<Integer>();

		for (IntExpr loc : program.locVars) {
			int l = ((IntNum) model.getConstInterp(loc)).getInt();
			ls.add(l);
		}

		List<Integer> cs = new ArrayList<Integer>();
		for (IntExpr cons : program.constVars) {
			int c = 0;
			try {
				c = ((IntNum) model.getConstInterp(cons)).getInt();
			} catch (Exception e) {
			}
			cs.add(c);
		}

		return new Result(ls, cs);
	}

	public static Object[] resolveInput(Model model, ProgramAbstract program,
			ProgramInstance pi) throws Z3Exception {

		List<Object> ret = new ArrayList<Object>();

		for (int i = 0; i < program.inputTypes.size(); i++) {
			if (program.inputTypes.get(i).varType == VarType.INTEGER) {
				int input = ((IntNum) model.getConstInterp(pi.inputVars.get(i)))
						.getInt();
				ret.add(input);
			} else if (program.inputTypes.get(i).varType == VarType.BOOLEAN) {
				//
			} else {
				//
			}
		}
		return ret.toArray();
	}

	public static void resolveOutputs(Model model, ProgramAbstract program,
			ProgramInstance pi1, ProgramInstance pi2) throws Z3Exception {

		if (program.outputType.varType == VarType.INTEGER) {
			int o1 = ((IntNum) model.getConstInterp(pi1.outputVar)).getInt();
			int o2 = ((IntNum) model.getConstInterp(pi2.outputVar)).getInt();
			System.out.println("Output 1:" + o1);
			System.out.println("Output 2:" + o2);
		} else if (program.outputType.varType == VarType.BOOLEAN) {
			int o1 = ((BoolExpr) model.getConstInterp(pi1.outputVar))
					.getBoolValue().toInt();
			int o2 = ((BoolExpr) model.getConstInterp(pi2.outputVar))
					.getBoolValue().toInt();
			System.out.println("Output 1:" + o1);
			System.out.println("Output 2:" + o2);
		} else {
		}
	}

	public static void addInputConstraint(Context ctx, Solver solver,
			ProgramInstance pi, IOPair pair) throws Z3Exception {
		for (int i = 0; i < pi.inputVars.size(); i++) {
			Object input = pair.inputs[i];
			if (input instanceof Integer) {
				BoolExpr inputCons = ctx.mkEq(pi.inputVars.get(i),
						ctx.mkInt((Integer) input));
				solver.add(inputCons);
			} else if (input instanceof Boolean) {
				BoolExpr inputCons = ctx.mkEq(pi.inputVars.get(i),
						ctx.mkBool((Boolean) input));
				solver.add(inputCons);
			} else if (input.getClass().isArray()) {
				int[] arrInput = (int[]) input;
				ArrayExpr varInput = (ArrayExpr) pi.inputVars.get(i);
				for (int j = 0; j < arrInput.length; j++) {
					BoolExpr arrInputCons = ctx.mkEq(
							ctx.mkSelect(varInput, ctx.mkInt(j)),
							ctx.mkInt(arrInput[j]));
					solver.add(arrInputCons);
				}
			}
		}
	}

	public static void printDistinctPrograms(Solver solver,
			ProgramAbstract prog1, ProgramAbstract prog2) throws Z3Exception {

		System.out.println("Distinct program 1:");
		Result res1 = resolveResult(solver, prog1);
		Lval2Prog.tranform(res1, prog1, true);

		System.out.println("Distinct program 2:");
		Result res2 = resolveResult(solver, prog2);
		Lval2Prog.tranform(res2, prog2, true);
	}

	public static void printIOPairs(List<IOPair> ioPairs) {
		System.out.println("input-output pairs:");
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < ioPairs.size(); i++) {
			IOPair pair = ioPairs.get(i);
			sb.append(pair);

			if (i < ioPairs.size() - 1) {
				sb.append(", ");
			}
		}
		System.out.println(sb);
	}

	public static String printObjects(Object[] inputs) {
		StringBuffer sb = new StringBuffer();
		for (Object in : inputs) {
			if (in.getClass().isArray()) {
				int[] arr = (int[]) in;
				sb.append("\"");
				for (int i = 0; i < arr.length; i++) {
					sb.append(arr[i]);
				}
				sb.append("\"");
			} else {
				sb.append(in);
			}
			sb.append(",");
		}
		sb.delete(sb.length() - 1, sb.length());
		return sb.toString();
	}
}