package synthesis;

import java.util.List;

import synthesis.component.Component;
import synthesis.component.Components;

public class Program {

	private Result res;
	private List<Type> types;
	private List<Component> comps;
	private int numOfInput;

	public Program(Result res, List<Type> types, List<Component> comps,
			int numOfInput) {
		this.res = res;
		this.types = types;
		this.comps = comps;
		this.numOfInput = numOfInput;
	}

	public Line[] tranform() {
		List<Integer> ls = res.locs;
		List<Integer> cs = res.cons;

		List<Component> components = Components.getComponents(comps);
		int sizeOfProgram = components.size() + numOfInput;

		int compId = 0;
		int consId = 0;
		Line[] progs = new Line[sizeOfProgram + 1];
		for (int i = 0; i < ls.size();) {
			Type type = types.get(i);
			if (type == Type.FUN_INPUT) {
				Line line = new Line(LineType.INPUT, null, new Integer[] { i });
				progs[i] = line;
				i++;
			} else if (type == Type.FUN_OUTPUT) {
				Line line = new Line(LineType.OUTPUT, null,
						new Integer[] { ls.get(i) });
				progs[sizeOfProgram] = line;
				i++;
			} else if (type == Type.COMP_CONSTANT) {
				Integer lineNum = ls.get(i);
				Line line = new Line(LineType.CONSTANT, null,
						new Integer[] { cs.get(consId++) });
				progs[lineNum] = line;
				compId++;
				i++;
			} else {
				int k = i;
				for (; k < ls.size(); k++) {
					if (types.get(k) == Type.COMP_OUTPUT) {
						break;
					}
				}
				List<Integer> inputs = ls.subList(i, k);
				Integer lineNum = ls.get(k);
				Line line = new Line(LineType.COMPONENT,
						components.get(compId++),
						inputs.toArray(new Integer[0]));
				progs[lineNum] = line;

				i = k + 1;
			}
		}

		return progs;
	}

	public enum LineType {
		INPUT, OUTPUT, CONSTANT, COMPONENT;
	}

	public class Line {

		public LineType type;
		public Component comp;
		public Integer[] inputs;

		public Line(LineType type, Component comp, Integer[] inputs) {
			this.type = type;
			this.comp = comp;
			this.inputs = inputs;
		}
	}
}
