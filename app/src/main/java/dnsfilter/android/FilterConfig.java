package dnsfilter.android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.View.OnClickListener;

import java.net.URL;
import java.util.TreeMap;


public class FilterConfig implements OnClickListener, DialogInterface.OnKeyListener {

	static String NEW_ITEM = "<new>";
	static String INVALID_URL = "<invalid URL!>";
	static String ALL_CATEGORIES = "All Categories";
	static String ALL_ACTIVE = "All Active Filters";



	TableLayout configTable;
	FilterConfigEntry[] filterEntries;
	boolean loaded = false;

	TableRow editedRow;
	Button editOk;
	Button editDelete;
	Button editCancel;
	Dialog editDialog;
	Button categoryUp;
	Button categoryDown;
	TextView categoryField;
	TreeMap <String, Integer> categoryMap;

	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)
			dialog.dismiss();

		return false;
	}


	public static class FilterConfigEntry {
		boolean active;
		String category;
		String id;
		String url;

		public FilterConfigEntry(boolean active, String category, String id, String url) {
			this.active = active;
			this.category = category;
			this.id = id;
			this.url = url;
		}
	}

	public FilterConfig(TableLayout table, Button categoryUp, Button categoryDn, TextView categoryField ) {

		configTable = table;
		editDialog = new Dialog(table.getContext(), R.style.Theme_dialog_TitleBar);

		editDialog.setOnKeyListener(this);
		editDialog.setContentView(R.layout.filterentryeditdialog);
		editDialog.setTitle(table.getContext().getResources().getString(R.string.editFilterDialogTitle));
		editOk = editDialog.findViewById(R.id.filterEditOkBtn);
		editDelete = editDialog.findViewById(R.id.filterEditDelBtn);
		editCancel = editDialog.findViewById(R.id.filterEditCancelBtn);
		editOk.setOnClickListener(this);
		editDelete.setOnClickListener(this);
		editCancel.setOnClickListener(this);
		
		this.categoryUp = categoryUp;
		this.categoryDown = categoryDn;
		this.categoryField = categoryField;
		categoryField.setText(ALL_ACTIVE);

		categoryMap = new TreeMap();
	}

	private View[] getContentCells(TableRow row) {

		View[] result = new View[5];
		for (int i = 0; i < 5; i++)
			result[i] = row.getChildAt(i);

		result[2] = ((ViewGroup) result[2]).getChildAt(0); // element 1 is a scrollview with nested TextView
		result[3] = ((ViewGroup) result[3]).getChildAt(0); // element 2 is a scrollview with nested TextView

		return result;
	}

	private void addItem(FilterConfigEntry entry) {
		TableRow row = (TableRow) LayoutInflater.from(configTable.getContext()).inflate(R.layout.filterconfigentry, null);
		configTable.addView(row);



		View[] cells = getContentCells(row);
		((CheckBox) cells[0]).setChecked(entry.active);
		((TextView) cells[1]).setText(entry.category);
		((TextView) cells[2]).setText(entry.id);
		((TextView) cells[3]).setText(entry.url);
		cells[4].setOnClickListener(this);

		setVisibility(row);
	}

	private void setVisibility(TableRow row) {
		String currentCategory = categoryField.getText().toString();
		String rowCategory = ((TextView)row.getVirtualChildAt(1)).getText().toString();
		boolean active = ((CheckBox)row.getVirtualChildAt(0)).isChecked();
		boolean visible =	(currentCategory.equals(ALL_CATEGORIES)) ||
				(currentCategory.equals(ALL_ACTIVE) && 	active) ||
				(rowCategory.equals(NEW_ITEM)) ||
				(rowCategory.equals(currentCategory));

		if (visible)
			row.setVisibility(View.VISIBLE);
		else
			row.setVisibility(View.GONE);
	}


	public void setEntries(FilterConfigEntry[] entries) {

		this.filterEntries = entries;
		categoryMap.clear();
		categoryMap.put(ALL_ACTIVE, 0);
		categoryMap.put(ALL_CATEGORIES, 0);
		for (FilterConfigEntry filterEntry : filterEntries) {
			Integer count = categoryMap.get(filterEntry.category);
			if (count == null)
				count = 0;
			count = count + 1;
			categoryMap.put(filterEntry.category, count);
		}
	}

	public void load() {
		if (loaded) {
			return;
		}

		for (FilterConfigEntry filterEntry : filterEntries) {
			addItem(filterEntry);
		}
		addEmptyEndItem();

		categoryDown.setOnClickListener(this);
		categoryUp.setOnClickListener(this);

		loaded = true;
	}

	private void addEmptyEndItem() {
		addItem(new FilterConfigEntry(false, NEW_ITEM, NEW_ITEM, NEW_ITEM));
	}

	public String getCurrentCategory() {
		return categoryField.getText().toString();
	}

	public void setCurrentCategory(String category) {
		categoryField.setText(category);
	}
	
	public FilterConfigEntry[] getFilterEntries() {
		if (!loaded)
			return filterEntries;

		int count = configTable.getChildCount() - 2;
		FilterConfigEntry[] result = new FilterConfigEntry[count];
		for (int i = 0; i < count; i++) {
			View[] rowContent = getContentCells((TableRow) configTable.getChildAt(i + 1));
			result[i] = new FilterConfigEntry(((CheckBox) rowContent[0]).isChecked(),((TextView) rowContent[1]).getText().toString().trim(), ((TextView) rowContent[2]).getText().toString().trim(), ((TextView) rowContent[3]).getText().toString().trim());
		}
		return result;
	}

	public void clear() {
		filterEntries = getFilterEntries();
		int count = configTable.getChildCount() - 1;
		for (int i = count; i > 0; i--) {
			TableRow row = (TableRow) configTable.getChildAt(i);
			row.getChildAt(4).setOnClickListener(null);
			configTable.removeView(row);
		}
		categoryDown.setOnClickListener(null);
		categoryUp.setOnClickListener(null);
		loaded = false;
	}

	@Override
	public void onClick(View v) {
		if (v == editOk || v == editDelete || v == editCancel)
			handleEditDialogEvent(v);
		else if (v==categoryUp || v == categoryDown)
			handleCategoryChange((Button) v);
		else
			showEditDialog((TableRow) v.getParent());
	}

	private void handleCategoryChange(Button v) {
		String currentCategory = categoryField.getText().toString();
		String newCategory;
		if (!categoryMap.containsKey(currentCategory))
			newCategory = ALL_ACTIVE;

		else if (v == categoryUp) {
			newCategory = categoryMap.higherKey(currentCategory);
			if (newCategory == null)
				newCategory = categoryMap.firstKey();
		}
		else {
			newCategory = categoryMap.lowerKey(currentCategory);
			if (newCategory == null)
				newCategory = categoryMap.lastKey();
		}
		categoryField.setText(newCategory);
		updateView();
	}

	private void updateView() {
		int count = configTable.getChildCount() - 2;

		for (int i = 0; i < count; i++) {
			TableRow row = ((TableRow) configTable.getChildAt(i + 1));
			setVisibility(row);
		}
	}

	private void showEditDialog(TableRow row) {
		editedRow = row;
		View[] currentContent = getContentCells(editedRow);
		((CheckBox)editDialog.findViewById(R.id.activeChk)).setChecked(((CheckBox)currentContent[0]).isChecked());
		((TextView)editDialog.findViewById(R.id.filterCategory)).setText(((TextView)currentContent[1]).getText().toString());
		((TextView)editDialog.findViewById(R.id.filterName)).setText(((TextView)currentContent[2]).getText().toString());
		((TextView)editDialog.findViewById(R.id.filterUrl)).setText(((TextView)currentContent[3]).getText().toString());
		editDialog.show();
		WindowManager.LayoutParams lp = editDialog.getWindow().getAttributes();
		lp.width = (int)(configTable.getContext().getResources().getDisplayMetrics().widthPixels*1.00);
		editDialog.getWindow().setAttributes(lp);
	}

	private void handleEditDialogEvent(View v) {
		if (v == editCancel) {
			editDialog.dismiss();
			editDialog.findViewById(R.id.errorMsg).setVisibility(View.GONE);
			return;
		}

		View[] currentContent = getContentCells(editedRow);
		boolean newItem = ((TextView)currentContent[2]).getText().toString().equals(NEW_ITEM);

		if (v == editDelete) {
			if (!newItem) {
				editedRow.getChildAt(3).setOnClickListener(null);
				configTable.removeView(editedRow);
				String currentCategory = ((TextView)currentContent[1]).getText().toString();
				Integer count = categoryMap.get(currentCategory);
				if (count == 1) {
					categoryMap.remove(currentCategory);

				} else {
					categoryMap.put(currentCategory, count - 1);
				}
			}
			editDialog.dismiss();
			editDialog.findViewById(R.id.errorMsg).setVisibility(View.GONE);
		}
		else if (v == editOk) {
			View[] content = new View[4];
			content[0] = editDialog.findViewById(R.id.activeChk);
			content[1] = editDialog.findViewById(R.id.filterCategory);
			content[2] = editDialog.findViewById(R.id.filterName);
			content[3] = editDialog.findViewById(R.id.filterUrl);

			try  {
				validateContent(content);
			} catch (Exception e) {
				TextView errorView = editDialog.findViewById(R.id.errorMsg);
				errorView.setVisibility(View.VISIBLE);
				errorView.setText(e.getMessage());
				return;
			}

			boolean active = ((CheckBox)content[0]).isChecked();
			String category = ((TextView)content[1]).getText().toString();
			String name = ((TextView)content[2]).getText().toString();
			String url = ((TextView)content[3]).getText().toString();

			//category changed? => Update categories!

			String currentCategory = ((TextView)currentContent[1]).getText().toString();
			if (!currentCategory.equals(category)) {
				//decrease count for previous category
				if (!currentCategory.equals(NEW_ITEM)) {
					Integer count = categoryMap.get(currentCategory);
					if (count == 1)
						categoryMap.remove(currentCategory);
					else
						categoryMap.put(currentCategory, count - 1);
				}
				//increase count for new category
				Integer count = categoryMap.get(category);
				if (count == null)
					count = 0;
				categoryMap.put(category, count + 1);
			}

			((CheckBox)currentContent[0]).setChecked(active);
			((TextView)currentContent[1]).setText(category);
			((TextView)currentContent[2]).setText(name);
			((TextView)currentContent[3]).setText(url);

			if (newItem)
				newItem(editedRow);

			editDialog.dismiss();
			editDialog.findViewById(R.id.errorMsg).setVisibility(View.GONE);
		}

	}

	private void newItem(TableRow row) {
		addEmptyEndItem();
	}

	private void validateContent(View[] cells) throws Exception {
		URL url = new URL(((TextView) cells[3]).getText().toString());
		String shortTxt = ((TextView) cells[2]).getText().toString().trim();
		if (shortTxt.equals(NEW_ITEM) || shortTxt.equals("")) {
			((TextView) cells[2]).setText(url.getHost());
		}

		String category = ((TextView) cells[1]).getText().toString().trim();
		if (category.equals(NEW_ITEM) || category.equals("")) {
			((TextView) cells[1]).setText(url.getHost());
		}
	}
}
