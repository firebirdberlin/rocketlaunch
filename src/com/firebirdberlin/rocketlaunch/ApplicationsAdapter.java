package com.firebirdberlin.rocketlaunch;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SectionIndexer;
import android.widget.TextView;
import java.lang.Character;
import java.text.Normalizer.Form;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * GridView adapter to show the list of all installed applications.
 */
class ApplicationsAdapter extends ArrayAdapter<mApplicationInfo> implements SectionIndexer,  Filterable{
	private Rect mOldBounds = new Rect();
	public ArrayList<mApplicationInfo> filtered;
	private ApplicationInfoArray  mOriginalData;
	private Context ctx;
	HashMap<String, Integer> alphaIndexer;
	String[] sections;
	private int SortOrder;
	public static final int SORT_BY_ALPHA = 0;
	public static final int SORT_BY_USAGE = 1;

	public ApplicationsAdapter(Context context, ArrayList<mApplicationInfo> apps) {
		super(context, 0, apps);
		ctx = context;
		sort(compByAlpha);
		SortOrder = SORT_BY_ALPHA;
		updateIndex();
	}

	public void updateData(ArrayList<mApplicationInfo> apps){
		clear();
		addAll(apps);

		sort(compByAlpha);
		updateIndex();
		save();

		// restore order if needed
		if (SortOrder == SORT_BY_USAGE) sort(compByUsage);
		notifyDataSetChanged();
	}

	public void save(){
		boolean mOriginalData_present = true;
		if (mOriginalData == null){
			mOriginalData_present = false;
			mOriginalData = new ApplicationInfoArray(ctx);
			for (int i = 0; i < getCount(); i++){
				mOriginalData.add(getItem(i));
			}
		}

		Collections.sort(mOriginalData,compByAlpha); // Sort in order to ...
		mOriginalData.saveToFile("apps.dat");		 // ... save the data


		if (mOriginalData_present == false) mOriginalData = null;
		else if (SortOrder == SORT_BY_USAGE) {
            Collections.sort(mOriginalData,compByUsage);
		}
	}

	public void updateIcons(){
		for (int i = 0; i < getCount(); i++){
			getItem(i).loadIconCache(ctx);
		}
		notifyDataSetChanged();

		try{
			if (mOriginalData != null){
				for (int i = 0; i < mOriginalData.size(); i++){
					mOriginalData.get(i).loadIconCache(ctx);
				}
			}
		} catch (NullPointerException e){};
	}

	public void clearIcons(){
		for (int i = 0; i < getCount(); i++){
			getItem(i).icon = null;
		}

		try{
			if (mOriginalData != null){
				for (int i = 0; i < mOriginalData.size(); i++){
					mOriginalData.get(i).icon = null;
				}
			}
		} catch (NullPointerException e){};
	}

	public void updateIndex(){
		// in order to build the index, we need alphabetically sorted data
		alphaIndexer = new HashMap<String, Integer>();
		int size = getCount();
		int cnt = 0;
		char oldch = '0';
		for (int x = 0; x < size; x++) {
			char ch = getItem(x).title.charAt(0);
			ch = Character.toUpperCase(ch);
			String chs = Character.toString(ch);
			//// put only if the key does not exist
			if ( x == 0 || oldch != ch){
				alphaIndexer.put(chs, x);
				cnt++;
				oldch = ch;
			}
		}

		sections = new String[cnt];
		cnt = 0;
		for (int x = 0; x < size; x++) {
			char ch = getItem(x).title.charAt(0);
			ch = Character.toUpperCase(ch);
			if ( x == 0 || oldch != ch){
				sections[cnt++] = Character.toString(ch);
				oldch = ch;
			}
		}
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        try{
            final mApplicationInfo info = getItem(position);
            if (info == null || convertView == null) {
                //final LayoutInflater inflater = getContext().getLayoutInflater();
                final LayoutInflater inflater = 
                    (LayoutInflater) ctx.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                convertView = inflater.inflate(R.layout.application, parent, false);
            }

            final TextView textView = (TextView) convertView.findViewById(R.id.label);

            if (info.icon != null){
                info.filterIcon(ctx);
                textView.setCompoundDrawables(null, info.icon, null, null);
            } else {
                textView.setCompoundDrawables(null, null, null, null);
            }
            textView.setText(info.title);

            return convertView;
        } catch (NullPointerException e) {
            final LayoutInflater inflater = 
                (LayoutInflater) ctx.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            return inflater.inflate(R.layout.application, parent, false);
        }
	}

	@Override
	public Filter getFilter() {
		return new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				// constraint is the result from text you want to filter against.
				// objects is your data set you will filter from
				if (mOriginalData == null){
					int len = getCount();
					mOriginalData = new ApplicationInfoArray(ctx, len);
					for (int i = 0; i < len; i++){
						mOriginalData.add(getItem(i));
					}
				}

				FilterResults filterResults = new FilterResults();
				ArrayList<mApplicationInfo> tempList=new ArrayList<mApplicationInfo>();

				if (constraint == null || constraint.length() <= 0){ // no filter
					filterResults.values = new ArrayList<mApplicationInfo>(mOriginalData);
					filterResults.count = mOriginalData.size();
					mOriginalData = null;
				} else if (constraint != null && mOriginalData!=null) {
                    for (int i = 0; i < mOriginalData.size(); i++){
                        mApplicationInfo item=mOriginalData.get(i);
                        if (item.title.toString().toLowerCase().contains(constraint.toString().toLowerCase())){
                            tempList.add(item);
                        }
                    }
                    // The following two lines are very important
                    // as publish result can only take FilterResults objects
                    filterResults.values = tempList;
                    filterResults.count = tempList.size();
				}
				
				return filterResults;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				//Log.d(LOG_TAG, "publishResults()");
				filtered = (ArrayList<mApplicationInfo>)results.values;
				if (filtered != null){
					notifyDataSetChanged();
					clear();
					for(int i = 0, l = filtered.size(); i < l; i++)
						add(filtered.get(i));
				}
				notifyDataSetInvalidated();

			}

		};
	}

	 /**
	 * Performs a binary search or cache lookup to find the first row that matches a given section's starting letter.
	 */
	@Override
	public int getPositionForSection(int sectionIndex){
		return alphaIndexer.get(sections[sectionIndex]);
	}

	/**
	 * Returns the section index for a given position in the list by querying the item and comparing it with all items
	 * in the section array.
	 */
	@Override
	public int getSectionForPosition(int position){
		return 0;
	}

	/**
	 * Returns the section array constructed from the alphabet provided in the constructor.
	 */
	@Override
	public Object[] getSections() {
		switch (SortOrder){
			case SORT_BY_ALPHA:
				return sections;
			case SORT_BY_USAGE:
				return null;
		}
		 return sections;
	}

    final public static Comparator<mApplicationInfo> compByUsage = new Comparator<mApplicationInfo>() {
        public int compare(mApplicationInfo e1, mApplicationInfo e2) {
            return e2.usage - e1.usage;
        }
    };

    final public static Comparator<mApplicationInfo> compByAlpha = new Comparator<mApplicationInfo>() {
        public int compare(mApplicationInfo e1, mApplicationInfo e2) {

            String s1 = e1.title.toString().toLowerCase();
            String s2 = e2.title.toString().toLowerCase();
            s1 = Normalizer.normalize(s1, Normalizer.Form.NFD);
            s2 = Normalizer.normalize(s2, Normalizer.Form.NFD);
            return s1.compareTo(s2);

        }
    };

	public void toggleSortOrder() {
		switch (SortOrder){
			case SORT_BY_ALPHA:
				sort(compByUsage);
				SortOrder = SORT_BY_USAGE;
				break;
			case SORT_BY_USAGE:
				sort(compByAlpha);
				SortOrder = SORT_BY_ALPHA;
				break;
			default:
				sort(compByAlpha);
				SortOrder = SORT_BY_ALPHA;
				break;
		}
	}
}
