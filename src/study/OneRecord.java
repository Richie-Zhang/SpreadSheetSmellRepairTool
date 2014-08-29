package study;

import java.util.ArrayList;

import core.StructDefine;

public class OneRecord {
	public static class CellArray {
		StructDefine.Region cellArray;
		String sheetName;
		int sheetNo;
		
		public CellArray(int sno, String sname, StructDefine.Region ca) {
			sheetNo = sno;
			sheetName = sname;
			cellArray = ca;
		}
	}
	
	String folder;
	String filename;
	boolean canOpenByPOI;
	int numOfTables;
	boolean withFormula;
	int numOfFormulaTables;
	int numOfCellArrays;
	int numOfSmellCellArrays;
	ArrayList<CellArray> cellArraysWithSmell;
	
	public OneRecord(String f, String fn) {
		folder = f;
		filename = fn;
		canOpenByPOI = false;
		withFormula = false;
		numOfTables = 0;
		numOfFormulaTables = 0;
		numOfCellArrays = 0;
		numOfSmellCellArrays = 0;
		cellArraysWithSmell = new ArrayList<>();
	}
	
	public String get_folder() {
		return folder;
	}
	
	public String get_filename() {
		return filename;
	}
	
	public void set_canOpenbyPOI(boolean cop) {
		canOpenByPOI = cop;
	}
	
	public boolean get_canOpenbyPOI() {
		return canOpenByPOI;
	}
	
	public void set_withFormula(boolean wf) {
		withFormula = wf;
	}
	
	public boolean get_withFormula() {
		return withFormula;
	}
	
	public void set_numOfTables(int nft) {
		numOfTables = nft;
	}
	
	public int get_numOfTables() {
		return numOfTables;
	}
	
	public void set_numOfFormulaTables(int noft) {
		numOfFormulaTables = noft;
	}
	
	public int get_numOfFormulaTables() {
		return numOfFormulaTables;
	}
	
	public void set_numOfCellArrays(int noca) {
		numOfCellArrays = noca;
	}
	
	public int get_numOfCellArrays() {
		return numOfCellArrays;
	}
	
	/*public void set_numOfSmellCellArrays(int nosca) {
		numOfsmellCellArrays = nosca;
	}*/
	
	public int get_numOfSmellCellArrays() {
		return numOfSmellCellArrays;
	}
	
	public void add_cellArraysWithSmell(CellArray caws) {
		cellArraysWithSmell.add(caws);
		numOfSmellCellArrays++;
	}
}