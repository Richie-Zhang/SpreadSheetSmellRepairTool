package synthesis.basic;

import java.util.ArrayList;
import java.util.List;

import synthesis.component.Component;
import synthesis.component.Components;

import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Z3Exception;

public class ProgramAbstract {

	private static int programId = 1;

	public List<Component> components = null;

	public List<Type> inputTypes = null;
	
	public Type outputType = null;

	public List<Type> types = new ArrayList<Type>();

	public List<IntExpr> locVars = new ArrayList<IntExpr>();

	public List<IntExpr> constVars = new ArrayList<IntExpr>();

	public ProgramAbstract(List<Component> components, List<Type> inputTypes, Type outputType) {
		this.components = components;
		this.inputTypes = inputTypes;
		this.outputType = outputType;

		programId++;
	}

	public ProgramAbstract(List<Component> comps, int size) {
		// TODO Auto-generated constructor stub
		List<Type> inputTypes = new ArrayList<>();
  		for(int i = 0 ; i < size ; i++) {
  			inputTypes.add(new Type(IOType.FUN_INPUT, VarType.INTEGER));
  		}
  		
  		Type outputType = new Type(IOType.FUN_OUTPUT, VarType.INTEGER);
  		
  		this.components = comps;
  		this.inputTypes = inputTypes;
  		this.outputType = outputType;
  		
  		programId++;
	}

	public ProgramInstance getInstance() {
		return new ProgramInstance(this);
	}

	public int getSize() {
		return inputTypes.size() + components.size();
	}

	public void init() throws Z3Exception {
		// sort the components, make the constants in the first, and init the
		// components
		List<Component> newComps = Components.getComponents(components);
		components.clear();

		for (Component comp : newComps) {
			if (comp.getType() == Components.CONSTANT) {
				components.add(comp);
			}
		}
		for (Component comp : newComps) {
			if (comp.getType() != Components.CONSTANT) {
				components.add(comp);
			}
		}

		// init the types
		types = extractTypes();

		// init the loc variables
		for (int i = 0; i < types.size(); i++) {
			IntExpr locVar = BasicSynthesis.ctx.mkIntConst("loc_" + programId
					+ "_" + i);
			locVars.add(locVar);
		}

		// init the constant variables
		int constIndex = 0;
		for (Component comp : components) {
			if (comp.getType() == Components.CONSTANT) {
				IntExpr constVar = BasicSynthesis.ctx.mkIntConst("const_"
						+ constIndex);
				constVars.add(constVar);
				constIndex++;
			}
		}
	}

	private List<Type> extractTypes() {

		for (Type type : inputTypes) {
			type.ioType = IOType.FUN_INPUT;
			types.add(type);
		}

		// function output
		if (this.outputType == null) {
			outputType = new Type(IOType.FUN_OUTPUT, VarType.INTEGER);
		}
		types.add(outputType);

		// all the components
		for (Component comp : components) {
			types.addAll(comp.getVarTypes());
		}

		return types;
	}

}
