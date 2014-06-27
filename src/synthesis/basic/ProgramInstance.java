package synthesis.basic;

import java.util.ArrayList;
import java.util.List;

import synthesis.component.Component;
import synthesis.component.Components;
import synthesis.util.Z3Util;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

public class ProgramInstance {

	private static int intanceId = 1;

	public ProgramAbstract program = null;

	public List<Component> components = null;

	public List<Expr> inputVars = new ArrayList<Expr>();

	public Expr outputVar = null;

	public List<Expr> allVars = new ArrayList<Expr>();

	public ProgramInstance(ProgramAbstract pa) {
		program = pa;

		intanceId++;
	}

	public void init() throws Z3Exception {
		// init all the variables
		// function inputs
		for (int i = 0; i < program.inputTypes.size(); i++) {
			Type type = program.inputTypes.get(i);

			Expr var = Z3Util.getVariable(type, BasicSynthesis.ctx, "input_"
					+ intanceId + "_" + i);

			allVars.add(var);

			inputVars.add(var);
		}

		// function output
		outputVar = Z3Util.getVariable(program.outputType, BasicSynthesis.ctx,
				"result_" + intanceId);
		allVars.add(outputVar);

		// all the constant variables
		allVars.addAll(program.constVars);

		// all the other components
		components = Components.getComponents(program.components);
		for (Component comp : components) {
			if (comp.getType() != Components.CONSTANT) {
				allVars.addAll(comp.getVariables());
			}
		}

	}
}
