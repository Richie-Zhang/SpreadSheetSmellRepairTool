package file;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import core.StructDefine;

public class SheetReader {
	private String sheetName;
	private int rowCount, columnCount;	//最大行数/列数
	private boolean overFlow;	//是否超过500*26的大小
	private StructDefine.Cell[][] cells;
	
	public SheetReader(Sheet sheet) {
		this.sheetName = sheet.getSheetName();
		this.rowCount = 0;
		this.columnCount = 0;
		this.cells = new StructDefine.Cell[500][26];
		
		for(int i = 0 ; i < 500 ; i++)
			for(int j = 0; j < 26 ; j++){
				this.cells[i][j] = new StructDefine.Cell();
			}
		
		this.rowCount = sheet.getLastRowNum()+1;
		this.overFlow = false;
		if(this.rowCount > 500){
			this.overFlow = true;
			this.rowCount = 500;
		}
		for (int i = 0; i < this.rowCount; i++) {
			Row row = sheet.getRow(i);
			if(row == null) continue;
			if(this.columnCount < row.getLastCellNum())
				this.columnCount = row.getLastCellNum();
			if(this.columnCount > 26){
				this.columnCount = 26;
				this.overFlow = true;
			}
			for (int j = 0; j < ((row.getLastCellNum()>26)?26:row.getLastCellNum()); j++) {
				Cell cell = row.getCell(j);
				if (cell == null) continue;
				this.cells[i][j].setCellType(cell.getCellType()); 
				switch (cell.getCellType()) {
					case Cell.CELL_TYPE_NUMERIC:
						this.cells[i][j].setValue(cell.getNumericCellValue() + "");
						this.cells[i][j].setValueType(0);
						break;
					case Cell.CELL_TYPE_STRING:
						this.cells[i][j].setValue(cell.getStringCellValue());
						if(cell.getStringCellValue().length() > 0)
							this.cells[i][j].setValueType(1);
						break;
					case Cell.CELL_TYPE_BOOLEAN:
						this.cells[i][j].setValue(cell.getBooleanCellValue() ? "true" : "false");
						this.cells[i][j].setValueType(2);
						break;
					case Cell.CELL_TYPE_FORMULA:
						this.cells[i][j].setFormula(cell.getCellFormula());
						try{
							cell.getNumericCellValue();
							this.cells[i][j].setValueType(0);
							this.cells[i][j].setValue(cell.getNumericCellValue() + "");
						} catch(IllegalStateException e1){
							try{
								cell.getStringCellValue();
								this.cells[i][j].setValueType(1);
							} catch(IllegalStateException e2){
								try{
									cell.getBooleanCellValue();
									this.cells[i][j].setValueType(2);
								} catch(IllegalStateException e3){
									
								}
							}
						}
						break;
					case Cell.CELL_TYPE_BLANK:
						break;
					case Cell.CELL_TYPE_ERROR:
						break;
				} 
			}
		}
	}

	public String getSheetName() {
		return this.sheetName;
	}
	
	public int getRowCount() {
		return this.rowCount;
	}
	
	public int getColumnCount() {
		return this.columnCount;
	}
	
	public boolean getOverFlow() {
		return overFlow;
	}
	
	public StructDefine.Cell[][] getCells() {
		return this.cells;
	}
	
	public void setCellValueAt(double d, int i, int j) {
		cells[i][j].setValue(d+"");
	}
	
	public void setCellFormulaAt(String formula, int i, int j) {
		cells[i][j].setFormula(formula);
	}
	
	public double getCellValueAt(StructDefine.Position pos) {
		if(cells[pos.GetRow()][pos.GetColumn()].getValueType() == 0)
			return Double.parseDouble(cells[pos.GetRow()][pos.GetColumn()].getValue());
		else
			return 0;
	}
}
