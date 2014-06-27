package synthesis.basic;

import java.util.List;

import synthesis.component.Component;
import synthesis.component.Components;

public class Program {

	private Result res;
	private ProgramAbstract pa;

	public Program(Result res, ProgramAbstract pa) {
		this.res = res;
		this.pa = pa;
	}

	public Line[] tranform() {
		List<Integer> ls = res.locs;
		List<Integer> cs = res.cons;

		int sizeOfProgram = pa.getSize();

		int compId = 0;
		int consId = 0;
		Line[] progs = new Line[sizeOfProgram + 1];
		for (int i = 0; i < ls.size();) {
			Type type = pa.types.get(i);
			if (type.ioType == IOType.FUN_INPUT) {
				Line line = new Line(LineType.INPUT, null, new Integer[] { i });
				progs[i] = line;
				i++;
			} else if (type.ioType == IOType.FUN_OUTPUT) {
				Line line = new Line(LineType.OUTPUT, null,
						new Integer[] { ls.get(i) });
				progs[sizeOfProgram] = line;
				i++;
			} else { // components
				Component comp = pa.components.get(compId);
				compId++;

				if (comp.getType() == Components.CONSTANT) {
					Integer lineNum = ls.get(i);
					Line line = new Line(LineType.COMPONENT, comp,
							new Integer[] { cs.get(consId++) });
					progs[lineNum] = line;
					
					i++;
				} else {
					int k = i;
					for (; k < ls.size(); k++) {
						if (pa.types.get(k).ioType == IOType.COMP_OUTPUT) {
							break;
						}
					}
					List<Integer> inputs = ls.subList(i, k);
					Integer lineNum = ls.get(k);
					Line line = new Line(LineType.COMPONENT, comp,
							inputs.toArray(new Integer[0]));
					progs[lineNum] = line;
					
					i = k + 1;
				}
			}
		}

		return progs;
	}

	public enum LineType {
		INPUT, OUTPUT, COMPONENT;
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

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(type);
			sb.append(": ");
			if (comp.getType() == Components.CONSTANT) {
				sb.append(inputs[0].toString());
			} else {
				String[] strs = new String[inputs.length];
				for (int i = 0; i < inputs.length; i++) {
					strs[i] = inputs[i].toString();
				}
				sb.append(comp.getProg(strs));
			}
			return sb.toString();
		}
	}
}
