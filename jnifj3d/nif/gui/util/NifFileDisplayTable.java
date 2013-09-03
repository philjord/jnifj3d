package nif.gui.util;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import nif.NifFile;
import nif.niobject.NiObject;

public class NifFileDisplayTable extends JPanel
{
	private NifTableModel tableModel = new NifTableModel(new String[]
	{ "Index", "Type", "Name" }, 0);

	private JTable table = new JTable(tableModel);

	private NifFile nifFile;

	private NiObjectDisplayTable niObjectDisplayTable;

	public NifFileDisplayTable(NiObjectDisplayTable niObjectDisplayTable2)
	{
		this.niObjectDisplayTable = niObjectDisplayTable2;
		this.setLayout(new GridLayout(1, 1));
		this.add(new JScrollPane(table));

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (isEnabled())
				{
					int selectedRow = table.getSelectedRow();
					if (selectedRow != -1)
					{
						NiObject niobject = nifFile.blocks.getNiObjects()[selectedRow];
						niObjectDisplayTable.displayNiObject(niobject);
					}
					else
					{
						niObjectDisplayTable.displayNiObject(null);
					}
				}
			}
		});

	}

	public void displayNifFile(NifFile nifFile2)
	{
		this.nifFile = nifFile2;
		table.setRowSorter(null);
		tableModel.setNumRows(0);

		for (int i = 0; i < nifFile.blocks.getNiObjects().length; i++)
		{
			NiObject niobject = nifFile.blocks.getNiObjects()[i];
			tableModel.addRow(new Object[]
			{ i, niobject.getClass(), niobject.toString() });
		}
		table.setRowSorter(new TableRowSorter<NifTableModel>(tableModel));
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
