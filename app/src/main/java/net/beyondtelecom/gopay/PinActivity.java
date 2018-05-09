package net.beyondtelecom.gopay;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.beyondtelecom.gopay.common.ActivityCommon;
import net.beyondtelecom.gopay.common.BTResponseObject;

import java.util.Hashtable;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static net.beyondtelecom.gopay.TabCashout.getCashoutAccount;
import static net.beyondtelecom.gopay.TabCharge.getTransactionDetails;
import static net.beyondtelecom.gopay.common.ActivityCommon.*;
import static net.beyondtelecom.gopay.common.ActivityCommon.getDeviceIMEI;
import static net.beyondtelecom.gopay.common.ActivityCommon.getUserDetails;
import static net.beyondtelecom.gopay.common.BTResponseCode.SUCCESS;
import static net.beyondtelecom.gopay.common.CommonUtilities.formatDoubleToMoney;
import static net.beyondtelecom.gopay.common.Validator.isNullOrEmpty;
import static net.beyondtelecom.gopay.common.Validator.isValidCardPin;

public class PinActivity extends AppCompatActivity {

	private PinActivity pinActivity;
	private TextView txtCardBytes;
	private TextView txtAmountInfo;
	private TextView txtTransactionReference;
	private Button btnCancelTransaction;
	private Button btnCompleteTransaction;
	private EditText edtCardPin;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pin);
		pinActivity = this;

		txtCardBytes = (TextView) findViewById(R.id.txtCardBytes);
		txtAmountInfo = (TextView) findViewById(R.id.txtAmountInfo);
		txtTransactionReference = (TextView) findViewById(R.id.txtTransactionReference);
		btnCancelTransaction = (Button) findViewById(R.id.btnCancelTransaction);
		btnCancelTransaction.setOnClickListener(new CancelTransactionListener());
		btnCompleteTransaction = (Button) findViewById(R.id.btnCompleteTransaction);
		btnCompleteTransaction.setOnClickListener(new CompleteTransactionListener());
		edtCardPin = (EditText) findViewById(R.id.edtCardPin);
		populateTransactionDetails();
		edtCardPin.requestFocus();
	}

	private void populateTransactionDetails() {

		txtCardBytes.setText(getTransactionDetails().getCardNumber());

		String amount = getTransactionDetails().getTransactionCurrency() + " " +
			formatDoubleToMoney(getTransactionDetails().getTransactionAmount().doubleValue());
		txtAmountInfo.setText(amount);

		if (!isNullOrEmpty(getTransactionDetails().getTransactionReference())) {
			txtTransactionReference.setVisibility(View.VISIBLE);
			txtTransactionReference.setText(format("Reference: %s",
				getTransactionDetails().getTransactionReference()));
		} else {
			txtTransactionReference.setVisibility(View.GONE);
			txtTransactionReference.setText(R.string.label_no_reference);
		}
	}

	class CancelTransactionListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			finish();
		}
	}

	class CompleteTransactionListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (!isValidCardPin(edtCardPin.getText().toString())) {
				edtCardPin.setError("This pin is not valid");
				return;
			}
			getTransactionDetails().setCardPin(Integer.parseInt(edtCardPin.getText().toString()));

			Hashtable<String, String> swipeDetails = new Hashtable<>();
			swipeDetails.put("userId", valueOf(getUserDetails(pinActivity).getBtUserId()));
			swipeDetails.put("deviceId", getDeviceIMEI(pinActivity));
			swipeDetails.put("amount", valueOf(getTransactionDetails().getTransactionAmount().doubleValue()));
			swipeDetails.put("reference", getTransactionDetails().getTransactionReference());
//			swipeDetails.put("cardNumber", getTransactionDetails().getCardNumber());
			swipeDetails.put("cardNumber", "1234567890987");
			swipeDetails.put("cardPin", edtCardPin.getText().toString());

			BTResponseObject<Double> swipeResponse = swipeTransaction(pinActivity, swipeDetails);

			if (swipeResponse.getResponseCode().equals(SUCCESS)) {
				new AlertDialog.Builder(pinActivity)
					.setTitle("Transaction Complete")
					.setMessage("Transaction successfully submitted for processing.")
					.setCancelable(false)
					.setIcon(R.drawable.success)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}
					}).show();
			}
			else {
				new AlertDialog.Builder(pinActivity)
						.setTitle("Transaction Failed")
						.setMessage(swipeResponse.getMessage())
						.setCancelable(false)
						.setIcon(R.drawable.error)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).show();
			}
		}
	}
}
