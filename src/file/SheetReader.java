package file;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import core.StructDefine;

public class SheetReader {
	private String sheetName;
	private int rowCount, columnCount;	//最大行数/列数
	private boolean valid;
	private boolean hasFormula;
	private StructDefine.Cell[][] cells;
	
	public SheetReader(Sheet sheet) {
		this.sheetName = sheet.getSheetName();
		this.rowCount = sheet.getLastRowNum()+1;
		this.hasFormula = false;
		this.valid = true;
		
		for(int i = 0 ; i < this.rowCount; i++) {
			Row row = sheet.getRow(i);
			if(row == null) continue;
			if(this.columnCount < row.getLastCellNum())
				this.columnCount = row.getLastCellNum();
		}
		this.cells = new StructDefine.Cell[this.rowCount][this.columnCount];
		
		for(int i = 0 ; i < this.rowCount ; i++)
			for(int j = 0; j < this.columnCount ; j++){
				this.cells[i][j] = new StructDefine.Cell();
			}

		for(int i = 0 ; i < this.rowCount; i++) {
			Row row = sheet.getRow(i);
			if(row == null) continue;
			for(int j = 0 ; j < row.getLastCellNum() ; j++) {
				Cell cell = row.getCell(j);
				if (cell == null) continue;
				if(cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
					try{
						cell.getNumericCellValue();
					} catch(IllegalStateException e1){
						continue;
					}
					this.hasFormula = true;
					break;
				}
			}
			if(this.hasFormula) break;
		}
		
		if(!this.hasFormula) return;

		try{
		for (int i = 0; i < this.rowCount; i++) {
			Row row = sheet.getRow(i);
			if(row == null) continue;
			for (int j = 0; j < row.getLastCellNum(); j++) {
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
						
						if(cell.getCellFormula().contains("$") || cell.getCellFormula().contains("IF") || cell.getCellFormula().contains("!"))
							this.cells[i][j].setCellType(Cell.CELL_TYPE_STRING); 
						break;
					case Cell.CELL_TYPE_BLANK:
						break;
					case Cell.CELL_TYPE_ERROR:
						break;
				} 
			}
		}
		}catch(Exception e) {
			this.valid = false;
			return;
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
	
	public boolean getValid() {
		return valid;
	}
	
	public boolean getHasFormula() {
		return hasFormula;
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
		if(pos.GetRow() < 0 || pos.GetColumn() < 0 || pos.GetRow() >= this.rowCount || pos.GetColumn() >= this.columnCount)
			return 0;
		if(cells[pos.GetRow()][pos.GetColumn()].getValueType() == 0)
			return Double.parseDouble(cells[pos.GetRow()][pos.GetColumn()].getValue());
		return 0;
	}
}
