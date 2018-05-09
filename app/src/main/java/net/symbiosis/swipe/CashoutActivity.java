package net.symbiosis.swipe;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import net.symbiosis.swipe.common.BTResponseObject;
import net.symbiosis.swipe.dto.CurrencyType;

import java.util.ArrayList;
import java.util.Hashtable;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static net.symbiosis.swipe.TabCashout.getCashoutAccount;
import static net.symbiosis.swipe.TabCashout.getCashoutDetails;
import static net.symbiosis.swipe.common.ActivityCommon.cashoutTransaction;
import static net.symbiosis.swipe.common.ActivityCommon.getCurrencies;
import static net.symbiosis.swipe.common.ActivityCommon.getDeviceIMEI;
import static net.symbiosis.swipe.common.ActivityCommon.getUserDetails;
import static net.symbiosis.swipe.common.BTResponseCode.SUCCESS;
import static net.symbiosis.swipe.common.Validator.isNullOrEmpty;
import static net.symbiosis.swipe.common.Validator.isValidCardPin;

public class CashoutActivity extends AppCompatActivity {

    private CashoutActivity cashoutActivity;
    private ProgressBar progressBar;
    private TextView txvCashoutAccountType;
    private TextView txvCashoutAccountDetails;
    private Spinner spnChooseCurrencyType;
    private EditText edtCashoutAmount;
    private EditText edtCashoutReference;
    private EditText edtCashoutPin;
    private Button btnCashout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashout);
        cashoutActivity = this;
        progressBar = (ProgressBar) findViewById(R.id.prgNetwork);

        txvCashoutAccountType = (TextView) findViewById(R.id.txvCashoutAccountType);
        txvCashoutAccountDetails = (TextView) findViewById(R.id.txvCashoutAccountDetails);
        spnChooseCurrencyType = (Spinner) findViewById(R.id.spnChooseCurrencyType);
        edtCashoutAmount = (EditText) findViewById(R.id.edtCashoutAmount);
        edtCashoutReference = (EditText) findViewById(R.id.edtCashoutReference);
        edtCashoutPin = (EditText) findViewById(R.id.edtCashoutPin);
        btnCashout = (Button) findViewById(R.id.btnCashout);

        btnCashout.setOnClickListener(new CompleteCashoutListener());

        populateCurrencies();
        populateCashoutDetails();
    }

    private void populateCurrencies() {

        progressBar.setVisibility(View.VISIBLE);
        ArrayList<CurrencyType> currencyTypes = getCurrencies(this);
        ArrayAdapter<CharSequence> currencyTypeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item);

        for (CurrencyType currencyType : currencyTypes) {
            currencyTypeAdapter.add(currencyType.getCurrencyTypeName());
        }

        progressBar.setVisibility(View.GONE);

        spnChooseCurrencyType.setAdapter(currencyTypeAdapter);
        spnChooseCurrencyType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getCashoutDetails().setCashoutCurrency((String)spnChooseCurrencyType.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }


    private void populateCashoutDetails() {

        txvCashoutAccountType.setText(format("Cashout to %s : %s : %s",
            getCashoutAccount().getAccountNickName(),
            getCashoutAccount().getFinancialInstitution().getInstitutionName(),
            getCashoutAccount().getAccountNumber())
        );

        StringBuilder accountDetails = new StringBuilder();

        if (!isNullOrEmpty(getCashoutAccount().getAccountName())) {
            accountDetails.append(format("Account Name: %s\n", getCashoutAccount().getAccountName()));
        }
        if (!isNullOrEmpty(getCashoutAccount().getAccountBranchCode())) {
            accountDetails.append(format("Account Branch: %s\n", getCashoutAccount().getAccountBranchCode()));
        }
        if (!isNullOrEmpty(getCashoutAccount().getAccountEmail())) {
            accountDetails.append(format("Account Email: %s\n", getCashoutAccount().getAccountEmail()));
        }
        if (!isNullOrEmpty(getCashoutAccount().getAccountPhone())) {
            accountDetails.append(format("Account Phone: %s\n", getCashoutAccount().getAccountPhone()));
        }

        txvCashoutAccountDetails.setText(accountDetails.toString());
    }

    class CompleteCashoutListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (!isValidCardPin(edtCashoutPin.getText().toString())) {
                edtCashoutPin.setError("This pin is not valid");
                return;
            }
            getCashoutDetails().setCashoutPin(parseInt(edtCashoutPin.getText().toString()));

            Hashtable<String, String> cashoutDetails = new Hashtable<>();
            cashoutDetails.put("userId", valueOf(getUserDetails(cashoutActivity).getBtUserId()));
            cashoutDetails.put("deviceId", getDeviceIMEI(cashoutActivity));
            cashoutDetails.put("amount", edtCashoutAmount.getText().toString());
            cashoutDetails.put("reference", edtCashoutReference.getText().toString());
            cashoutDetails.put("cashoutAccountId", valueOf(getCashoutAccount().getCashoutAccountId()));
            cashoutDetails.put("pin", edtCashoutPin.getText().toString());

            BTResponseObject<Double> cashoutResponse = cashoutTransaction(cashoutActivity, cashoutDetails);

            if (cashoutResponse.getResponseCode().equals(SUCCESS)) {
                new AlertDialog.Builder(cashoutActivity)
                    .setTitle("Cashout Complete")
                    .setMessage("Cashout successfully submitted for processing.")
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
                new AlertDialog.Builder(cashoutActivity)
                    .setTitle("Cashout Failed")
                    .setMessage(cashoutResponse.getMessage())
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