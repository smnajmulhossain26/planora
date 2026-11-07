package com.planora.app.calc;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.planora.app.databinding.FragmentCalculatorBinding;
import com.planora.app.util.Prefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Scientific calculator with live preview, like a real phone calculator. */
public class CalculatorFragment extends Fragment {
    private FragmentCalculatorBinding b;
    private final StringBuilder expr = new StringBuilder();
    private boolean deg = true;
    private String ans = "";
    private boolean justEvaluated;
    private Prefs prefs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentCalculatorBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        prefs = new Prefs(requireContext());
        attach(b.keys);
        render();
    }

    private void attach(ViewGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c instanceof Button) c.setOnClickListener(this::onKey);
            else if (c instanceof ViewGroup) attach((ViewGroup) c);
        }
    }

    private void onKey(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        String k = ((Button) v).getText().toString();
        switch (k) {
            case "AC": expr.setLength(0); justEvaluated = false; break;
            case "⌫":
                if (justEvaluated) { expr.setLength(0); justEvaluated = false; }
                else if (expr.length() > 0) expr.setLength(expr.length() - 1);
                break;
            case "DEG": case "RAD":
                deg = !deg;
                ((Button) v).setText(deg ? "DEG" : "RAD");
                b.tvMode.setText(deg ? "DEG" : "RAD");
                break;
            case "=": evaluate(); return;
            case "History": showHistory(); return;
            case "Ans": if (!ans.isEmpty()) append(ans); break;
            case "±": toggleSign(); break;
            case "x²": append("^2"); break;
            case "sin": case "cos": case "tan": case "asin": case "acos": case "atan": case "ln": case "log":
                append(k + "("); break;
            case "√": append("√("); break;
            default: append(k);
        }
        render();
    }

    private void toggleSign() {
        if (expr.length() == 0) return;
        String e = expr.toString();
        if (e.startsWith("-(") && e.endsWith(")")) {
            expr.setLength(0);
            expr.append(e, 2, e.length() - 1);
        } else {
            expr.insert(0, "-(").append(")");
        }
        justEvaluated = false;
    }

    private void append(String t) {
        if (justEvaluated && t.length() > 0) {
            char c = t.charAt(0);
            // a new number/function starts a fresh calculation; an operator continues with the result
            if (Character.isDigit(c) || c == '.' || Character.isLetter(c) || c == '√' || c == 'π' || c == '(') expr.setLength(0);
        }
        justEvaluated = false;
        expr.append(t);
    }

    /** Shows the current input big and a live result preview above it. */
    private void render() {
        if (b == null) return;
        String e = expr.toString();
        if (justEvaluated) return; // result screen is drawn by evaluate()
        b.tvMain.setText(e.isEmpty() ? "0" : e);
        String preview = "";
        if (!e.isEmpty()) {
            try {
                String r = Calc.eval(e, deg);
                if (!r.equals(e)) preview = "= " + r;
            } catch (Exception ignored) { }
        }
        b.tvTop.setText(preview);
    }

    private void evaluate() {
        if (expr.length() == 0) return;
        try {
            String r = Calc.eval(expr.toString(), deg);
            addHistory(expr + " = " + r);
            b.tvTop.setText(expr + " =");
            b.tvMain.setText(r);
            ans = r;
            expr.setLength(0);
            expr.append(r);
            justEvaluated = true;
        } catch (Exception e) {
            b.tvTop.setText(e.getMessage() == null ? "Error" : e.getMessage());
        }
    }

    private void addHistory(String line) {
        List<String> list = new ArrayList<>();
        String h = prefs.calcHistory();
        if (!h.isEmpty()) list.addAll(Arrays.asList(h.split("\n")));
        list.add(0, line);
        while (list.size() > 30) list.remove(list.size() - 1);
        prefs.setCalcHistory(String.join("\n", list));
    }

    private void showHistory() {
        String h = prefs.calcHistory();
        if (h.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext()).setTitle("History").setMessage("No calculations yet.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String[] items = h.split("\n");
        new MaterialAlertDialogBuilder(requireContext()).setTitle("History")
                .setItems(items, (d, i) -> {
                    String line = items[i];
                    int eq = line.lastIndexOf(" = ");
                    if (eq > 0) {
                        expr.setLength(0);
                        expr.append(line.substring(eq + 3));
                        justEvaluated = false;
                        render();
                    }
                })
                .setPositiveButton("Close", null)
                .setNeutralButton("Clear", (d, w) -> prefs.setCalcHistory("")).show();
    }
}
