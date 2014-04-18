package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;

import parser.ConvertFormula;
import parser.ExpParser;
import parser.FormulaParser;
import synthesis.IOPair;
import synthesis.SpecSynthesis;
import synthesis.component.*;
import choco.Choco;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.kernel.model.Model;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;
import file.SheetReader;

public class DetectRepairSmell {
	private SheetReader sheetReader;
	private StructDefine.Region cellArray;
	private ArrayList<StructDefine.Position> CA;
	private ArrayList<Entry<Formula, Integer>> SPEC;
	private ArrayList<StructDefine.R1C1Relative> IV;
	private ArrayList<StructDefine.Function> FUNC;
	private ArrayList<IOPair> IO;
	private ArrayList<IntegerVariable> varsTable;
	private StructDefine.SmellAndRepair smellAndRepair;
	private Formula repairFormula;
	
	private int state;	//-1-无法合成，1-已有公式，2-合成成功, 3-已修复
	
	public DetectRepairSmell(SheetReader sr, StructDefine.Region snip) throws Exception{
		this.sheetReader = sr;
		this.cellArray = snip;
		this.state = 0;
		this.CA = new ArrayList<StructDefine.Position>();
		this.smellAndRepair = new StructDefine.SmellAndRepair(snip);
		this.IV = new ArrayList<>();
		this.FUNC = new ArrayList<>();
		this.IO = new ArrayList<>();
		initCA(snip);
		initSPEC();
		initFUNC();
		initIO();
	}
	
	public boolean hasSmell() {
		if(FUNC.size() > 1) return true;
	    Formula lastFormula = null;
	    for(StructDefine.Position position : CA) {
    		if(sheetReader.getCells()[position.GetRow()][position.GetColumn()].getCellType() != Cell.CELL_TYPE_FORMULA)
    			return true;
    		else{
    			Formula formula = new Formula(position, sheetReader.getCells()[position.GetRow()][position.GetColumn()].getFormula());
    			if(lastFormula == null)
    				lastFormula = formula;
    			else {
    				if(lastFormula.getR1C1Formula().equals(formula.getR1C1Formula()))
    					continue;
    				if(!FormulaParser.isEquivalent(formula, lastFormula, IV))
    					return true;
    			}
    		}
    	}
    	return false;
	}
	
	public boolean recovery() {
		if(state == 3) return true;
		try {
			repairFormula = recoveryFormulaPattern();
			if(repairFormula != null) {
				state = 1;
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean synthesize() {
		if(state == 3) return true;
		try {
			repairFormula = synthesizeFormulaPattern();
			if(repairFormula != null) {
				state = 2;
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		state = -1;
		return false;
	}

	public void repairSmell() throws Exception {
		if(state == 3 || repairFormula == null) return;
		
		int startRow = cellArray.GetTopLeft().GetRow(), endRow = cellArray.GetBottomRight().GetRow();
    	int startColumn = cellArray.GetTopLeft().GetColumn(), endColumn = cellArray.GetBottomRight().GetColumn();
    	for(int i = startRow ; i <= endRow ; i++)
    		for(int j = startColumn ; j <= endColumn ; j++) {
    			String advise = repairFormula.getA1Formula(new StructDefine.Position(i, j));
    			ExpParser expParser = new ExpParser(sheetReader, new StructDefine.Position(i, j));
    			double value = expParser.evaluate(repairFormula.getR1C1Formula());	
    			if(!advise.equals(sheetReader.getCells()[i][j].getFormula())) {
    				StructDefine.RepairAdvise ra = new StructDefine.RepairAdvise();
    				ra.SetFormula(advise);
    				ra.SetValue(value);
    				if((Double.parseDouble(sheetReader.getCells()[i][j].getValue()) - value > 0.000001) || (Double.parseDouble(sheetReader.getCells()[i][j].getValue()) - value < -0.000001))
    					ra.SetType(1);	//值错误
    				else 
    					ra.SetType(2);	//
    				smellAndRepair.AddRepairAdvise(new StructDefine.Position(i,j), ra);
    			}
    		}
    	state = 3;
	}
	
	public int getState() {
		return state;
	}
	
	public StructDefine.SmellAndRepair getSmellAndRepair() {
		return smellAndRepair;
	}

	public StructDefine.Region getCellArray() {
		return cellArray;
	}
	
	private double getCellValue(StructDefine.Position position) {
		return sheetReader.getCellValueAt(position);
	}
	
	private double getCellValue(StructDefine.Position position, StructDefine.R1C1Relative r1c1Relative) {
		StructDefine.Position thisPosition = r1c1Relative.GetPosition(position);
		return sheetReader.getCellValueAt(thisPosition);
	}
	
	private void initCA(StructDefine.Region snip) {
		int rowStart = snip.GetTopLeft().GetRow(), rowEnd = snip.GetBottomRight().GetRow();
	    int columnStart = snip.GetTopLeft().GetColumn(), columnEnd = snip.GetBottomRight().GetColumn();
	    for(int i = rowStart ; i <= rowEnd ; i++)
	    	for(int j = columnStart ; j <= columnEnd ; j++)
	    		CA.add(new StructDefine.Position(i, j));
	}
	
	private void initSPEC() {
		Map<Formula, Integer> SPEC_Map = new HashMap<>();
		for(StructDefine.Position position : CA){
			StructDefine.Cell cell = sheetReader.getCells()[position.GetRow()][position.GetColumn()];
			if(cell.getCellType() == Cell.CELL_TYPE_FORMULA){
				Formula formula = new Formula(position, cell.getFormula());
				Iterator<Entry<Formula, Integer>> iter = SPEC_Map.entrySet().iterator();
				boolean existed = false;
				while (iter.hasNext()) {
					Entry<Formula, Integer> entry = iter.next();
					Formula key = entry.getKey();
					if(key.getR1C1Formula().equals(formula.getR1C1Formula())){
						entry.setValue(entry.getValue()+1);
						existed = true;
						break;
					}
				}
				if(!existed) SPEC_Map.put(formula, 1);

				ArrayList<StructDefine.R1C1Relative> input = formula.GetInputSet();
				for(StructDefine.R1C1Relative in : input){
					boolean contain = false;
					for(StructDefine.R1C1Relative tRelative : IV) {
						if(tRelative.equal(in)){
							contain = true;
							break;
						}
					}
					if(!contain) {
						IV.add(in);
					}
				}
			}	
		}
		Collections.sort(IV);
		
		this.SPEC = new ArrayList<>(SPEC_Map.entrySet());  
		Collections.sort(SPEC, new Comparator<Object>(){   
			@SuppressWarnings("unchecked")
			public int compare(Object e1, Object e2){   
				int v1 = ((Entry<Formula,Integer>) e1).getValue(); 
				int v2 = ((Entry<Formula,Integer>) e2).getValue();   
		        return v2-v1;        
		    }
		});
		
		this.varsTable = new ArrayList<>();
		for(int i = 0 ; i < IV.size() ; i++) {
			varsTable.add(Choco.makeIntVar("i"+i));
		}
	}
	
	private void initFUNC() {
		for(Entry<Formula, Integer> entry : SPEC){
			Formula formula = entry.getKey();
			String func = FormulaParser.preProcessFormula(formula.getR1C1Formula(), IV);
			if(FormulaParser.validFormula(func)) {
				boolean existed = false;
				for(StructDefine.Function function : FUNC) {
					String s = function.getFunc();
					if(FormulaParser.isEquivalent(func, s, IV)) existed = true;
				}
				if(!existed) {
					StructDefine.Function newFunction = new StructDefine.Function(formula);
					newFunction.setFunc(func);
					FUNC.add(newFunction);
				}
			}
		}
	}
	
	private void initIO() {
		for(StructDefine.Position position : CA) {
			boolean valid = true;
			int []inputs = new int[IV.size()];
			for(int i = 0 ; i < IV.size() ; i++) {
				StructDefine.R1C1Relative r1c1 = IV.get(i);
				double value = getCellValue(position, r1c1);
				if((int)value - value > 0.000001 || (int)value - value < -0.000001) valid = false;;
				inputs[i] = (int)value;
			}
			double output = getCellValue(position);
			if((int)output - output > 0.000001 || (int)output - output < -0.000001) valid = false;
			if(valid) IO.add(new IOPair(inputs, (int)output));
		}
	}
	
	private Formula recoveryFormulaPattern() throws Exception {
		if(SPEC.size() == 1) return SPEC.get(0).getKey();
		for(Entry<Formula, Integer> entry : SPEC){
			Formula form = entry.getKey();
			//if(form.GetInputSet().size() == IV.size()) {
				if(coverage(form) == CA.size()) {
					return form;
				}
			//}
		}
		return null;
	}
	
	private Formula synthesizeFormulaPattern() throws Exception {
		System.out.println("in---synthesizeFormulaPattern");
		ArrayList<ArrayList<StructDefine.Function>> groups = classify();
		System.out.println(groups.size());
		int pert = 0;
		StructDefine.Function F = null;
		for(ArrayList<StructDefine.Function> group : groups) {
			SpecSynthesis specSynthesis = new SpecSynthesis(group, varsTable);
			ArrayList<Component> comps = getComponents();
			ArrayList<IOPair> ioPairs = specSynthesis.doSpecSynthesis(comps);
			String[] progs = specSynthesis.doIOSynthesis(ioPairs, IO, comps);
			
			StructDefine.Function function = null;
			if (progs == null) {
				System.out.println("No solution! Components insufficient!");
			} else {
				String program = mergeProgs(progs);
		    	String A1Formula = convertToFormula(program, CA.get(0));
		    	Formula formula = new Formula(CA.get(0), A1Formula);
		    	function = new StructDefine.Function(formula);
		    	function.setFunc(program);
			}
			
			if(function == null) continue;
			int coverage = coverage(function.getFormula());
			if(coverage > pert) {
				pert = coverage;
				F = function;
			}
		}
		System.out.println("out---synthesizeFormulaPattern");
		if(F != null)
			return F.getFormula();
		else
			return null;
	}
	
	private ArrayList<ArrayList<StructDefine.Function>> classify() {
		ArrayList<ArrayList<StructDefine.Function>> groups = new ArrayList<>();
		if(FUNC.size() == 0) return groups;
		int index = 0;
		while(index != -1) {
			ArrayList<StructDefine.Function> newGroup = new ArrayList<>();
			newGroup.add(FUNC.get(index));
			FUNC.get(index).setClassified();
			for(StructDefine.Function func : FUNC){
				boolean existed = false, compatible = true;
				for(StructDefine.Function function : newGroup) {
					if(func == function) existed = true;
					if(!isCompatible(function, func)) compatible = false;
				}
				if(existed || !compatible) continue;
				newGroup.add(func);
				func.setClassified();
			}
			groups.add(newGroup);
			
			index = -1;
			for(int i = 0 ; i < FUNC.size() ; i++) {
				if(!FUNC.get(i).classified()){
					index = i; break;
				}
			}
		}
		return groups;
	}
	
	private int coverage(Formula formula) throws Exception{
		int coveredCells = 0;
		for(StructDefine.Position position : CA) {
			StructDefine.Cell cell = sheetReader.getCells()[position.GetRow()][position.GetColumn()];
			if(cell.getValueType() == 0){
				double cellValue = Double.parseDouble(cell.getValue());
				ExpParser eParser = new ExpParser(sheetReader, position);
				double formulaValue = eParser.evaluate(formula.getR1C1Formula());
				if(cellValue - formulaValue < 0.000001 && cellValue - formulaValue > -0.000001)
					coveredCells++;
			}
		}
		return coveredCells;
	}
	
	private boolean isCompatible(StructDefine.Function func1, StructDefine.Function func2) {
		ConvertFormula convertFormula = new ConvertFormula();
		IntegerExpressionVariable iev1 = convertFormula.convertFormula(func1.getFunc(), varsTable);
		IntegerExpressionVariable iev2 = convertFormula.convertFormula(func2.getFunc(), varsTable);
		
		Constraint cons = Choco.neq(iev1, iev2);
		
		Model model = new CPModel();
		Solver solver = new CPSolver();
		
		model.addConstraint(cons);
		
		for(int i = 0 ; i < IV.size() ; i++) {
			if(!func1.getFunc().contains("i"+i) || !func2.getFunc().contains("i"+i))
				model.addConstraint(Choco.eq(0, varsTable.get(i)));
		}
		
		solver.read(model);
		solver.setTimeLimit(180000);
		solver.solve();
		
		if (solver.getSolutionCount() > 0) {
			return false;
		} else {
			return true;
		}
	}
	
	private ArrayList<Component> getComponents() {
		ArrayList<Integer> constants = new ArrayList<>(); 
		boolean []exist = new boolean[5];
		for(StructDefine.Function function : FUNC) {
			String func = function.getFunc();
			if(func.contains("+")) exist[0] = true;
			if(func.contains("-")) exist[1] = true;
			if(func.contains("*")) exist[2] = true;
			ArrayList<Integer> consts = getConstantNum(func);
			for(Integer integer : consts) {
				if(!constants.contains(integer))
					constants.add(integer);
			}
		}
		ArrayList<Component> comps = new ArrayList<Component>();
		for(int i = 0 ; i < constants.size() ; i++)
			comps.add(new ConstantComponent());
		for(int i = 1 ; i < IV.size() + constants.size() ; i++) {
			if(exist[0]) {comps.add(new PlusComponent());}
			if(exist[1]) {comps.add(new MinusComponent());}
			if(exist[2]) {comps.add(new MultComponent());}
		}
		return comps;
	}
	
	private ArrayList<Integer> getConstantNum(String func) {
		ArrayList<Integer> constants = new ArrayList<>();
		String temp = func;
		for(int i = 0 ; i < IV.size() ; i++) 
			temp = temp.replaceAll("i"+i, "");
		int index = 0, length = temp.length();
		while(index < length) {
			int value = 0;
			while(index < length && !(temp.charAt(index) > '0' && temp.charAt(index) < '9'))
				index++;
			while(index < length && (temp.charAt(index) > '0' && temp.charAt(index) < '9')) {
				value = 10*value + (int)(temp.charAt(index)-'0');
				index++;
			}
			constants.add(value);
		}
		return constants;
	}
	
	private String mergeProgs(String[] progs) {
		int lineCount = progs.length;
		String program = null;
		int index = lineCount-1;
		for( ; index >= 0 ; index--) {
			if(progs[index].startsWith("ret")) {
				program = progs[index].substring(4);
				break;
			}
		}
		while(index > 0) {
			index--;
			String left = progs[index].split(" ")[0];
			String right = progs[index].split(" ")[2];
			program = program.replaceAll(left, right);
		}
		return program;
	}
	
	private String convertToFormula(String program, StructDefine.Position position) {
		String formula = program;
		for(int i = 0 ; i < IV.size() ; i++) {
			String si = "i" + i;
			StructDefine.Position thisPosition = IV.get(i).GetPosition(position);
			char c = (char) ('A' + thisPosition.GetColumn());
			String sip = c + "" + (thisPosition.GetRow() + 1);
			formula = formula.replaceAll(si, sip);
		}
		return formula;
	}
}
