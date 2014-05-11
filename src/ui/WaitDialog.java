package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;

import javax.swing.UIManager;

public class WaitDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();

	public WaitDialog(int type) {
		setTitle("Hint");
		//setBounds(100, 100, 250, 110);
		setSize(new Dimension(280, 100));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JLabel lbl1, lbl2;
			if(type == 1)
				lbl1 = new JLabel("The tool is synthesizing formulas.");
			else
				lbl1 = new JLabel("The tool is repairing all smells and errors.");
			lbl1.setFont(UIManager.getFont("OptionPane.messageFont"));
			contentPanel.add(lbl1);
			
			lbl2 = new JLabel("Please stand by...");
			lbl2.setFont(UIManager.getFont("OptionPane.messageFont"));
			contentPanel.add(lbl2);
		}
	}
}
