package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import core.StructDefine;

import javax.swing.BoxLayout;

import org.apache.poi.ss.usermodel.Cell;

import file.SheetReader;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ManualUpdate extends JDialog {
	private static final long serialVersionUID = 1L;
	
	private SheetReader sheetReader;
	private ArrayList<JTextField> textFields = new ArrayList<>();
	private ArrayList<String> formulas = new ArrayList<>();

	public ManualUpdate(SheetReader sheetReader, StructDefine.Region cellArray) {
		this.sheetReader = sheetReader;
		getContentPane().setLayout(new BorderLayout());
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		
		int count = 0;
		for(int i = cellArray.GetTopLeft().GetRow() ; i <= cellArray.GetBottomRight().GetRow() ; i++)
			for(int j = cellArray.GetTopLeft().GetColumn() ; j <= cellArray.GetBottomRight().GetColumn() ; j++) {
				count++;
				JPanel item = createItem(new StructDefine.Position(i, j));
				contentPanel.add(item);
			}
		setSize(new Dimension(280, 40*count+80));
		setLocationRelativeTo(null);

		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						for(JTextField textField : textFields) {
							formulas.add(textField.getText());
						}
						setVisible(false);
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						setVisible(false);
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	private JPanel createItem(StructDefine.Position pos) {
		JPanel itemPanel = new JPanel();
		itemPanel.setLayout(new FlowLayout(10));
		
		char column = (char) ('A' + pos.GetColumn());
		JLabel label = new JLabel(column + "" + (pos.GetRow() + 1));
		itemPanel.add(label);
		
		JTextField textField = new JTextField();
		textField.setPreferredSize(new Dimension(200, 25));
		if(sheetReader.getCells()[pos.GetRow()][pos.GetColumn()].getCellType() == Cell.CELL_TYPE_FORMULA)
			textField.setText(sheetReader.getCells()[pos.GetRow()][pos.GetColumn()].getFormula());
		else
			textField.setText(sheetReader.getCells()[pos.GetRow()][pos.GetColumn()].getValue());
		itemPanel.add(textField);
		
		textFields.add(textField);
		
		return itemPanel;
	}
	
	public ArrayList<String> getFormulas() {
		return formulas;
	}
}
