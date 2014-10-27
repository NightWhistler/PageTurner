package net.nightwhistler.pageturner.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import net.nightwhistler.pageturner.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FileAdapter extends BaseAdapter {

    private File currentFolder;
    private List<FileItem> items = new ArrayList<>();

    private Context context;

    private ItemSelectionListener itemSelectionListener;

    public static interface ItemSelectionListener {
        void fileSelected( File file );
    }

    public FileAdapter( Context context ) {
        this.context = context;
    }

    public void setFolder( File folder ) {

        this.currentFolder = folder;
        items = new ArrayList<>();
        File[] listing = folder.listFiles();

        if ( listing != null ) {
            for ( File childFile: listing ) {
                if ( childFile.isDirectory() || childFile.getName().toLowerCase(Locale.US).endsWith(".epub")) {
                    items.add(new FileItem(childFile.getName(), childFile, ! childFile.isDirectory() ));
                }
            }
        }

        Collections.sort(items, (lhs, rhs) -> {

            if ((lhs.getFile().isDirectory() && rhs.getFile().isDirectory()) ||
                    (!lhs.getFile().isDirectory() && !rhs.getFile().isDirectory())) {
                return lhs.getFile().getName().compareTo(rhs.getFile().getName());
            }

            if (lhs.getFile().isDirectory()) {
                return -1;
            } else {
                return 1;
            }
        });

        items.add( 0, new FileItem( "[" + context.getString(R.string.import_this) + "]", folder, true));

        if ( folder.getParentFile() != null ) {
            items.add(0, new FileItem( "[..]", folder.getParentFile(), false ));
        }

        notifyDataSetChanged();
    }

    public String getCurrentFolder() {
        return this.currentFolder.getAbsolutePath();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public FileItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView;
        final FileItem fileItem = getItem(position);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if ( convertView == null ) {
            rowView = inflater.inflate(R.layout.folder_line, parent, false);
        } else {
            rowView = convertView;
        }

        ImageView img = (ImageView) rowView.findViewById(R.id.folderIcon);
        CheckBox selectBox = (CheckBox) rowView.findViewById(R.id.selectBox);

        if ( fileItem.getFile().isDirectory() ) {
            img.setImageDrawable( context.getResources().getDrawable(R.drawable.folder));
            selectBox.setVisibility(View.VISIBLE);
        } else {
            img.setImageDrawable( context.getResources().getDrawable(R.drawable.file));
            selectBox.setVisibility(View.GONE);
        }

        selectBox.setOnCheckedChangeListener( (buttonView, isChecked) -> {
            if ( isChecked && itemSelectionListener != null ) {
                this.itemSelectionListener.fileSelected( fileItem.getFile() );
            }
        });

        selectBox.setFocusable(false);

        TextView label = (TextView) rowView.findViewById(R.id.folderName);

        label.setText( fileItem.getLabel() );

        return rowView;
    }

    public void setItemSelectionListener(ItemSelectionListener itemSelectionListener) {
        this.itemSelectionListener = itemSelectionListener;
    }
}

