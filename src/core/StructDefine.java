package core;

import java.util.ArrayList;

public class StructDefine {
	public static class Cell {
		private int valueType; //-1-空， 0-数值， 1-字符串， 2-bool
    	private int cellType;
    	private String value;
    	private String formula;
    	
    	public Cell() {
    		valueType = -1;
    		cellType = -1;
    		value = null;
    		formula = null;
    	}
    	
    	public void setValueType(int valuetype) {
    		this.valueType = valuetype;
    	}
    	
    	public int getValueType() {
    		return this.valueType;
    	}
    	
    	public void setCellType(int celltype) {
    		this.cellType = celltype;
    	}
    	
    	public int getCellType() {
    		return this.cellType;
    	}
    	
    	public void setValue(String value) {
    		this.value = value;
    	}
    	
    	public String getValue() {
    		return this.value;
    	}
    	
    	public void setFormula(String formula) {
    		this.cellType = org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA;
    		this.formula = formula;
    	}
    	
    	public String getFormula() {
    		return this.formula;
    	}
	}
	
	public static class Position {
		int row, column;

        public Position(int r, int c) {
            row = r;
            column = c;
        }

        public static Position ConvertPosition(String pos) {
            if (pos == null || pos.length() < 2) {
                return null;
            }
            if (!(pos.charAt(0) >= 'A' && pos.charAt(0) <= 'Z')) {
                return null;
            }
            int c = pos.charAt(0) - 'A';
            int r = 0;
            for (int i = 1; i < pos.length(); i++) {
                char temp = pos.charAt(i);
                if (!(temp >= '0' && temp <= '9')) {
                    return null;
                }
                r = r * 10 + (temp - '0');
            }
            r--;
            return new Position(r, c);
        }

        public int GetRow() {
            return row;
        }

        public int GetColumn() {
            return column;
        }

        public void SettRow(int r) {
            row = r;
        }

        public void SetColumn(int c) {
            column = c;
        }
	}

	public static class Region {
		Position TopLeft, BottomRight;
		
        public Region(Position tl, Position br) {
            TopLeft = tl;
            BottomRight = br;
        }

        public Position GetTopLeft() {
            return TopLeft;
        }

        public Position GetBottomRight() {
            return BottomRight;
        }

        public void SetTopLeft(Position tl) {
            TopLeft = tl;
        }

        public void SetBottomRight(Position br) {
            BottomRight = br;
        }
	}

	public static class R1C1Relative implements Comparable<R1C1Relative> {
        int row, column;

        public R1C1Relative(int r, int c) {
            row = r;
            column = c;
        }

        public int GetRow() {
            return row;
        }

        public int GetColumn() {
            return column;
        }
        
        public Position GetPosition(Position current) {
        	return new Position(current.GetRow() + row, current.GetColumn() + column);
        }

		public int compareTo(R1C1Relative o) {
			if(o == null) return 0;
			if(row != o.row) return row-o.row;
			else return column-o.column;
		}
		
		public boolean equal(R1C1Relative o) {
			if(o == null) return false;
			if(row == o.row && column == o.column) return true;
			else return false;
		}
		
		public static StructDefine.R1C1Relative convertStringToR1C1Relative(String s) {
			if(!s.startsWith("<") || !s.endsWith(">"))
				return null;
			String[] items = s.split("\\[|]");
			if(items.length != 5) return null;
			int r = Integer.parseInt(items[1]);
			int c = Integer.parseInt(items[3]);
			
			return new StructDefine.R1C1Relative(r, c);
		}
    }

	public static class RepairAdvise {
    	private int smellType;	//0-no smell, 1-value error, 2-formula
    	private String formula;
    	private double value;
    	
    	public RepairAdvise() {
    		smellType = 0;
    	}
    	
    	public int GetType() {
    		return smellType;
    	}
    	
    	public void SetType(int t) {
    		smellType = t;
    	}
    	
    	public String GetFormula() {
    		return formula;
    	}
    	
    	public void SetFormula(String s) {
    		formula = s;
    	}
    	
    	public double GetValue() {
    		return value;
    	}
    	
    	public void SetValue(double val) {
    		value = val;
    	}
    }

	public static class SmellAndRepair {
    	private Region cellArray;
    	private int acceptType;	//1-应用，2-忽略，3-手动输入
    	private ArrayList<Position> positions;
    	private ArrayList<RepairAdvise> repairAdvises;
    	private ArrayList<Position> inputPositions;
    	
    	public SmellAndRepair(Region cellArray) {
    		this.cellArray = cellArray;
    		this.positions = new ArrayList<>();
    		this.repairAdvises = new ArrayList<>();
    		this.inputPositions = new ArrayList<>();
    		this.acceptType = 0;
    	}
    	
    	public void AddRepairAdvise(Position pos, RepairAdvise repairAdvise) {
    		this.positions.add(pos);
    		this.repairAdvises.add(repairAdvise);
    	}
    	
    	public Region GetCellArray() {
    		return cellArray;
    	}

    	public ArrayList<Position> GetPositions() {
    		return positions;
    	}
    	
    	public ArrayList<Position> GetInputPositions() {
    		return inputPositions;
    	}
    	
    	public void AddInputPosition(Position pos) {
    		inputPositions.add(pos);
    	}
    	
    	public ArrayList<RepairAdvise> GetRepairAdvises() {
    		return repairAdvises;
    	}
    	
    	public void SetAccepted(int b){
    		this.acceptType = b;
    	}
    	
    	public int GetAccepted() {
    		return this.acceptType;
    	}
    }

	public static class Function {
		private Formula formula;
		private String func;
		private boolean classified;
		
		public Function(Formula form) {
			this.formula = form;
			this.func = null;
			this.classified = false;
		}
		
		public Formula getFormula() {
			return this.formula;
		}
		
		public String getFunc() {
			return this.func;
		}
		
		public void setFunc(String s) {
			this.func = s;
		}
		
		public void setClassified() {
			this.classified = true;
		}
		
		public boolean classified() {
			return this.classified;
		}
	}
}
