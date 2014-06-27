package synthesis.io;

import java.util.ArrayList;
import java.util.List;

import synthesis.basic.BasicSynthesis;
import synthesis.basic.IOPair;
import synthesis.basic.Lval2Prog;
import synthesis.basic.ProgramAbstract;
import synthesis.basic.ProgramInstance;
import synthesis.basic.Result;
import synthesis.basic.Specification;
import synthesis.basic.Type;
import synthesis.component.Component;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

public class IOSynthesis extends BasicSynthesis {

	public static void doSynthesis(List<Type> inputTypes,
			List<Component> comps, List<IOPair> ioPairs) throws Z3Exception {
		doSynthesis(inputTypes, null, comps, ioPairs);
	}

	public static void doSynthesis(List<Type> inputTypes,
			List<Component> comps, Specification spec) throws Z3Exception {
		doSynthesis(inputTypes, null, comps, spec);
	}

	public static void doSynthesis(List<Type> inputTypes, Type outputType,
			List<Component> comps, List<IOPair> ioPairs) throws Z3Exception {

		ProgramAbstract program = new ProgramAbstract(comps, inputTypes,
				outputType);
		program.init();

		System.out.println("Start to generate a program...");
		Result res = generateProgram(program, ioPairs);
		System.out.println("Generating a program has done.");

		if (res == null) {
			System.out.println("No solution! Components insufficient!");
			return;
		} else {
			System.out.println("Current program:");
			Lval2Prog.tranform(res, program, true);
		}
		System.out.println("Start to generate distinct programs...");
		Object[] newInputs = generateDistinctProgram(program, ioPairs);
		System.out.println("Generating distinct programs have done.");

		if (newInputs != null) {
			System.out.println("New inputs:" + printObjects(newInputs));
		}
	}

	public static void doSynthesis(List<Type> inputTypes, Type outputType,
			List<Component> comps, Specification spec) throws Z3Exception {

		List<IOPair> ioPairs = new ArrayList<IOPair>();
		ioPairs.add(spec.getInitIO(inputTypes));

		for (int iter = 1;; iter++) {

			System.out.println();
			System.out.println("Iteration " + iter + ":");
			printIOPairs(ioPairs);

			ProgramAbstract program = new ProgramAbstract(comps, inputTypes,
					outputType);
			program.init();

			Result res = generateProgram(program, ioPairs);

			if (res == null) {
				System.out.println("No solution! Components insufficient!");
				return;
			} else {
				System.out.println("Current program:");
				Lval2Prog.tranform(res, program, true);
			}

			Object[] newInputs = generateDistinctProgram(program, ioPairs);

			if (newInputs != null) {
				ioPairs.add(spec.getIO(newInputs));

				System.out.print("New inputs:" + printObjects(newInputs));
				System.out.println();
			} else {
				break;
			}
		}
	}

	// generate a program which satisfies the input-output constraints.
	public static Result generateProgram(ProgramAbstract program,
			List<IOPair> ioPairs) throws Z3Exception {
		Solver solver = ctx.mkSolver();

		addSynthesisConstraints(solver, program, ioPairs);

		long start = System.currentTimeMillis();
		Params params = ctx.mkParams();
		params.add("timeout", 180000);
		solver.setParameters(params);
		Status status = solver.check();
		long end = System.currentTimeMillis();

		System.out.println("Time spent on generating program: " + (end - start)
				+ "ms");

		if (status == Status.SATISFIABLE) {
			Result res = resolveResult(solver, program);
			return res;
		} else {
			return null;
		}
	}

	// synthesis constraints
	public static void addSynthesisConstraints(Solver solver,
			ProgramAbstract program, List<IOPair> ioPairs) throws Z3Exception {

		addWellFormConstraints(solver, program);

		for (IOPair pair : ioPairs) {
			ProgramInstance pi = program.getInstance();
			pi.init();

			addFuncConstraints(solver, pi);

			addInputOutputConstraints(solver, pi, pair);
		}
	}

	// input output constraints
	public static void addInputOutputConstraints(Solver solver,
			ProgramInstance pi, IOPair pair) throws Z3Exception {

		addInputConstraint(ctx, solver, pi, pair);

		Object output = pair.output;
		if (output instanceof Integer) {
			BoolExpr o = ctx.mkEq(pi.outputVar,
					ctx.mkInt((Integer) pair.output));
			solver.add(o);
		} else if (output instanceof Boolean) {
			BoolExpr o = ctx.mkEq(pi.outputVar,
					ctx.mkBool((Boolean) pair.output));
			solver.add(o);
		}
	}

	// generate distinct program
	public static Object[] generateDistinctProgram(ProgramAbstract program,
			List<IOPair> ioPairs) throws Z3Exception {
		Solver solver = ctx.mkSolver();

		// First program
		ProgramAbstract prog1 = new ProgramAbstract(program.components,
				program.inputTypes, program.outputType);
		prog1.init();
		addSynthesisConstraints(solver, prog1, ioPairs);

		// Second program
		ProgramAbstract prog2 = new ProgramAbstract(program.components,
				program.inputTypes, program.outputType);
		prog2.init();
		addSynthesisConstraints(solver, prog2, ioPairs);

		// function constraints for first program
		ProgramInstance pi1 = prog1.getInstance();
		pi1.init();
		addFuncConstraints(solver, pi1);

		// function constraints for second program
		ProgramInstance pi2 = prog2.getInstance();
		pi2.init();
		addFuncConstraints(solver, pi2);

		// input - output constraint for two program for same inputs
		for (int i = 0; i < program.inputTypes.size(); i++) {
			solver.add(ctx.mkEq(pi1.inputVars.get(i), pi2.inputVars.get(i)));
		}

		solver.add(ctx.mkNot(ctx.mkEq(
				pi1.outputVar, pi2.outputVar)));

		long start = System.currentTimeMillis();
		Params params = ctx.mkParams();
		params.add("timeout", 18000000);
		solver.setParameters(params);
		Status status = solver.check();
		long end = System.currentTimeMillis();

		System.out.println("Time spent on generating distinct program: "
				+ (end - start) + "ms");

		if (status == Status.SATISFIABLE) {
			printDistinctPrograms(solver, prog1, prog2);

			Model model = solver.getModel();

			Object[] newInputs = resolveInput(model, program, pi1);
			
			resolveOutputs(model, program, pi1, pi2);

			return newInputs;
		}
		return null;
	}
}