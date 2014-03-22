package synthesis;

import java.util.ArrayList;
import java.util.List;
import synthesis.component.*;
import choco.Choco;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.kernel.model.Model;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;

public class Synthesis {
	public static void main(String[] args) {
		// function components
		List<Component> comps = new ArrayList<Component>();
		//comps.add(new SumComponent(3));
		comps.add(new PlusComponent());
		comps.add(new PlusComponent());
		//comps.add(new PlusComponent());

		// function inputs
		int numOfInput = 3;

		List<IOPair> pairs = new ArrayList<IOPair>();
		pairs.add(new IOPair(new int[] { 1, 2, 0}, 3));
		pairs.add(new IOPair(new int[] { 0, 4, 5}, 9));
		pairs.add(new IOPair(new int[] { 0, 1, 0}, 1));
		pairs.add(new IOPair(new int[] { 1, 0, 0}, 1));
		//pairs.add(new IOPair(new int[] { 7, 0}, 9));
		// pairs.add(new IOPair(new int[] { 0, 0}, 2));
		// pairs.add(new IOPair(new int[] { 0, 0}, 1));
		// pairs.add(new IOPair(new int[] { 0, 0 }, 2));
		// pairs.add(new IOPair(new int[] { 2, 0 }, 6));

		doSynthesis(pairs, numOfInput, comps);
	}
	
	public static String[] getSynthesisResult(List<IOPair> pairs, int numOfInput, List<Component> comps) {
		List<Component> newComps = new ArrayList<Component>();
		for (Component comp : comps) {
			if (comp.getId() == Components.CONSTANT) {
				newComps.add(comp);
			}
		}
		for (Component comp : comps) {
			if (comp.getId() != Components.CONSTANT) {
				newComps.add(comp);
			}
		}
		
		List<Type> types = extractTypes(newComps, numOfInput);
		Result res = generateProgram(types, newComps, numOfInput, pairs);
		
		if(res == null) return null;
		else
			return Lval2Prog.tranform(res, types, newComps, numOfInput);
	}

	public static void doSynthesis(List<IOPair> pairs, int numOfInput,
			List<Component> comps) {
		// sort the components, make the constants in the first
		List<Component> newComps = new ArrayList<Component>();
		for (Component comp : comps) {
			if (comp.getId() == Components.CONSTANT) {
				newComps.add(comp);
			}
		}
		for (Component comp : comps) {
			if (comp.getId() != Components.CONSTANT) {
				newComps.add(comp);
			}
		}
		
		// all the variable types
		List<Type> types = extractTypes(newComps, numOfInput);

		System.out.println("Start to generate a program...");
		Result res = generateProgram(types, newComps, numOfInput, pairs);
		System.out.println("Generating a program has done.");

		if (res == null) {
			System.out.println("No solution! Components insufficient!");
			return;
		} else {
			System.out.println("Current program:");
			Lval2Prog.tranform(res, types, newComps, numOfInput);
		}
		System.out.println("Start to generate distinct programs...");
		List<Integer> newInputs = generateDistinctProgram(types, newComps,
				numOfInput, pairs);
		System.out.println("Generating distinct programs have done.");

		if (newInputs != null) {
			System.out.print("New inputs:");
			for (int input : newInputs) {
				System.out.print(" " + input);
			}
		}
	}

	public static List<Integer> generateDistinctProgram(List<Type> types,
			List<Component> comps, int numOfInput, List<IOPair> pairs) {
		Model model = new CPModel();
		Solver solver = new CPSolver();

		// First program
		List<IntegerVariable> constVars1 = new ArrayList<IntegerVariable>();
		List<IntegerVariable> locs1 = addSynthesisConstraints(constVars1,
				model, types, comps, numOfInput, pairs);
		// Second program
		List<IntegerVariable> constVars2 = new ArrayList<IntegerVariable>();
		List<IntegerVariable> locs2 = addSynthesisConstraints(constVars2,
				model, types, comps, numOfInput, pairs);

		// function constraints for first program
		List<IntegerVariable> inputVars1 = new ArrayList<IntegerVariable>();
		List<IntegerVariable> outputVars1 = new ArrayList<IntegerVariable>();

		addFuncConstraints(model, comps, locs1, inputVars1, numOfInput,
				outputVars1, constVars1);
		IntegerVariable outputVar1 = outputVars1.get(0);

		// function constraints for second program
		List<IntegerVariable> inputVars2 = new ArrayList<IntegerVariable>();
		List<IntegerVariable> outputVars2 = new ArrayList<IntegerVariable>();

		addFuncConstraints(model, comps, locs2, inputVars2, numOfInput,
				outputVars2, constVars2);
		IntegerVariable outputVar2 = outputVars2.get(0);

		// input - output constraint for two program for different inputs
		for (int i = 0; i < numOfInput; i++) {
			model.addConstraint(Choco.eq(inputVars1.get(i), inputVars2.get(i)));

			// extra constraints, should be removed
			model.addConstraints(Choco.geq(inputVars1.get(i), 0),
					Choco.leq(inputVars1.get(i), 100));
		}
		// extra constraints, should be removed
		for (IntegerVariable constVar : constVars1) {
			model.addConstraints(Choco.geq(constVar, 0),
					Choco.leq(constVar, 100));
		}
		// extra constraints, should be removed
		for (IntegerVariable constVar : constVars2) {
			model.addConstraints(Choco.geq(constVar, 0),
					Choco.leq(constVar, 100));
		}
		
		model.addConstraint(Choco.neq(outputVar1, outputVar2));

		solver.read(model);
		solver.setTimeLimit(60000);
		solver.solve();

		System.out.println("Time spent on generating distinct program: " + solver.getTimeCount() + "ms");

		if (solver.getSolutionCount() > 0) {
			List<Integer> newInputs = new ArrayList<Integer>();
			for (int i = 0; i < numOfInput; i++) {
				int input = solver.getVar(inputVars1.get(i)).getVal();
				newInputs.add(input);
			}
			return newInputs;
		}
		return null;
	}

	// generate a program which satifies the input-output constraints.
	public static Result generateProgram(List<Type> types, List<Component> comps,
			int numOfInput, List<IOPair> pairs) {
		Model model = new CPModel();
		Solver solver = new CPSolver();

		List<IntegerVariable> constVars = new ArrayList<IntegerVariable>();
		List<IntegerVariable> locs = addSynthesisConstraints(constVars,
				model, types, comps, numOfInput, pairs);

		// extra constraints, should be removed
		for (IntegerVariable constVar : constVars) {
			model.addConstraints(Choco.geq(constVar, 0),
					Choco.leq(constVar, 100));
		}
		
		solver.read(model);
		solver.setTimeLimit(180000);
		solver.solve();

		System.out.println("Time spent on generating program: " + solver.getTimeCount() + "ms");

		// System.out.println("All solution count: " +
		// solver.getSolutionCount());

		if (solver.getSolutionCount() > 0) {
			Result res = resolveResult(solver, locs, constVars);
			return res;
		} else {
			return null;
		}
	}

	// synthesis constraints, return the location variable
	public static List<IntegerVariable> addSynthesisConstraints(
			List<IntegerVariable> constVars, Model model, List<Type> types,
			List<Component> comps, int numOfInput, List<IOPair> pairs) {
		
		List<IntegerVariable> locs = new ArrayList<IntegerVariable>();

		addWellFormConstraints(model, locs, types,
				Components.getComponents(comps), numOfInput, constVars);

		for (IOPair pair : pairs) {
			List<IntegerVariable> inputVars = new ArrayList<IntegerVariable>();
			List<IntegerVariable> outputVars = new ArrayList<IntegerVariable>();

			addFuncConstraints(model, comps, locs, inputVars, numOfInput,
					outputVars, constVars);
			IntegerVariable outputVar = outputVars.get(0);

			addInputOutputConstraints(model, inputVars, outputVar, pair.inputs,
					pair.output);
		}
		
		return locs;
	}

	// well formedness constrains
	public static void addWellFormConstraints(Model model,
			List<IntegerVariable> locs, List<Type> types,
			List<Component> components, int numOfInput, List<IntegerVariable> constantVars) {
				
		for (int i = 0; i < numOfInput; i++) {
			IntegerVariable locVar = Choco.makeIntVar("loc_input_" + (i + 1));
			locs.add(locVar);
		}

		// function output
		IntegerVariable locFunOutput = Choco.makeIntVar("loc_result");
		locs.add(locFunOutput);

		// all the constants
		for (Component comp : components) {
			if (comp.getId() == Components.CONSTANT) {			
				IntegerVariable locVar = Choco.makeIntVar("loc_"
						+ comp.getOutput().getName());
				locs.add(locVar);
				
				constantVars.add(comp.getOutput());
			}
		}
		
		// all the other components
		for (Component comp : components) {
			if (comp.getId() != Components.CONSTANT) {			
				for (IntegerVariable var : comp.getInputs()) {
					IntegerVariable locVar = Choco.makeIntVar("loc_"
							+ var.getName());
					locs.add(locVar);
				}

				IntegerVariable locVar = Choco.makeIntVar("loc_"
						+ comp.getOutput().getName());
				locs.add(locVar);
			}
		}

		// add all the location variables into the model
		for (IntegerVariable loc : locs) {
			model.addVariable(loc);
		}
		// add all the constant variables into the model
		for (IntegerVariable cons : constantVars) {
			model.addVariable(cons);
		}
		
		// well formedness constraints
		int sizeOfProgram = numOfInput + components.size();

		int constNum = constantVars.size();

		int constIndex = 0;
		int funInputIndex = 0;
		for (int i = 0; i < locs.size(); i++) {
			IntegerVariable loc = locs.get(i);
			Type type = types.get(i);
			if (type == Type.FUN_INPUT) { // constraints for the input
											// of
											// the function
				model.addConstraint(Choco.eq(loc, funInputIndex));
				funInputIndex++;
			} else if (type == Type.FUN_OUTPUT) { // constraints for the
													// output of the
													// function
				model.addConstraints(Choco.geq(loc, 0),
						Choco.lt(loc, sizeOfProgram));
			} else if (type == Type.COMP_INPUT) { // constraints for all
													// the
													// input of the
													// components
				model.addConstraints(Choco.geq(loc, 0),
						Choco.lt(loc, sizeOfProgram));
			} else if (type == Type.COMP_OUTPUT) { // constraints for
													// the
													// locations of the
													// components
				model.addConstraints(Choco.geq(loc, numOfInput + constNum),
						Choco.lt(loc, sizeOfProgram));
			} else if (type == Type.COMP_CONSTANT) { // constraints for the
														// locations of constant
														// components
				model.addConstraints(Choco.eq(loc, numOfInput + constIndex));
				constIndex ++;
			}
		}

		// constraints for one component in each line
		for (int i = 0; i < locs.size(); i++) {
			if (types.get(i) == Type.COMP_OUTPUT) {
				IntegerVariable iLoc = locs.get(i);
				for (int j = i + 1; j < locs.size(); j++) {
					if (types.get(j) == Type.COMP_OUTPUT) {
						IntegerVariable jLoc = locs.get(j);
						model.addConstraint(Choco.neq(iLoc, jLoc));
					}
				}
			}
		}

		// constraints for inputs of each component are defined before used
		for (int i = 0; i < locs.size(); i++) {
			if (types.get(i) != Type.COMP_INPUT) {
				continue;
			}
			if (i < locs.size()) {
				int j = i + 1;
				for (; j < locs.size(); j++) {
					if (types.get(j) == Type.COMP_OUTPUT) {
						break;
					}
				}
				for (int k = i; k < j; k++) {
					model.addConstraint(Choco.lt(locs.get(k), locs.get(j)));
				}
				
				// compute the next component
				i = j;
			}
		}
	}

	// function constraints
	public static void addFuncConstraints(Model model, List<Component> comps,
			List<IntegerVariable> locs, List<IntegerVariable> inputVars,
			int numOfInput, List<IntegerVariable> outputVars,
			List<IntegerVariable> constVars) {
		List<Component> components = Components.getComponents(comps);

		// all the variables used in the program
		List<IntegerVariable> allVars = new ArrayList<IntegerVariable>();

		// function inputs
		for (int i = 0; i < numOfInput; i++) {
			IntegerVariable var = Choco.makeIntVar("input_" + (i + 1));
			allVars.add(var);
			
			inputVars.add(var);
		}

		// function output
		IntegerVariable funOutput = Choco.makeIntVar("result");
		allVars.add(funOutput);
		
		outputVars.add(funOutput);

		// all the constant variables
		allVars.addAll(constVars);
		
		// all the other components
		for (Component comp : components) {
			if (comp.getId() != Components.CONSTANT) {
				allVars.addAll(comp.getInputs());
				allVars.add(comp.getOutput());
			}
		}

		// add all the variables into the model
		for (IntegerVariable var : allVars) {
			model.addVariable(var);
		}

		// the relation between location variables and program variables
		for (int i = 0; i < allVars.size(); i++) {
			for (int j = i + 1; j < allVars.size(); j++) {
				Constraint c = Choco.implies(
						Choco.eq(locs.get(i), locs.get(j)),
						Choco.eq(allVars.get(i), allVars.get(j)));
				model.addConstraint(c);
			}
		}

		// the specifications for the components
		for (Component comp : components) {
			model.addConstraint(comp.getSpecification());
		}

	}

	// input output constraints
	public static void addInputOutputConstraints(Model model,
			List<IntegerVariable> inputVars, IntegerVariable outputVar,
			int[] inputs, int output) {
		for (int i = 0; i < inputVars.size(); i++) {
			Constraint input = Choco.eq(inputVars.get(i), inputs[i]);
			model.addConstraint(input);
		}
		Constraint o = Choco.eq(outputVar, output);
		model.addConstraint(o);
	}

	// get the type for each location variables
	public static List<Type> extractTypes(List<Component> components, int numOfInput) {
		List<Type> types = new ArrayList<Type>();

		for (int i = 0; i < numOfInput; i++) {
			types.add(Type.FUN_INPUT);
		}

		// function output
		types.add(Type.FUN_OUTPUT);

		// all the constants
		for (Component comp : components) {
			if (comp.getId() == Components.CONSTANT) {
				types.add(Type.COMP_CONSTANT);
			}
		}
		// all the other components
		for (Component comp : components) {
			if (comp.getId() != Components.CONSTANT) {
				for (int i = 0; i < comp.getInputs().size(); i++) {
					types.add(Type.COMP_INPUT);
				}
				types.add(Type.COMP_OUTPUT);
			}
		}

		return types;
	}

	// transform the result from solver to int array for location variables
	public static Result resolveResult(Solver solver,
			List<IntegerVariable> locs, List<IntegerVariable> constantVars) {
		List<Integer> ls = new ArrayList<Integer>();

		for (IntegerVariable loc : locs) {
			int l = solver.getVar(loc).getVal();
			ls.add(l);
		}

		List<Integer> cs = new ArrayList<Integer>();
		for (IntegerVariable cons : constantVars) {
			int c = solver.getVar(cons).getVal();
			cs.add(c);
		}

		return new Result(ls, cs);
	}
}