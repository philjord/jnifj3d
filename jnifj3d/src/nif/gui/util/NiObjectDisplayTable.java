package nif.gui.util;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import nif.niobject.NiObject;

public class NiObjectDisplayTable extends JPanel
{
	private NifTableModel tableModel = new NifTableModel(new String[]
	{ "Class", "Variable", "Value" }, 0);

	private JTable table = new JTable(tableModel);

	private NiObject niobject;

	public NiObjectDisplayTable()
	{
		this.setLayout(new GridLayout(1, 1));
		this.add(new JScrollPane(table));
		table.setRowSorter(new TableRowSorter<NifTableModel>(tableModel));
	}

	public void displayNiObject(NiObject niobject2)
	{
		this.niobject = niobject2;
		tableModel.setNumRows(0);

		if (niobject != null)
		{

			ArrayList<Object[]> rowsToDisplay = new ArrayList<Object[]>();
			rowsToDisplay.add(new Object[]
			{ niobject.getClass(), niobject.toString(), "" });
			niobject.addDisplayRows(rowsToDisplay);

			for (Object[] row : rowsToDisplay)
			{
				tableModel.addRow(row);
			}
		}

	}

	private class NifTableModel extends DefaultTableModel
	{
		public NifTableModel(Object[] columnNames, int rowCount)
		{
			super(columnNames, rowCount);
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return false; // disallow editing of the table
		}

		@Override
		public Class<?> getColumnClass(int c)
		{
			return getValueAt(0, c).getClass();
		}
	}
}
