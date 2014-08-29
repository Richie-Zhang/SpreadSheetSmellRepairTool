package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.poi.ss.usermodel.Cell;

import parser.ExpParser;
import core.DetectRepairSmell;
import core.ExtractCellArray;
import core.Formula;
import core.StructDefine;
import file.Metadata;
import file.ReadWriteFile;
import file.SheetReader;

public class MainUI extends JFrame {
	private static final long serialVersionUID = 1L;
	
	public static final int MAXROW = 700;
	public static final int MAXCOLUMN = 52;
	
	private JTextField textField_fileName;
	private JList<String> list_sheetList;
	private JTable table, table_snippet, table_cellArray, table_smell;
	private JButton button_switchValueFormula;
	private JTabbedPane tabbedPane;
	private JTextArea textArea_repairAdvise;
	private JButton button_ignore, button_apply, button_manualUpdate;
	private JButton btn_selectAllSnippet, btn_unselectAllSnippet, btn_addSnippet, btn_deleteSnippet, btn_alterSnippet, btn_submitSnippet;
	private JButton btn_selectAllCellArray, btn_unselectAllCellArray, btn_addCellArray, btn_deleteCellArray, btn_alterCellArray, btn_submitCellArray;
	
	private ArrayList<SheetReader> sheetReaders = new ArrayList<>();
	private ArrayList<ExtractCellArray> extractCellArrays = new ArrayList<>();
	private ArrayList<DetectRepairSmell> detectRepairSmells = new ArrayList<>();
	private int currentSheetIndex = -1;
	private Color[][] snippetColors = new Color[MAXROW][MAXCOLUMN], cellArrayColors = new Color[MAXROW][MAXCOLUMN], smellColors = new Color[MAXROW][MAXCOLUMN];

	private String lastPath = System.getProperty("user.dir");
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		Locale.setDefault(Locale.ENGLISH);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainUI frame = new MainUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public MainUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1350, 700); 
		setLocationRelativeTo(null);
		setTitle("AmCheck");

		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(10, 10));
		
		JPanel openFilePanel = createOpenFilePanel();
		contentPane.add(openFilePanel, BorderLayout.PAGE_START);
		
		JPanel sheetListPanel = createSheetListPanel();
		contentPane.add(sheetListPanel, BorderLayout.LINE_START);
		
		JPanel tablePanel = createTabelPanel();
		JPanel tabbedPanel = createTabbedPanel();
		JSplitPane splitPane = new JSplitPane();
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, tabbedPanel);
		splitPane.setDividerLocation(800);
		splitPane.setResizeWeight(1.0);
		splitPane.setBorder(null);
		
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		clearSnippets();
	}
	
	private JPanel createOpenFilePanel() {
		JPanel openFilePanel = new JPanel();
		openFilePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
		
		JButton btn_openFile = new JButton("Open file");
		btn_openFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser_openFile = new JFileChooser(lastPath);
		        FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel File(*.xlsx,.xls)", "xlsx", "xls");
		        fileChooser_openFile.setFileFilter(filter);
		        int openFile = fileChooser_openFile.showOpenDialog(null);

		        if (openFile == JFileChooser.APPROVE_OPTION) {
		            String filename = fileChooser_openFile.getSelectedFile().toString();
		            lastPath = new File(filename).getParent();
		            try {
						sheetReaders = ReadWriteFile.readExcelFile(filename);
						if(sheetReaders == null){
							JOptionPane.showMessageDialog(null, "Open file failed!");
							return;
						}
						for(SheetReader sheetReader : sheetReaders) {
							System.err.println(sheetReader.getRowCount() + "," + sheetReader.getColumnCount());
							if(sheetReader.getRowCount() > MAXROW || sheetReader.getColumnCount() > MAXCOLUMN) {
								JOptionPane.showMessageDialog(null, "File is too large!");
								return;
							}
						}
						
						extractCellArrays.clear();
						for(SheetReader sheetReader : sheetReaders) {
							extractCellArrays.add(new ExtractCellArray(sheetReader));
						}
						loadSheetList();
						currentSheetIndex = -1;
						button_switchValueFormula.setVisible(false);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
	                textField_fileName.setText(filename);
		        }
			}
		});
		openFilePanel.add(btn_openFile);
		
		textField_fileName = new JTextField();
		textField_fileName.setEnabled(false);
		openFilePanel.add(textField_fileName);
		textField_fileName.setColumns(80);
		
		button_switchValueFormula = new JButton("Click to view formula");
		button_switchValueFormula.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(button_switchValueFormula.getText().equals("Click to view formula")) {
					button_switchValueFormula.setText("Click to view value");
					loadSheetFormula();
				}
				else {
					button_switchValueFormula.setText("Click to view formula");
					loadSheetValue();
				}
			}
		});
		button_switchValueFormula.setVisible(false);
		openFilePanel.add(button_switchValueFormula);

		return openFilePanel;
	}

	private JPanel createSheetListPanel() {
		JPanel sheetListPanel = new JPanel();
		sheetListPanel.setPreferredSize(new Dimension(160, HEIGHT));
		sheetListPanel.setLayout(new BorderLayout(5, 5));
		
		JLabel lblSheetList = new JLabel("Sheet list");
		sheetListPanel.add(lblSheetList, BorderLayout.NORTH);
		
		JScrollPane scrollPane_sheetList = new JScrollPane();
		sheetListPanel.add(scrollPane_sheetList, BorderLayout.CENTER);
		
		list_sheetList = new JList<>();
		list_sheetList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				if (list_sheetList.getValueIsAdjusting()) return;
				currentSheetIndex = list_sheetList.getSelectedIndex();
				
				loadSheetValue();
				clearSnippets();
				button_switchValueFormula.setVisible(true);
				button_switchValueFormula.setText("Click to view formula");
				
				tabbedPane.setSelectedIndex(0);
			}
		});
		list_sheetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane_sheetList.setViewportView(list_sheetList);
		
		return sheetListPanel;
	}
	
	private JPanel createTabelPanel() {
		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BorderLayout(5, 5));

		JScrollPane scrollPane_table = new JScrollPane();
		tablePanel.add(scrollPane_table, BorderLayout.CENTER);
		
		String[] headers = {"","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
				"AA","AB","AC","AD","AE","AF","AG","AH","AI","AJ","AK","AL","AM","AN","AO","AP","AQ","AR","AS","AT","AU","AV","AW","AX","AY","AZ"/*,
				"BA","BA","BC","BD","BE","BF","BG","BH","BI","BJ","BK","BL","BM","BN","BO","BP","BQ","BR","BS","BT","BU","BV","BW","BX","BY","BZ",
				"CA","CB","CC","CD","CE","CF","CG","CH","CI","CJ","CK","CL","CM","CN","CO","CP","CQ","CR","CS","CT","CU","CV","CW","CX","CY","CZ",
				"DA","DB","DC","DD","DE","DF","DG","DH","DI","DJ","DK","DL","DM","DN","DO","DP","DQ","DR","DS","DT","DU","DV","DW","DX","DY","DZ",
				"EA","EB","EC","ED","EE","EF","EG","EH","EI","EJ","EK","EL","EM","EN","EO","EP","EQ","ER","ES","ET","EU","EV","EW","EX","EY","EZ",
				"FA","FB","FC","FD","FE","FF","FG","FH","FI","FJ","FK","FL","FM","FN","FO","FP","FQ","FR","FS","FT","FU","FV","FW","FX","FY","FZ",
				"GA","GB","GC","GD","GE","GF","GG","GH","GI","GJ","GK","GL","GM","GN","GO","GP","GQ","GR","GS","GT","GU","GV","GW","GX","GY","GZ",
				"HA","HB","HC","HD","HE","HF","HG","HH","HI","HJ","HK","HL","HM","HN","HO","HP","HQ","HR","HS","HT","HU","HV","HW","HX","HY","HZ",
				"IA","IB","IC","ID","IE","IF","IG","IH","II","IJ","IK","IL","IM","IN","IO","IP","IQ","IR","IS","IT","IU","IV"*/};
		Object[][] cellData = new Object[MAXROW][MAXCOLUMN];
		for(int i = 0 ; i < MAXROW ; i++)
			cellData[i][0] = i+1;
		DefaultTableModel model = new DefaultTableModel(cellData, headers);
		table = new JTable(model){
			private static final long serialVersionUID = 1L;
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component component = super.prepareRenderer(renderer, row, column);
				if(tabbedPane.getSelectedIndex() == 0)
					component.setBackground(snippetColors[row][column]);
				else if(tabbedPane.getSelectedIndex() == 1)
					component.setBackground(cellArrayColors[row][column]);
				else if(tabbedPane.getSelectedIndex() == 2)
					component.setBackground(smellColors[row][column]);
				if(column != 0 && table.isCellSelected(row, column)){
					component.setBackground(table.getSelectionBackground());
				}
				return component;
			}
			public boolean isCellEditable(int row, int column) {
				return false;
			};
		};
		table.setFillsViewportHeight(true);
		table.setColumnSelectionAllowed(true);
		table.setCellSelectionEnabled(true);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getTableHeader().setReorderingAllowed(false);
		table.getColumnModel().getColumn(0).setPreferredWidth(30);
		for(int i = 1 ; i < MAXCOLUMN ; i++) {
			TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(100);
			column.setMinWidth(50);
		}
		for(int i = 0 ; i < MAXROW ; i++){
			snippetColors[i][0] = table.getTableHeader().getBackground();
			cellArrayColors[i][0] = table.getTableHeader().getBackground();
			smellColors[i][0] = table.getTableHeader().getBackground();
		}

		scrollPane_table.setViewportView(table);
		
		return tablePanel;
	}
	
	private JPanel createTabbedPanel() {
		JPanel tabbedPanel = new JPanel();
		tabbedPanel.setPreferredSize(new Dimension(400, HEIGHT));
		tabbedPanel.setLayout(new BorderLayout(0, 0));
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPanel.add(tabbedPane, BorderLayout.CENTER);
		
		tabbedPane.setPreferredSize(new Dimension(400, 580));
		JPanel snippetPanel = createSnippetPanel();
		tabbedPane.addTab("Snippets", snippetPanel);
		JPanel cellArrayPanel = createCellArrayPanel();
		tabbedPane.addTab("Cell arrays", cellArrayPanel);
		JPanel smellRepairPanel = createSmellRepairPanel();
		tabbedPane.addTab("Smells or errors", smellRepairPanel);
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				table.updateUI();
			}
		});	
		
		return tabbedPanel;
	}
	
	private JPanel createSnippetPanel() {
		JPanel snippetPanel = new JPanel();
		snippetPanel.setLayout(new BorderLayout(5, 5));
		
		JPanel panel_opSnippet = new JPanel();
		snippetPanel.add(panel_opSnippet, BorderLayout.EAST);
		panel_opSnippet.setLayout(null);
		panel_opSnippet.setPreferredSize(new Dimension(120, HEIGHT));
		
		JButton btn_identifySnippet = new JButton("Identify");
		btn_identifySnippet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(currentSheetIndex < 0) return;
				extractCellArrays.get(currentSheetIndex).identifySnippets();
				updateSnippets();
				table_snippet.clearSelection();
				clearColor(snippetColors);
				clearCellArrays();
				btn_selectAllSnippet.setEnabled(true);
				btn_unselectAllSnippet.setEnabled(true);
				btn_addSnippet.setEnabled(true);
				btn_deleteSnippet.setEnabled(true);
				btn_alterSnippet.setEnabled(true);
				btn_submitSnippet.setEnabled(true);
			}
		});
		btn_identifySnippet.setBounds(0, 0, 115, 25);
		panel_opSnippet.add(btn_identifySnippet);
		
		JButton btn_loadMetadataSnippet = new JButton("Load meta");
		btn_loadMetadataSnippet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(currentSheetIndex < 0) return;
				
				try {
					ArrayList<StructDefine.Region> snips = Metadata.loadMetadataSnippet(textField_fileName.getText(), currentSheetIndex);
					if(snips == null)
						JOptionPane.showMessageDialog(null, "Metadata file is not existed !!");
					else if(snips.size() == 0) 
						JOptionPane.showMessageDialog(null, "The metadata file does not contain Snippet information !!");
					else {
						extractCellArrays.get(currentSheetIndex).setSnippets(snips);
						clearSnippets();
						updateSnippets();
						
						btn_selectAllSnippet.setEnabled(true);
						btn_unselectAllSnippet.setEnabled(true);
						btn_addSnippet.setEnabled(true);
						btn_deleteSnippet.setEnabled(true);
						btn_alterSnippet.setEnabled(true);
						btn_submitSnippet.setEnabled(true);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		btn_loadMetadataSnippet.setBounds(0, 30, 115, 25);
		panel_opSnippet.add(btn_loadMetadataSnippet);
		
		JButton btn_saveMetaSnippet = new JButton("Save meta");
		btn_saveMetaSnippet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				try {
					if(!Metadata.saveMetadataSnippet(textField_fileName.getText(), currentSheetIndex, extractCellArrays.get(currentSheetIndex).getSnippets()))
						JOptionPane.showMessageDialog(null, "Save failed, please close all related file and try again !!");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		btn_saveMetaSnippet.setBounds(0, 60, 115, 25);
		panel_opSnippet.add(btn_saveMetaSnippet);
		
		btn_selectAllSnippet = new JButton("Select all");
		btn_selectAllSnippet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(currentSheetIndex < 0) return;
				MyTableModel tableModel = (MyTableModel) table_snippet.getModel();
				int rowCount = tableModel.getRowCount();
				for(int i = 0 ; i < rowCount ; i++) {
					tableModel.setValueAt(new Boolean(true), i, 0);
				}
				table_snippet.setModel(tableModel);
			}
		});
		btn_selectAllSnippet.setBounds(0, 120, 115, 25);
		panel_opSnippet.add(btn_selectAllSnippet);
		
		btn_unselectAllSnippet = new JButton("Clear all");
		btn_unselectAllSnippet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				MyTableModel tableModel = (MyTableModel) table_snippet.getModel();
				int rowCount = tableModel.getRowCount();
				for(int i = 0 ; i < rowCount ; i++) {
					tableModel.setValueAt(new Boolean(false), i, 0);
				}
				table_snippet.setModel(tableModel);
				table_snippet.clearSelection();
			}
		});
		btn_unselectAllSnippet.setBounds(0, 150, 115, 25);
		panel_opSnippet.add(btn_unselectAllSnippet);
		
		btn_addSnippet = new JButton("Add");
		btn_addSnippet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				String topLeft = JOptionPane.showInputDialog(null, "Enter upper left position:(e.g., A1)", "", JOptionPane.QUESTION_MESSAGE);
				if(topLeft == null || topLeft.length() < 2) return;
                String bottomRight = JOptionPane.showInputDialog(null, "Enter lower right position:(e.g., C5)", "", JOptionPane.QUESTION_MESSAGE);
                if (bottomRight == null || bottomRight.length() < 2) return;
                StructDefine.Position tlPosition = StructDefine.Position.ConvertPosition(topLeft);
                StructDefine.Position brPosition = StructDefine.Position.ConvertPosition(bottomRight);
                if (tlPosition == null || brPosition == null) {
                    JOptionPane.showMessageDialog(null, "Invalid input, add failed !");
                    return;
                }
                boolean addSuccess = extractCellArrays.get(currentSheetIndex).addSnippet(new StructDefine.Region(tlPosition, brPosition));
                if(!addSuccess) {
                	JOptionPane.showMessageDialog(null, "The input is not a valid snippet, add failed ! \n "
                			+ "Hint: The snippet cann't share the same cell with an existed snippet and every cell must have an numerical value or formula !");
                	return;
                }
                
                MyTableModel tableModel = (MyTableModel) table_snippet.getModel();
                Object[] arr = new Object[3];
                char tlrow = (char) ('A' + tlPosition.GetColumn());
                char brrow = (char) ('A' + brPosition.GetColumn());
                arr[0] = new Boolean(false);
                arr[1] = tlrow + "" + (tlPosition.GetRow() + 1);
                arr[2] = brrow + "" + (brPosition.GetRow() + 1);
    			tableModel.addValue(arr);
    			table_snippet.setModel(tableModel);
    			table_snippet.updateUI();
    			
    			clearCellArrays();
			}
		});
		btn_addSnippet.setBounds(0, 180, 115, 25);
		panel_opSnippet.add(btn_addSnippet);
		
		btn_deleteSnippet = new JButton("Delete");
		btn_deleteSnippet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				int selectedItem = table_snippet.getSelectedRow();
                if (selectedItem < 0) {
                    JOptionPane.showMessageDialog(null, "Please select one item first !");
                    return;
                }
                StructDefine.Region snip = extractCellArrays.get(currentSheetIndex).removeSnippet(selectedItem);
                setColor(snippetColors, snip, Color.WHITE);
                MyTableModel tableModel = (MyTableModel) table_snippet.getModel();
                tableModel.removeItem(selectedItem);
                table_snippet.setModel(tableModel);
                table_snippet.clearSelection();
				table_snippet.updateUI();
				
				clearCellArrays();
			}
		});
		btn_deleteSnippet.setBounds(0, 210, 115, 25);
		panel_opSnippet.add(btn_deleteSnippet);
		
		btn_alterSnippet = new JButton("Alter");
		btn_alterSnippet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				int selectedItem = table_snippet.getSelectedRow();
                if (selectedItem < 0) {
                    JOptionPane.showMessageDialog(null, "Please select one item first !");
                    return;
                }
                MyTableModel tableModel = (MyTableModel) table_snippet.getModel();
                String topLeft = JOptionPane.showInputDialog(null, "Enter upper left position:(e.g., A1)", tableModel.getValueAt(selectedItem, 1));
                String bottomRight = JOptionPane.showInputDialog(null, "Enter lower right position:(e.g., C5)", tableModel.getValueAt(selectedItem, 2));
                if (topLeft == null || bottomRight == null || topLeft.length() < 2 || bottomRight.length() < 2) {
                    return;
                }
                StructDefine.Position tlPosition = StructDefine.Position.ConvertPosition(topLeft);
                StructDefine.Position brPosition = StructDefine.Position.ConvertPosition(bottomRight);
                if (tlPosition == null || brPosition == null) {
                    JOptionPane.showMessageDialog(null, "Invalid input, alter failed !");
                    return;
                }
                StructDefine.Region newSnippet = new StructDefine.Region(tlPosition, brPosition);
                StructDefine.Region oldSnippet = extractCellArrays.get(currentSheetIndex).changeSnippet(selectedItem, newSnippet);
                
                if(oldSnippet == null){
                    JOptionPane.showMessageDialog(null, "The input is not a valid Snippet, alter failed!");
                    return;
                }
                
                setColor(snippetColors, oldSnippet, Color.WHITE);
                
                Object[] arr = new Object[3];
                char tlrow = (char) ('A' + tlPosition.GetColumn());
                char brrow = (char) ('A' + brPosition.GetColumn());
                arr[0] = tableModel.getValueAt(selectedItem, 0);
                arr[1] = tlrow + "" + (tlPosition.GetRow() + 1);
                arr[2] = brrow + "" + (brPosition.GetRow() + 1);
                tableModel.alterValue(selectedItem, arr);
                table_snippet.setModel(tableModel);
                table_snippet.updateUI();
                
                if((Boolean)tableModel.getValueAt(selectedItem, 0))
                	setColor(snippetColors, newSnippet, Color.GREEN);
                else
					setColor(snippetColors, newSnippet, new Color(135,206,235));
                
                clearCellArrays();
			}
		});
		btn_alterSnippet.setBounds(0, 240, 115, 25);
		panel_opSnippet.add(btn_alterSnippet);
		
		btn_submitSnippet = new JButton("Confirm");
		btn_submitSnippet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				tabbedPane.setSelectedIndex(1);
				
				extractCellArrays.get(currentSheetIndex).extractCellArrays();
				updateCellArrays();
				
				btn_selectAllCellArray.setEnabled(true);
				btn_unselectAllCellArray.setEnabled(true);
				btn_addCellArray.setEnabled(true);
				btn_deleteCellArray.setEnabled(true);
				btn_alterCellArray.setEnabled(true);
				btn_submitCellArray.setEnabled(true);
				
				tabbedPane.setEnabledAt(1, true);
			}
		});
		btn_submitSnippet.setBounds(0, 310, 115, 25);
		panel_opSnippet.add(btn_submitSnippet);
		
		JScrollPane scrollPane_snippet = new JScrollPane();
		snippetPanel.add(scrollPane_snippet, BorderLayout.CENTER);
		
		table_snippet = new JTable(new MyTableModel());
		table_snippet.setFillsViewportHeight(true);
		table_snippet.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table_snippet.getTableHeader().setReorderingAllowed(false);
		table_snippet.getTableHeader().setResizingAllowed(false);
		table_snippet.getColumnModel().getColumn(0).setPreferredWidth(table_snippet.getColumnModel().getColumn(0).getMinWidth());
		table_snippet.getModel().addTableModelListener(new TableModelListener() { 
	    	public void tableChanged(TableModelEvent e) {
	    		int col = e.getColumn();                   
	    		int row = e.getFirstRow();
	    		if(col != 0) return;
	    		ArrayList<StructDefine.Region> snippets = extractCellArrays.get(currentSheetIndex).getSnippets();
	    		if(table_snippet.getSelectedRow() == row) {
		    		if((Boolean)table_snippet.getValueAt(row, col))
		    			setColor(snippetColors, snippets.get(row), Color.GREEN);
		    		else
		    			setColor(snippetColors, snippets.get(row), new Color(135,206,235));
	    		}
	    		else {
	    			if((Boolean)table_snippet.getValueAt(row, col))
		    			setColor(snippetColors, snippets.get(row), Color.YELLOW);
		    		else
		    			setColor(snippetColors, snippets.get(row), Color.WHITE);
	    		}
	    	}
		});
		table_snippet.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				if(e.getValueIsAdjusting()) return;
				if(table_snippet.getSelectedRow() < 0) return;
				ArrayList<StructDefine.Region> snippets = extractCellArrays.get(currentSheetIndex).getSnippets();
				for(int i = 0 ; i < table_snippet.getRowCount() ; i++)
					if((Boolean)table_snippet.getValueAt(i, 0))
						setColor(snippetColors, snippets.get(i), Color.YELLOW);
		    		else
		    			setColor(snippetColors, snippets.get(i), Color.WHITE);
				int row = table_snippet.getSelectedRow();
				if(row < 0) return;
				if((Boolean)table_snippet.getValueAt(row, 0))
					setColor(snippetColors, snippets.get(row), Color.GREEN);
				else
					setColor(snippetColors, snippets.get(row), new Color(135,206,235));
			}
		});
		
		scrollPane_snippet.setViewportView(table_snippet);
		
		return snippetPanel;
	}
	
	private JPanel createCellArrayPanel() {
		JPanel cellArrayPanel = new JPanel();
		cellArrayPanel.setLayout(new BorderLayout(5, 5));
		
		JPanel panel_opCellArray = new JPanel();
		cellArrayPanel.add(panel_opCellArray, BorderLayout.EAST);
		panel_opCellArray.setLayout(null);
		panel_opCellArray.setPreferredSize(new Dimension(120, HEIGHT));
		
		/*JButton btn_extractCellArray = new JButton("Extract");
		btn_extractCellArray.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(currentSheetIndex < 0) return;
				extractCellArrays.get(currentSheetIndex).extractCellArrays();
				updateCellArrays();
				table_cellArray.clearSelection();
				clearColor(cellArrayColors);
				clearSmells();
				
				btn_selectAllCellArray.setEnabled(true);
				btn_unselectAllCellArray.setEnabled(true);
				btn_addCellArray.setEnabled(true);
				btn_deleteCellArray.setEnabled(true);
				btn_alterCellArray.setEnabled(true);
				btn_submitCellArray.setEnabled(true);
			}
		});
		btn_extractCellArray.setBounds(0, 0, 115, 25);
		panel_opCellArray.add(btn_extractCellArray);*/
		
		JButton btn_loadMetadataCellArray = new JButton("Load meta");
		btn_loadMetadataCellArray.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				
				try {
					ArrayList<StructDefine.Region> cas = Metadata.loadMetadataCellArray(textField_fileName.getText(), currentSheetIndex);
					if(cas == null)
						JOptionPane.showMessageDialog(null, "Metadata file is not existed !!");
					else if(cas.size() == 0) 
						JOptionPane.showMessageDialog(null, "The metadata file does not contain Snippet information !!");
					else {
						extractCellArrays.get(currentSheetIndex).setCellArrays(cas);
						clearCellArrays();
						updateCellArrays();
						
						btn_selectAllCellArray.setEnabled(true);
						btn_unselectAllCellArray.setEnabled(true);
						btn_addCellArray.setEnabled(true);
						btn_deleteCellArray.setEnabled(true);
						btn_alterCellArray.setEnabled(true);
						btn_submitCellArray.setEnabled(true);
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		btn_loadMetadataCellArray.setBounds(0, 30, 115, 25);
		panel_opCellArray.add(btn_loadMetadataCellArray);
		
		JButton btn_saveMetaCellArray = new JButton("Save meta");
		btn_saveMetaCellArray.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				try {
					if(!Metadata.saveMetadataCellArray(textField_fileName.getText(), currentSheetIndex, extractCellArrays.get(currentSheetIndex).getCellArrays()))
						JOptionPane.showMessageDialog(null, "Save failed, please close all related file and try again !!");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		btn_saveMetaCellArray.setBounds(0, 60, 115, 25);
		panel_opCellArray.add(btn_saveMetaCellArray);
		
		btn_selectAllCellArray = new JButton("Select all");
		btn_selectAllCellArray.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				MyTableModel tableModel = (MyTableModel) table_cellArray.getModel();
				int rowCount = tableModel.getRowCount();
				for(int i = 0 ; i < rowCount ; i++) {
					tableModel.setValueAt(new Boolean(true), i, 0);
				}
				table_cellArray.setModel(tableModel);
			}
		});
		btn_selectAllCellArray.setBounds(0, 120, 115, 25);
		panel_opCellArray.add(btn_selectAllCellArray);
		
		btn_unselectAllCellArray = new JButton("Clear all");
		btn_unselectAllCellArray.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				MyTableModel tableModel = (MyTableModel) table_cellArray.getModel();
				int rowCount = tableModel.getRowCount();
				for(int i = 0 ; i < rowCount ; i++) {
					tableModel.setValueAt(new Boolean(false), i, 0);
				}
				table_cellArray.setModel(tableModel);
				table_cellArray.clearSelection();
			}
		});
		btn_unselectAllCellArray.setBounds(0, 150, 115, 25);
		panel_opCellArray.add(btn_unselectAllCellArray);
		
		btn_addCellArray = new JButton("Add");
		btn_addCellArray.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				String topLeft = JOptionPane.showInputDialog(null, "Enter upper left position:(e.g., A1)", "", JOptionPane.QUESTION_MESSAGE);
				if(topLeft == null || topLeft.length() < 2) return;
                String bottomRight = JOptionPane.showInputDialog(null, "Enter lower right position:(e.g., A5)", "", JOptionPane.QUESTION_MESSAGE);
                if (bottomRight == null || bottomRight.length() < 2) return;
                StructDefine.Position tlPosition = StructDefine.Position.ConvertPosition(topLeft);
                StructDefine.Position brPosition = StructDefine.Position.ConvertPosition(bottomRight);
                if (tlPosition == null || brPosition == null) {
                    JOptionPane.showMessageDialog(null, "Invalid input, add failed !");
                    return;
                }
                boolean addSuccess = extractCellArrays.get(currentSheetIndex).addCellArray(new StructDefine.Region(tlPosition, brPosition));
                
                if(!addSuccess) {
                	JOptionPane.showMessageDialog(null, "The input is not a valid cell array, add failed ! \n "
                			+ "Hint: The cell array cann't share the same cell with an existed cell array and every cell must have an numerical value or formula !");
                	return;
                }
                
                MyTableModel tableModel = (MyTableModel) table_cellArray.getModel();
                Object[] arr = new Object[3];
                char tlrow = (char) ('A' + tlPosition.GetColumn());
                char brrow = (char) ('A' + brPosition.GetColumn());
                arr[0] = new Boolean(false);
                arr[1] = tlrow + "" + (tlPosition.GetRow() + 1);
                arr[2] = brrow + "" + (brPosition.GetRow() + 1);
    			tableModel.addValue(arr);
    			table_cellArray.setModel(tableModel);
    			table_cellArray.updateUI();
    			
    			clearSmells();
			}
		});
		btn_addCellArray.setBounds(0, 180, 115, 25);
		panel_opCellArray.add(btn_addCellArray);
		
		btn_deleteCellArray = new JButton("Delete");
		btn_deleteCellArray.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				int selectedItem = table_cellArray.getSelectedRow();
                if (selectedItem < 0) {
                    JOptionPane.showMessageDialog(null, "Please select one item first !");
                    return;
                }
                StructDefine.Region ca = extractCellArrays.get(currentSheetIndex).removeCellArray(selectedItem);
                setColor(cellArrayColors, ca, Color.WHITE);
                
                MyTableModel tableModel = (MyTableModel) table_cellArray.getModel();
                tableModel.removeItem(selectedItem);
                table_cellArray.setModel(tableModel);
                table_cellArray.clearSelection();
                table_cellArray.updateUI();
                
                clearSmells();
			}
		});
		btn_deleteCellArray.setBounds(0, 210, 115, 25);
		panel_opCellArray.add(btn_deleteCellArray);
		
		btn_alterCellArray = new JButton("Alter");
		btn_alterCellArray.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				int selectedItem = table_cellArray.getSelectedRow();
                if (selectedItem < 0) {
                    JOptionPane.showMessageDialog(null, "Please select one item first !");
                    return;
                }
                MyTableModel tableModel = (MyTableModel) table_cellArray.getModel();
                String topLeft = JOptionPane.showInputDialog(null, "Enter upper left position:(e.g., A1)", tableModel.getValueAt(selectedItem, 1));
                String bottomRight = JOptionPane.showInputDialog(null, "Enter lower right position:(e.g., C5)", tableModel.getValueAt(selectedItem, 2));
                if (topLeft == null || bottomRight == null || topLeft.length() < 2 || bottomRight.length() < 2) {
                    return;
                }
                StructDefine.Position tlPosition = StructDefine.Position.ConvertPosition(topLeft);
                StructDefine.Position brPosition = StructDefine.Position.ConvertPosition(bottomRight);
                if (tlPosition == null || brPosition == null) {
                    JOptionPane.showMessageDialog(null, "Invalid input, alter failed !");
                    return;
                }
                StructDefine.Region newSnippet = new StructDefine.Region(tlPosition, brPosition);
                StructDefine.Region oldSnippet = extractCellArrays.get(currentSheetIndex).changeCellArray(selectedItem, newSnippet);
                if(oldSnippet == null) {
	                JOptionPane.showMessageDialog(null, "The input is not a valid Cell Array, alter failed!");
	            	return;
                }
                
                setColor(cellArrayColors, oldSnippet, Color.WHITE);
                
                Object[] arr = new Object[3];
                char tlrow = (char) ('A' + tlPosition.GetColumn());
                char brrow = (char) ('A' + brPosition.GetColumn());
                arr[0] = tableModel.getValueAt(selectedItem, 0);
                arr[1] = tlrow + "" + (tlPosition.GetRow() + 1);
                arr[2] = brrow + "" + (brPosition.GetRow() + 1);
                tableModel.alterValue(selectedItem, arr);
                table_cellArray.setModel(tableModel);
                table_cellArray.updateUI();
                
                if((Boolean)tableModel.getValueAt(selectedItem, 0))
                	setColor(cellArrayColors, newSnippet, Color.GREEN);
                else
					setColor(cellArrayColors, newSnippet, new Color(135,206,235));
                
                clearSmells();
			}
		});
		btn_alterCellArray.setBounds(0, 240, 115, 25);
		panel_opCellArray.add(btn_alterCellArray);
		
		btn_submitCellArray = new JButton("Confirm");
		btn_submitCellArray.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				detectRepairSmells.clear();
				ArrayList<StructDefine.Region> cellArrays = extractCellArrays.get(currentSheetIndex).getCellArrays();
				for(StructDefine.Region ca : cellArrays) {
					DetectRepairSmell drs;
					try {
						drs = new DetectRepairSmell(sheetReaders.get(currentSheetIndex), ca);
						if(drs.hasSmell())
							detectRepairSmells.add(drs);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				
				tabbedPane.setSelectedIndex(2);
				tabbedPane.setEnabledAt(2, true);
				updateSmells();
			}
		});
		btn_submitCellArray.setBounds(0, 310, 115, 25);
		panel_opCellArray.add(btn_submitCellArray);
		
		JScrollPane scrollPane_CellArray = new JScrollPane();
		cellArrayPanel.add(scrollPane_CellArray, BorderLayout.CENTER);
		
		table_cellArray = new JTable(new MyTableModel());
		table_cellArray.setFillsViewportHeight(true);
		table_cellArray.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table_cellArray.getTableHeader().setReorderingAllowed(false);
		table_cellArray.getTableHeader().setResizingAllowed(false);
		table_cellArray.getColumnModel().getColumn(0).setPreferredWidth(table_cellArray.getColumnModel().getColumn(0).getMinWidth());
		table_cellArray.getModel().addTableModelListener(new TableModelListener() { 
	    	public void tableChanged(TableModelEvent e) {
	    		if(currentSheetIndex < 0) return;
	    		int col = e.getColumn();                   
	    		int row = e.getFirstRow();
	    		if(col != 0) return;
	    		ArrayList<StructDefine.Region> snippets = extractCellArrays.get(currentSheetIndex).getCellArrays();
	    		if(table_cellArray.getSelectedRow() == row) {
		    		if((Boolean)table_cellArray.getValueAt(row, col))
		    			setColor(cellArrayColors, snippets.get(row), Color.GREEN);
		    		else
		    			setColor(cellArrayColors, snippets.get(row), new Color(135,206,235));
	    		}
	    		else {
	    			if((Boolean)table_cellArray.getValueAt(row, col))
		    			setColor(cellArrayColors, snippets.get(row), Color.YELLOW);
		    		else
		    			setColor(cellArrayColors, snippets.get(row), Color.WHITE);
	    		}
	    	}
		});
		table_cellArray.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				if(currentSheetIndex < 0) return;
				if(e.getValueIsAdjusting()) return;
				if(table_cellArray.getSelectedRow() < 0) return;
				ArrayList<StructDefine.Region> snippets = extractCellArrays.get(currentSheetIndex).getCellArrays();
				for(int i = 0 ; i < table_cellArray.getRowCount() ; i++)
					if((Boolean)table_cellArray.getValueAt(i, 0))
						setColor(cellArrayColors, snippets.get(i), Color.YELLOW);
		    		else
		    			setColor(cellArrayColors, snippets.get(i), Color.WHITE);
				int row = table_cellArray.getSelectedRow();
				if(row < 0) return;
				if((Boolean)table_cellArray.getValueAt(row, 0))
					setColor(cellArrayColors, snippets.get(row), Color.GREEN);
				else
					setColor(cellArrayColors, snippets.get(row), new Color(135,206,235));
			}
		});
		
		scrollPane_CellArray.setViewportView(table_cellArray);
		
		return cellArrayPanel;
	}

	private JPanel createSmellRepairPanel() {
		JPanel smellRepairPanel = new JPanel();
		smellRepairPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel smellPanel = createSmellPanel();
		JPanel repairJPanel = createRepairPanel();
		
		JSplitPane splitPane = new JSplitPane();
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, smellPanel, repairJPanel);
		splitPane.setDividerLocation(200);
		splitPane.setResizeWeight(1.0);
		splitPane.setBorder(null);
		
		smellRepairPanel.add(splitPane, BorderLayout.CENTER);
		
		return smellRepairPanel;
	}
	
	private JPanel createSmellPanel() {
		JPanel panel_detectSmell = new JPanel();
		panel_detectSmell.setLayout(new BorderLayout(0, 5));
		JLabel label_smellList = new JLabel("Cell arrays with smells or errors");
		panel_detectSmell.add(label_smellList, BorderLayout.NORTH);
		
		JScrollPane scrollPane_smell = new JScrollPane();
		panel_detectSmell.add(scrollPane_smell, BorderLayout.CENTER);
		//scrollPane_smell.setPreferredSize(new Dimension(WIDTH, 200));
		
		String[] headers = {"Upper left", "Lower right", "Stage"};
		DefaultTableModel model = new DefaultTableModel(null, headers);
		table_smell = new JTable(model){
			private static final long serialVersionUID = 1L;
			public boolean isCellEditable(int row, int column) {
				return false;
			};
		};
		table_smell.setFillsViewportHeight(true);
		table_smell.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table_smell.getTableHeader().setReorderingAllowed(false);
		table_smell.getTableHeader().setResizingAllowed(false);
		table_smell.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if(currentSheetIndex < 0) return;
				if(e.getValueIsAdjusting()) return;
				textArea_repairAdvise.setText("");
				clearColor(smellColors);
				
				int row = table_smell.getSelectedRow();
				if(row < 0) return;
				
				button_apply.setEnabled(false);
				button_ignore.setEnabled(false);
				button_manualUpdate.setEnabled(false);
				
				DetectRepairSmell drs = detectRepairSmells.get(row);
				
				setColor(smellColors, drs.getCellArray(), Color.LIGHT_GRAY);
			}
		});
		scrollPane_smell.setViewportView(table_smell);
		
		JPanel panel_advise = new JPanel();
		panel_advise.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		JButton adviseButton = new JButton("Generate repair advise");
		adviseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(currentSheetIndex < 0) return;
				textArea_repairAdvise.setText("");
				clearColor(smellColors);
				
				int row = table_smell.getSelectedRow();
				if(row < 0) return;
				
				DetectRepairSmell drs = detectRepairSmells.get(row);

				if(!table_smell.getValueAt(row, 2).equals("Unhandled")) {
					button_apply.setEnabled(false);
					button_ignore.setEnabled(false);
					button_manualUpdate.setEnabled(false);
				}
				else {
					button_apply.setEnabled(true);
					button_ignore.setEnabled(true);
					button_manualUpdate.setEnabled(true);
				}
				
				setColor(smellColors, drs.getCellArray(), Color.LIGHT_GRAY);
				//table.update(table.getGraphics());
				textArea_repairAdvise.update(textArea_repairAdvise.getGraphics());
				
				boolean recovery = drs.recovery();
				if(!recovery) {
					WaitDialog waitDialog = new WaitDialog(1);
					waitDialog.setAlwaysOnTop(true);
					waitDialog.setLocationRelativeTo(getParent());
					waitDialog.setResizable(false);
					waitDialog.setVisible(true);
					waitDialog.update(waitDialog.getGraphics());

					drs.synthesize();
					
					waitDialog.setVisible(false);
				}
				
				try {
					drs.repairSmell();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
				StructDefine.SmellAndRepair smellAndRepair = drs.getSmellAndRepair();
				
				if(drs.getState() != -1) {
					if(smellAndRepair.GetAccepted() == 1) {	//ÒÑÐÞ¸´
						for(int i = 0 ; i < smellAndRepair.GetPositions().size() ; i++) {
							StructDefine.Position pos = smellAndRepair.GetPositions().get(i);
							//StructDefine.RepairAdvise ra = smellAndRepair.GetRepairAdvises().get(i);
							setColor(smellColors, new StructDefine.Region(pos, pos), Color.GREEN);
						}
					}
					else if(smellAndRepair.GetAccepted() == 2) {
						
					}
					else if(smellAndRepair.GetAccepted() == 3) {
						for(StructDefine.Position pos : smellAndRepair.GetInputPositions()) {
							setColor(smellColors, new StructDefine.Region(pos, pos), Color.BLUE);
						}
					}
					else {	//Î´ÐÞ¸´
						for(int i = 0 ; i < smellAndRepair.GetPositions().size() ; i++) {
							StructDefine.Position pos = smellAndRepair.GetPositions().get(i);
							StructDefine.RepairAdvise ra = smellAndRepair.GetRepairAdvises().get(i);
							
							char c = (char) ('A' + pos.GetColumn());
							String posString = c + "" + (pos.GetRow() + 1);
							String formulaString = ra.GetFormula();
							
							if(ra.GetType() == 1) {
								setColor(smellColors, new StructDefine.Region(pos, pos), Color.RED);
								textArea_repairAdvise.append("Error: Cell("+posString+"): Change to formula "+formulaString+" (value = "+ra.GetValue()+")\n");
							}
							else if(ra.GetType() == 2) {
								setColor(smellColors, new StructDefine.Region(pos, pos), Color.YELLOW);
								textArea_repairAdvise.append("Smell: Cell("+posString+"): Change to formula "+formulaString+" (value = "+ra.GetValue()+")\n");
							}
						}
					}
				}
				else {
					textArea_repairAdvise.setText("Cannot infer target formula patterns.");
					button_apply.setEnabled(false);
				}
			}
		});
		panel_advise.add(adviseButton);
		
		panel_detectSmell.add(panel_advise, BorderLayout.SOUTH);
		return panel_detectSmell;
	}
	
	private JPanel createRepairPanel() {
		JPanel repairPanel = new JPanel();
		
		repairPanel.setLayout(new BorderLayout(0, 5));
		
		JPanel panel_repairAdvise = new JPanel();
		repairPanel.add(panel_repairAdvise, BorderLayout.CENTER);
		panel_repairAdvise.setLayout(new BorderLayout(0, 5));
		
		JLabel lable_repair = new JLabel("Repairing options");
		panel_repairAdvise.add(lable_repair, BorderLayout.NORTH);
		
		JScrollPane scrollPane_repairAdvise = new JScrollPane();
		panel_repairAdvise.add(scrollPane_repairAdvise, BorderLayout.CENTER);
		
		textArea_repairAdvise = new JTextArea();
		textArea_repairAdvise.setEditable(false);
		scrollPane_repairAdvise.setViewportView(textArea_repairAdvise);
		
		JPanel panel_repairButton = new JPanel();
		panel_repairAdvise.add(panel_repairButton, BorderLayout.SOUTH);
		panel_repairButton.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		
		button_ignore = new JButton("Ignore");
		button_ignore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(currentSheetIndex < 0) return;
				int row = table_smell.getSelectedRow();
				if(row < 0) return;
				DetectRepairSmell drs = detectRepairSmells.get(row);
				if(drs.getSmellAndRepair().GetAccepted() != 0) return;
				int result = JOptionPane.showConfirmDialog(null, "Sure to ignore the item ?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(result == JOptionPane.YES_OPTION) {
					table_smell.setValueAt("Ignored", table_smell.getSelectedRow(), 2);
					drs.getSmellAndRepair().SetAccepted(2);
					setColor(smellColors, drs.getCellArray(), Color.LIGHT_GRAY);
				}
				
				button_apply.setEnabled(false);
				button_ignore.setEnabled(false);
				button_manualUpdate.setEnabled(false);
			}
		});
		panel_repairButton.add(button_ignore);
		
		button_apply = new JButton("Apply");
		button_apply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				int row = table_smell.getSelectedRow();
				if(row < 0) return;
				
				applyRepairSmell(row);
				
				for(int i = 0 ; i < detectRepairSmells.get(row).getSmellAndRepair().GetPositions().size() ; i++) {
					StructDefine.Position pos = detectRepairSmells.get(row).getSmellAndRepair().GetPositions().get(i);
					setColor(smellColors, new StructDefine.Region(pos, pos), Color.GREEN);
				}
				
				button_apply.setEnabled(false);
				button_ignore.setEnabled(false);
				button_manualUpdate.setEnabled(false);
			}
		});
		panel_repairButton.add(button_apply);
		
		button_manualUpdate = new JButton("Manual update");
		button_manualUpdate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(currentSheetIndex < 0) return;
				int row = table_smell.getSelectedRow();
				if(row < 0) return;
				DetectRepairSmell drs = detectRepairSmells.get(row);
				if(drs.getSmellAndRepair().GetAccepted() != 0) return;
				
				ManualUpdate manualUpdateDialog = new ManualUpdate(sheetReaders.get(currentSheetIndex), drs.getCellArray());
				manualUpdateDialog.setModal(true);
				manualUpdateDialog.setVisible(true);
				manualUpdateDialog.setLocationRelativeTo(null);
				ArrayList<String> inputFormulas = manualUpdateDialog.getFormulas();

				if(inputFormulas.size() == 0) return;
				int index = -1;
				drs.getSmellAndRepair().SetAccepted(3);
				table_smell.setValueAt("Manual Updated", table_smell.getSelectedRow(), 2);
				setColor(smellColors, new StructDefine.Region(drs.getCellArray().GetTopLeft(), drs.getCellArray().GetBottomRight()), Color.LIGHT_GRAY);
				DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
				for(int i = drs.getCellArray().GetTopLeft().GetRow() ; i <= drs.getCellArray().GetBottomRight().GetRow() ; i++)
					for(int j = drs.getCellArray().GetTopLeft().GetColumn() ; j <= drs.getCellArray().GetBottomRight().GetColumn() ; j++) {
						index++;
						if(sheetReaders.get(currentSheetIndex).getCells()[i][j].getCellType() == Cell.CELL_TYPE_FORMULA && sheetReaders.get(currentSheetIndex).getCells()[i][j].getFormula().equals(inputFormulas.get(index)))
							continue;
						if(sheetReaders.get(currentSheetIndex).getCells()[i][j].getCellType() == Cell.CELL_TYPE_NUMERIC && sheetReaders.get(currentSheetIndex).getCells()[i][j].getValue().equals(inputFormulas.get(index))){
							System.err.println(sheetReaders.get(currentSheetIndex).getCells()[i][j].getValue());
							continue;
						}
						drs.getSmellAndRepair().AddInputPosition(new StructDefine.Position(i, j));
						sheetReaders.get(currentSheetIndex).getCells()[i][j].setCellType(Cell.CELL_TYPE_FORMULA);
						sheetReaders.get(currentSheetIndex).getCells()[i][j].setFormula(inputFormulas.get(index));
						if(button_switchValueFormula.getText().equals("Click to view value"))
							tableModel.setValueAt(inputFormulas.get(index), i, j+1);
						setColor(smellColors, new StructDefine.Region(new StructDefine.Position(i, j), new StructDefine.Position(i, j)), Color.BLUE);
						ExpParser expParser = new ExpParser(sheetReaders.get(currentSheetIndex), new StructDefine.Position(i, j));
		    			try {
							double value = expParser.evaluate(new Formula(new StructDefine.Position(i, j), inputFormulas.get(index)).getR1C1Formula());
							sheetReaders.get(currentSheetIndex).getCells()[i][j].setValue(value+"");
							if(button_switchValueFormula.getText().equals("Click to view formula"))
								tableModel.setValueAt(value, i, j+1);
						} catch (Exception e1) {
							e1.printStackTrace();
						}		
					}
				
				button_apply.setEnabled(false);
				button_ignore.setEnabled(false);
				button_manualUpdate.setEnabled(false);
			}
		});
		panel_repairButton.add(button_manualUpdate);
		
		button_apply.setEnabled(false);
		button_ignore.setEnabled(false);
		button_manualUpdate.setEnabled(false);
		
		JPanel panel_saveToFile = new JPanel();
		panel_saveToFile.setLayout(new FlowLayout(FlowLayout.RIGHT));
		repairPanel.add(panel_saveToFile, BorderLayout.SOUTH);
		
		JButton btn_applyAll = new JButton("Apply all");
		btn_applyAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(currentSheetIndex < 0) return;
				textArea_repairAdvise.setText("");
				clearColor(smellColors);
				
				WaitDialog waitDialog = new WaitDialog(2);
				waitDialog.setAlwaysOnTop(true);
				waitDialog.setLocationRelativeTo(getParent());
				waitDialog.setResizable(false);
				waitDialog.setVisible(true);
				waitDialog.update(waitDialog.getGraphics());
				
				table_smell.clearSelection();
				for(int i = 0 ; i < detectRepairSmells.size() ; i++) {
					DetectRepairSmell drs = detectRepairSmells.get(i);
					boolean recovery = drs.recovery();
					if(!recovery) {
						drs.synthesize();
					}
					try {
						drs.repairSmell();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					
					applyRepairSmell(i);
				}
				
				waitDialog.setVisible(false);
			}
		});
		panel_saveToFile.add(btn_applyAll);
		
		JButton btn_saveToFile = new JButton("Save file");
		btn_saveToFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					String filename = ReadWriteFile.writeExcelFile(textField_fileName.getText(), sheetReaders);
					if(filename == null)
						JOptionPane.showMessageDialog(null, "Save failed");
					else {
						JOptionPane.showMessageDialog(null, "Save successfully, file name is: " + filename + " !");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		panel_saveToFile.add(btn_saveToFile);
		
		return repairPanel;
	}
	
	private void loadSheetList() {
		clearSheet();
		clearSnippets();
		list_sheetList.clearSelection();
		int sheetNum = sheetReaders.size();
		list_sheetList.removeAll();
		DefaultListModel<String> listModel = new DefaultListModel<String>();
		for(int i = 0 ; i < sheetNum ; i++) {
			listModel.addElement(sheetReaders.get(i).getSheetName());
		}
		list_sheetList.setModel(listModel);
	}
	
	private void loadSheetValue() {
		if(currentSheetIndex < 0) return;
		clearSheet();
		
		int rowCount = sheetReaders.get(currentSheetIndex).getRowCount(), columnCount = sheetReaders.get(currentSheetIndex).getColumnCount();
		
		TableModel tableModel = table.getModel();
		for(int i = 0 ; i < rowCount ; i++)
			for(int j = 0 ; j < columnCount ; j++){
				tableModel.setValueAt(sheetReaders.get(currentSheetIndex).getCells()[i][j].getValue(), i, j+1);
			}
		table.setModel(tableModel);
	}
	
	private void loadSheetFormula() {
		if(currentSheetIndex < 0) return;
		clearSheet();
		
		int rowCount = sheetReaders.get(currentSheetIndex).getRowCount(), columnCount = sheetReaders.get(currentSheetIndex).getColumnCount();
		
		TableModel tableModel = table.getModel();
		for(int i = 0 ; i < rowCount ; i++)
			for(int j = 0 ; j < columnCount ; j++){
				StructDefine.Cell cell = sheetReaders.get(currentSheetIndex).getCells()[i][j];
				if(cell.getCellType() == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA)
					tableModel.setValueAt(cell.getFormula(), i, j+1);
				else
					tableModel.setValueAt(cell.getValue(), i, j+1);
			}
		table.setModel(tableModel);
	}
	
	private void clearSheet() {
		table.clearSelection();
		TableModel tableModel = table.getModel();
		for(int i = 0 ; i < MAXROW ; i++)
			for(int j = 1 ; j < MAXCOLUMN ; j++){
				tableModel.setValueAt("", i, j);
			}
		table.setModel(tableModel);
	}

	private void updateSnippets() {
		if(currentSheetIndex < 0) return;
		ArrayList<StructDefine.Region> snippets = extractCellArrays.get(currentSheetIndex).getSnippets();
		MyTableModel tableModel = (MyTableModel) table_snippet.getModel();
		tableModel.removeAll();
		for(StructDefine.Region snip : snippets){
			Object[] arr = new Object[3];
			arr[0] = new Boolean(false);
            char tlrow = (char) ('A' + snip.GetTopLeft().GetColumn());
            char brrow = (char) ('A' + snip.GetBottomRight().GetColumn());
            arr[1] = tlrow + "" + (snip.GetTopLeft().GetRow() + 1);
            arr[2] = brrow + "" + (snip.GetBottomRight().GetRow() + 1);
			tableModel.addValue(arr);
		}
		table_snippet.setModel(tableModel);
		table_snippet.updateUI();
	}
	
	private void clearSnippets() {
		table_snippet.clearSelection();
		MyTableModel tableModel = (MyTableModel) table_snippet.getModel();
		tableModel.removeAll();
		table_snippet.setModel(tableModel);
		table_snippet.updateUI();

		clearColor(snippetColors);
		clearCellArrays();
		
		btn_selectAllSnippet.setEnabled(false);
		btn_unselectAllSnippet.setEnabled(false);
		btn_addSnippet.setEnabled(false);
		btn_deleteSnippet.setEnabled(false);
		btn_alterSnippet.setEnabled(false);
		btn_submitSnippet.setEnabled(false);
		
		tabbedPane.setEnabledAt(1, false);
	}
	
	private void updateCellArrays() {
		if(currentSheetIndex < 0) return;
		ArrayList<StructDefine.Region> cellArrays = extractCellArrays.get(currentSheetIndex).getCellArrays();
		MyTableModel tableModel = (MyTableModel) table_cellArray.getModel();
		tableModel.removeAll();
		for(StructDefine.Region cellArray : cellArrays){
			Object[] arr = new Object[3];
			arr[0] = new Boolean(false);
            char tlrow = (char) ('A' + cellArray.GetTopLeft().GetColumn());
            char brrow = (char) ('A' + cellArray.GetBottomRight().GetColumn());
            arr[1] = tlrow + "" + (cellArray.GetTopLeft().GetRow() + 1);
            arr[2] = brrow + "" + (cellArray.GetBottomRight().GetRow() + 1);
			tableModel.addValue(arr);
		}
		table_cellArray.setModel(tableModel);
		table_cellArray.updateUI();
	}
	
	private void clearCellArrays() {
		table_cellArray.clearSelection();
		MyTableModel tableModel = (MyTableModel) table_cellArray.getModel();
		tableModel.removeAll();
		table_cellArray.setModel(tableModel);
		table_cellArray.updateUI();
		
		clearColor(cellArrayColors);
		clearSmells();
		
		btn_selectAllCellArray.setEnabled(false);
		btn_unselectAllCellArray.setEnabled(false);
		btn_addCellArray.setEnabled(false);
		btn_deleteCellArray.setEnabled(false);
		btn_alterCellArray.setEnabled(false);
		btn_submitCellArray.setEnabled(false);
		
		tabbedPane.setEnabledAt(2, false);
	}
	
	private void setColor(Color[][] colors, StructDefine.Region snip, Color color){
		int startRow = snip.GetTopLeft().GetRow(), endRow = snip.GetBottomRight().GetRow();
		int startColumn = snip.GetTopLeft().GetColumn(), endColumn = snip.GetBottomRight().GetColumn();

        for(int i = startRow ; i <= endRow ; i++) {
        	for(int j = startColumn+1 ; j <= endColumn+1 ; j++) {
        		colors[i][j] = color;
        	}
        }
        
        table.updateUI();
	}
	
	private void clearColor(Color[][] colors) {
		for(int i = 0 ; i < MAXROW ; i++)
			for(int j = 1 ; j < MAXCOLUMN ; j++)
				colors[i][j] = Color.WHITE;
		table.updateUI();
	}

	private void updateSmells() {
		if(currentSheetIndex < 0) return;
		clearSmells();
		DefaultTableModel tableModel = (DefaultTableModel) table_smell.getModel();
		for(DetectRepairSmell drs : detectRepairSmells){
			Object[] arr = new Object[3];
            char tlrow = (char) ('A' + drs.getCellArray().GetTopLeft().GetColumn());
            char brrow = (char) ('A' + drs.getCellArray().GetBottomRight().GetColumn());
            arr[0] = tlrow + "" + (drs.getCellArray().GetTopLeft().GetRow() + 1);
            arr[1] = brrow + "" + (drs.getCellArray().GetBottomRight().GetRow() + 1);
            arr[2] = "Unhandled";
			tableModel.addRow(arr);
		}
		table_smell.setModel(tableModel);
		table_smell.updateUI();
	}
	
	private void clearSmells() {
		table_smell.clearSelection();
		DefaultTableModel tableModel = (DefaultTableModel) table_smell.getModel();
		
		while(tableModel.getRowCount()>0){
			tableModel.removeRow(tableModel.getRowCount()-1);
		}
		
		/*for(int i = tableModel.getRowCount()-1 ; i >= 0 ; i--)
			tableModel.removeRow(i);*/
		table_smell.setModel(tableModel);
		table_smell.updateUI();
		
		clearColor(smellColors);
		textArea_repairAdvise.setText("");
		button_apply.setEnabled(false);
		button_ignore.setEnabled(false);
		button_manualUpdate.setEnabled(false);
	}
	
	private void applyRepairSmell(int row) {
		DetectRepairSmell drs = detectRepairSmells.get(row);
		if(drs.getSmellAndRepair().GetAccepted() != 0 || drs.getState() == -1) return;
		
		if(drs.getState() != -1) {
			table_smell.setValueAt("Applied", row, 2);
			drs.getSmellAndRepair().SetAccepted(1);
			for(int i = 0 ; i < drs.getSmellAndRepair().GetPositions().size() ; i++) {
				StructDefine.Position pos = drs.getSmellAndRepair().GetPositions().get(i);
				StructDefine.RepairAdvise ra = drs.getSmellAndRepair().GetRepairAdvises().get(i);
				sheetReaders.get(currentSheetIndex).setCellFormulaAt(ra.GetFormula(), pos.GetRow(), pos.GetColumn());
				sheetReaders.get(currentSheetIndex).setCellValueAt(ra.GetValue(), pos.GetRow(), pos.GetColumn());
				
				DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
				if(button_switchValueFormula.getText().equals("Click to view formula"))
					tableModel.setValueAt(ra.GetValue()+"", pos.GetRow(), pos.GetColumn()+1);
				else
					tableModel.setValueAt(ra.GetFormula(), pos.GetRow(), pos.GetColumn()+1);
			}
		}
		else {
			//TODO
		}
	}
	
}
