package ui;

import javax.swing.table.AbstractTableModel;

public class MyTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private String[] columnNames = {"", "Upper left", "Lower right"};
	private Object[][] data = new Object[500][3];
	private int count = 0;
	
	public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
    	return count;
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
    	return data[row][col];
    }
    
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public boolean isCellEditable(int row, int col) {
    	if(col == 0) return true;
    	return false;
    }

    public void setValueAt(Object value, int row, int col) {
        data[row][col] = value;
        fireTableCellUpdated(row, col);
    }

    public void addValue(Object[] da) {
    	data[count][0] = da[0];
    	data[count][1] = da[1];
    	data[count][2] = da[2];
    	count++;
    }
    
    public void removeItem(int index) {
    	for(int i = index ; i < count ; i++) {
    		data[i][0] = data[i+1][0];
    		data[i][1] = data[i+1][1];
    		data[i][2] = data[i+1][2];
    	}
    	count--;
    }
    
    public void alterValue(int index, Object[] newValue) {
    	data[index][0] = newValue[0];
    	data[index][1] = newValue[1];
    	data[index][2] = newValue[2];
    }
    
    public void removeAll() {
    	for(int i = 0 ; i < count ; i++) {
    		data[i][0] = "";
    		data[i][1] = "";
    		data[i][2] = "";
    	}
    	count = 0;
    }
}