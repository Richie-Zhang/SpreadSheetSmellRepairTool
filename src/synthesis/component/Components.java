package synthesis.component;

import java.util.ArrayList;
import java.util.List;

public class Components {
	public final static int PLUS = 1;
	public final static int MINUS =2;
	public final static int MULT = 3;
	public final static int CONSTANT = 4;
	public final static int SUM = 5;
	
	public static List<Component> getComponents(List<Component> comps) {
		List<Component> newComps = new ArrayList<Component>();
		for (Component comp : comps) {
			if (comp.id == PLUS) {
				newComps.add(new PlusComponent());
			} else if (comp.id == MINUS){
				newComps.add(new MinusComponent());
			} else if (comp.id == MULT) {
				newComps.add(new MultComponent());
			} else if (comp.id == CONSTANT) {
				newComps.add(new ConstantComponent());
			} else if (comp.id == SUM) {
				newComps.add(new SumComponent(((SumComponent)comp).getNum()));
			}
		}
		return newComps;
	}
	
	public static Component getComponent(int id) {
		if (id == PLUS) {
			return new PlusComponent();
		} else if (id == MINUS){
			return new MinusComponent();
		} else if (id == MULT) {
			return new MultComponent();
		} else if (id == CONSTANT) {
			return new ConstantComponent();
		} 
		
		return null;
	}
}
