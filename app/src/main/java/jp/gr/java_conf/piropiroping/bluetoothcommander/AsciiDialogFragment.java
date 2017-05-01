package jp.gr.java_conf.piropiroping.bluetoothcommander;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

/**
 * Created by matsuzakihiroshi on 2017/03/18.
 */

public class AsciiDialogFragment extends DialogFragment implements View.OnClickListener {
    Button nulButton;
    Button sohButton;
    Button stxButton;
    Button etxButton;
    Button eotButton;
    Button enqButton;
    Button ackButton;
    Button belButton;
    Button bsButton;
    Button htButton;
    Button lfButton;
    Button vtButton;
    Button ffButton;
    Button crButton;
    Button soButton;
    Button siButton;
    Button dleButton;
    Button dc1Button;
    Button dc2Button;
    Button dc3Button;
    Button dc4Button;
    Button nakButton;
    Button synButton;
    Button etbButton;
    Button canButton;
    Button emButton;
    Button subButton;
    Button escButton;
    Button fsButton;
    Button gsButton;
    Button rsButton;
    Button usButton;
    Button delButton;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.ascii_dialog, null);
        setClickListner(view);
        builder.setView(view);
        builder.setMessage("制御コードを選択してください");

        return builder.create();
    }

    @Override
    public void onClick(View view) {
        mListener.onSelectCommandClick(view);
        dismiss();
    }

    public interface AsciiDialogListener {
        public void onSelectCommandClick(View view);
    }

    // Use this instance of the interface to deliver action events
    AsciiDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (AsciiDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString() + " must implement NoticeDialogListener");
        }
    }


    private void setClickListner(View view){
        nulButton = (Button) view.findViewById(R.id.nul_button);
        sohButton = (Button) view.findViewById(R.id.soh_button);
        stxButton = (Button) view.findViewById(R.id.stx_button);
        etxButton = (Button) view.findViewById(R.id.etx_button);
        eotButton = (Button) view.findViewById(R.id.eot_button);
        enqButton = (Button) view.findViewById(R.id.enq_button);
        ackButton = (Button) view.findViewById(R.id.ack_button);
        belButton = (Button) view.findViewById(R.id.bel_button);
        bsButton = (Button) view.findViewById(R.id.bs_button);
        htButton = (Button) view.findViewById(R.id.ht_button);
        lfButton = (Button) view.findViewById(R.id.lf_button);
        vtButton = (Button) view.findViewById(R.id.vt_button);
        ffButton = (Button) view.findViewById(R.id.ff_button);
        crButton = (Button) view.findViewById(R.id.cr_button);
        soButton = (Button) view.findViewById(R.id.so_button);
        siButton = (Button) view.findViewById(R.id.si_button);
        dleButton = (Button) view.findViewById(R.id.dle_button);
        dc1Button = (Button) view.findViewById(R.id.dc1_button);
        dc2Button = (Button) view.findViewById(R.id.dc2_button);
        dc3Button = (Button) view.findViewById(R.id.dc3_button);
        dc4Button = (Button) view.findViewById(R.id.dc4_button);
        nakButton = (Button) view.findViewById(R.id.nak_button);
        synButton = (Button) view.findViewById(R.id.syn_button);
        etbButton = (Button) view.findViewById(R.id.etb_button);
        canButton = (Button) view.findViewById(R.id.can_button);
        emButton = (Button) view.findViewById(R.id.em_button);
        subButton = (Button) view.findViewById(R.id.sub_button);
        escButton = (Button) view.findViewById(R.id.esc_button);
        fsButton = (Button) view.findViewById(R.id.fs_button);
        gsButton = (Button) view.findViewById(R.id.gs_button);
        rsButton = (Button) view.findViewById(R.id.rs_button);
        usButton = (Button) view.findViewById(R.id.us_button);
        delButton = (Button) view.findViewById(R.id.del_button);

        nulButton.setOnClickListener(this);
        sohButton.setOnClickListener(this);
        stxButton.setOnClickListener(this);
        etxButton.setOnClickListener(this);
        eotButton.setOnClickListener(this);
        enqButton.setOnClickListener(this);
        ackButton.setOnClickListener(this);
        belButton.setOnClickListener(this);
        bsButton.setOnClickListener(this);
        htButton.setOnClickListener(this);
        lfButton.setOnClickListener(this);
        vtButton.setOnClickListener(this);
        ffButton.setOnClickListener(this);
        crButton.setOnClickListener(this);
        soButton.setOnClickListener(this);
        siButton.setOnClickListener(this);
        dleButton.setOnClickListener(this);
        dc1Button.setOnClickListener(this);
        dc2Button.setOnClickListener(this);
        dc3Button.setOnClickListener(this);
        dc4Button.setOnClickListener(this);
        nakButton.setOnClickListener(this);
        synButton.setOnClickListener(this);
        etbButton.setOnClickListener(this);
        canButton.setOnClickListener(this);
        emButton.setOnClickListener(this);
        subButton.setOnClickListener(this);
        escButton.setOnClickListener(this);
        fsButton.setOnClickListener(this);
        gsButton.setOnClickListener(this);
        rsButton.setOnClickListener(this);
        usButton.setOnClickListener(this);
        delButton.setOnClickListener(this);
    }
}
