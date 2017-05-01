package jp.gr.java_conf.piropiroping.bluetoothcommander;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by matsuzakihiroshi on 2017/04/15.
 */

public class FileTransferFragment extends DialogFragment {
    String[] assetList = null;
    int selectedNum;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final AssetManager assetManager = getResources().getAssets();
        try{
            assetList = assetManager.list("app");
        } catch (IOException e) {
            e.printStackTrace();
        }
        builder.setTitle("Please select command file.");
        builder.setSingleChoiceItems(assetList, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                selectedNum = i;
            }
        });
        builder.setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });
        builder.setPositiveButton("送信", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(selectedNum < 0){
                    dismiss();
                }
                String assetName = assetList[selectedNum];
                InputStream is;
                byte[] assetData = null;
                try {
                    is = assetManager.open("app/" + assetName);
                    assetData = new byte[is.available()];
                    is.read(assetData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mListener.onDialogFileListClick(assetName, assetData);
                dismiss();
            }
        });

        return builder.create();
    }

    FileListListener mListener;

    public interface  FileListListener{
        public void onDialogFileListClick(String assetName, byte[] assetData);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        try{
            mListener = (FileListListener)activity;
        }catch(ClassCastException e){
            throw new ClassCastException(activity.toString() + "must implement FileListListener");
        }
    }

}
