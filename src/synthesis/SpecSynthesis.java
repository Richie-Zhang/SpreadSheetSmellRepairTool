package synthesis;

import java.util.ArrayList;
import java.util.List;

import parser.Calculate;
import synthesis.component.*;
import choco.Choco;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.kernel.model.Model;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;
import core.StructDefine;

public class SpecSynthesis {
	private ArrayList<StructDefine.Function> group;
	private ArrayList<IntegerVariable> varsTable;
	
	public SpecSynthesis(ArrayList<StructDefine.Function> func, ArrayList<IntegerVariable> vars) {
		this.group = func;
		this.varsTable = vars;
	}
	
	public String[] doIOSynthesis(ArrayList<IOPair> ioPairs, ArrayList<IOPair> sheetIoPairs, ArrayList<Component> comps) {
		System.out.println("in---doIOSynthesis");
		ArrayList<Component> newComps = new ArrayList<Component>();
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
		System.out.println(varsTable.size());
		ArrayList<Type> types = extractTypes(newComps);
		
		ArrayList<IOPair> curPairs = ioPairs;
		Result result = null, preresult = null;
		
		for(int i = 0 ; i < sheetIoPairs.size() ; i++) {
			/*else {
				List<Integer> newInputs = generateDistinctProgram(types, newComps, curPairs, false);
				if(newInputs == null) break;
			}*/
			curPairs.add(sheetIoPairs.get(i));
			if(result != null)
				preresult = result;
			result = generateProgram(types, newComps, curPairs);
			if(result == null) {
				curPairs.remove(curPairs.size() - 1);
			}
		}
		System.out.println("out---doIOSynthesis");
		if(result == null && preresult == null) return null;
		else if(result != null)
			return Lval2Prog.tranform(result, types, newComps, varsTable.size());
		else
			return Lval2Prog.tranform(preresult, types, newComps, varsTable.size());
	}
	
	public ArrayList<IOPair> doSpecSynthesis(ArrayList<Component> comps) {
		System.out.println("in---doSpecSynthesis");
		ArrayList<Component> newComps = new ArrayList<Component>();
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
		ArrayList<Type> types = extractTypes(newComps);
		
		ArrayList<IOPair> ioPairs = new ArrayList<>();
		
		while(true) {
			ArrayList<Integer> newInputs = generateDistinctProgram(types, comps, ioPairs, true);
			if(newInputs != null) {
				ioPairs.add(generateIOPair(newInputs));
			}
			else break;
		}
		System.out.println("out---doSpecSynthesis");
		return ioPairs;
	}
	
	// generate a program which satifies the input-output constraints.
	private Result generateProgram(ArrayList<Type> types, ArrayList<Component> comps, ArrayList<IOPair> pairs) {
		System.out.println("in---generateProgram");
		Model model = new CPModel();
		Solver solver = new CPSolver();

		ArrayList<IntegerVariable> constVars = new ArrayList<IntegerVariable>();
		List<IntegerVariable> locs = addSynthesisConstraints(constVars, model, types, comps, pairs);

		// extra constraints, should be removed
		for (IntegerVariable constVar : constVars) {
			model.addConstraints(Choco.geq(constVar, 0), Choco.leq(constVar, 100));
		}
		
		solver.read(model);
		solver.setTimeLimit(1800000);
		solver.solve();

		//System.out.println("Time spent on generating program: " + solver.getTimeCount() + "ms");
		System.out.println("out---generateProgram");
		
		if (solver.getSolutionCount() > 0) {
			Result res = resolveResult(solver, locs, constVars);
			return res;
		} else {
			return null;
		}
	}
	
	private ArrayList<Integer> generateDistinctProgram(ArrayList<Type> types, ArrayList<Component> comps, ArrayList<IOPair> pairs, boolean inputCons) {
		System.out.println("in---generateDistinctProgram");
		Model model = new CPModel();
		Solver solver = new CPSolver();
		
		// First program
		ArrayList<IntegerVariable> constVars1 = new ArrayList<IntegerVariable>();
		ArrayList<IntegerVariable> locs1 = addSynthesisConstraints(constVars1, model, types, comps, pairs);
		// Second program
		ArrayList<IntegerVariable> constVars2 = new ArrayList<IntegerVariable>();
		ArrayList<IntegerVariable> locs2 = addSynthesisConstraints(constVars2, model, types, comps, pairs);
		
		// function constraints for first program
		ArrayList<IntegerVariable> inputVars1 = new ArrayList<IntegerVariable>();
		ArrayList<IntegerVariable> outputVars1 = new ArrayList<IntegerVariable>();

		addFuncConstraints(model, comps, locs1, inputVars1, outputVars1, constVars1);
		IntegerVariable outputVar1 = outputVars1.get(0);
		
		// function constraints for second program
		ArrayList<IntegerVariable> inputVars2 = new ArrayList<IntegerVariable>();
		ArrayList<IntegerVariable> outputVars2 = new ArrayList<IntegerVariable>();

		addFuncConstraints(model, comps, locs2, inputVars2, outputVars2, constVars2);
		IntegerVariable outputVar2 = outputVars2.get(0);

		// input - output constraint for two program for different inputs
		for (int i = 0; i < varsTable.size(); i++) {
			model.addConstraint(Choco.eq(inputVars1.get(i), inputVars2.get(i)));
			// extra constraints, should be removed
			model.addConstraints(Choco.geq(inputVars1.get(i), 0), Choco.leq(inputVars1.get(i), 100));
		}
		// extra constraints, should be removed
		for (IntegerVariable constVar : constVars1) {
			model.addConstraints(Choco.geq(constVar, 0), Choco.leq(constVar, 100));
		}
		// extra constraints, should be removed
		for (IntegerVariable constVar : constVars2) {
			model.addConstraints(Choco.geq(constVar, 0), Choco.leq(constVar, 100));
		}
		
		if(inputCons) {
			Constraint inputConstraint = getInputConstraints();
			model.addConstraint(inputConstraint);
		}
		
		model.addConstraint(Choco.neq(outputVar1, outputVar2));

		solver.read(model);
		solver.setTimeLimit(10000);
		solver.solve();

		System.out.println("out---generateDistinctProgram");
		
		if (solver.getSolutionCount() > 0) {
			ArrayList<Integer> newInputs = new ArrayList<Integer>();
			for (int i = 0; i < varsTable.size(); i++) {
				int input = solver.getVar(inputVars1.get(i)).getVal();
				newInputs.add(input);
				System.out.print(input + ",");
			}
			System.out.println();
			return newInputs;
		}
		return null;
	}
	
	// synthesis constraints, return the location variable
	private ArrayList<IntegerVariable> addSynthesisConstraints(ArrayList<IntegerVariable> constVars, Model model, ArrayList<Type> types, ArrayList<Component> comps, ArrayList<IOPair> pairs) {
		ArrayList<IntegerVariable> locs = new ArrayList<IntegerVariable>();
		addWellFormConstraints(model, locs, types, Components.getComponents(comps), constVars);
		for (IOPair pair : pairs) {
			ArrayList<IntegerVariable> inputVars = new ArrayList<IntegerVariable>();
			ArrayList<IntegerVariable> outputVars = new ArrayList<IntegerVariable>();

			addFuncConstraints(model, comps, locs, inputVars, outputVars, constVars);
			IntegerVariable outputVar = outputVars.get(0);

			addInputOutputConstraints(model, inputVars, outputVar, pair.inputs, pair.output);
		}
		return locs;
	}
	
	// function constraints
	private void addFuncConstraints(Model model, ArrayList<Component> comps, List<IntegerVariable> locs, List<IntegerVariable> inputVars, List<IntegerVariable> outputVars, List<IntegerVariable> constVars) {
		List<Component> components = Components.getComponents(comps);
		// all the variables used in the program
		List<IntegerVariable> allVars = new ArrayList<IntegerVariable>();
		// function inputs
		for (int i = 0; i < varsTable.size(); i++) {
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
				Constraint c = Choco.implies(Choco.eq(locs.get(i), locs.get(j)), Choco.eq(allVars.get(i), allVars.get(j)));
				model.addConstraint(c);
			}
		}
		// the specifications for the components
		for (Component comp : components) {
			model.addConstraint(comp.getSpecification());
		}
	}

	// well formedness constrains
	private void addWellFormConstraints(Model model, List<IntegerVariable> locs, List<Type> types, List<Component> components, List<IntegerVariable> constantVars) {
		for (int i = 0; i < varsTable.size(); i++) {
			IntegerVariable locVar = Choco.makeIntVar("loc_input_" + (i + 1));
			locs.add(locVar);
		}
		// function output
		IntegerVariable locFunOutput = Choco.makeIntVar("loc_result");
		locs.add(locFunOutput);
		// all the constants
		for (Component comp : components) {
			if (comp.getId() == Components.CONSTANT) {			
				IntegerVariable locVar = Choco.makeIntVar("loc_" + comp.getOutput().getName());
				locs.add(locVar);
				constantVars.add(comp.getOutput());
			}
		}
		// all the other components
		for (Component comp : components) {
			if (comp.getId() != Components.CONSTANT) {			
				for (IntegerVariable var : comp.getInputs()) {
					IntegerVariable locVar = Choco.makeIntVar("loc_" + var.getName());
					locs.add(locVar);
				}
				IntegerVariable locVar = Choco.makeIntVar("loc_" + comp.getOutput().getName());
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
		int sizeOfProgram = varsTable.size() + components.size();
		int constNum = constantVars.size();
		int constIndex = 0;
		int funInputIndex = 0;
		for (int i = 0; i < locs.size(); i++) {
			IntegerVariable loc = locs.get(i);
			Type type = types.get(i);
			if (type == Type.FUN_INPUT) { // constraints for the input of the function
				model.addConstraint(Choco.eq(loc, funInputIndex));
				funInputIndex++;
			} else if (type == Type.FUN_OUTPUT) { // constraints for the output of the function
				model.addConstraints(Choco.geq(loc, 0), Choco.lt(loc, sizeOfProgram));
			} else if (type == Type.COMP_INPUT) { // constraints for all the input of the components
				model.addConstraints(Choco.geq(loc, 0), Choco.lt(loc, sizeOfProgram));
			} else if (type == Type.COMP_OUTPUT) { // constraints for the locations of the components
				model.addConstraints(Choco.geq(loc, varsTable.size() + constNum), Choco.lt(loc, sizeOfProgram));
			} else if (type == Type.COMP_CONSTANT) { // constraints for the locations of constant components
				model.addConstraints(Choco.eq(loc, varsTable.size() + constIndex));
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
	
	private void addInputOutputConstraints(Model model, List<IntegerVariable> inputVars, IntegerVariable outputVar, int[] inputs, int output) {
		for (int i = 0; i < inputVars.size(); i++) {
			Constraint input = Choco.eq(inputVars.get(i), inputs[i]);
			model.addConstraint(input);
		}
		Constraint o = Choco.eq(outputVar, output);
		model.addConstraint(o);
	}
	
	private ArrayList<Type> extractTypes(List<Component> components) {
		ArrayList<Type> types = new ArrayList<Type>();
		for (int i = 0; i < varsTable.size(); i++) {
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
	
	private Constraint getInputConstraints() {
		Constraint constraint = Choco.FALSE;
		for(StructDefine.Function function : group) {
			Constraint specConstraint = getSpecInputConstraints(function);
			constraint = Choco.or(constraint, specConstraint);
		}
		return constraint;
	}
	
	private Constraint getSpecInputConstraints(StructDefine.Function function) {
		Constraint constraint = Choco.TRUE;
		String func = function.getFunc();
		for(int i = 0 ; i < varsTable.size() ; i++) {
			if(!func.contains("i" + i)) {
				constraint = Choco.and(constraint, Choco.eq(varsTable.get(i), 0));
			}
		}
		return constraint;
	}
	
	private IOPair generateIOPair(ArrayList<Integer> newInputs) {
		System.out.println("in---generateIOPair");
		StructDefine.Function suitFunction = null;
		for(StructDefine.Function function : group) {
			boolean suit = true;
			for(int i = 0 ; i < varsTable.size() ; i++) {
				if(newInputs.get(i) !=0 && !function.getFunc().contains("i"+i))
					suit = false;
			}
			if(suit) {
				suitFunction = function;
				break;
			}
		}
		if(suitFunction == null) return null;
		
		String func = suitFunction.getFunc();
		for(int i = 0 ; i < varsTable.size() ; i++) {
			func = func.replaceAll("i"+i, newInputs.get(i)+"");
		}
		
		Calculate calculate = new Calculate();
		System.out.println("func:" + func);
		ArrayList<String> result = calculate.getStringList(func);
		result = calculate.getPostOrder(result); 
		int output = calculate.calculate(result); 
		
		int []input = new int[varsTable.size()];
		for(int i = 0 ; i < varsTable.size() ; i++)
			input[i] = newInputs.get(i); 
		
		IOPair ioPair = new IOPair(input, output);
		
		System.out.println("out---generateIOPair");
		return ioPair;
	}

	private Result resolveResult(Solver solver, List<IntegerVariable> locs, List<IntegerVariable> constantVars) {
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
