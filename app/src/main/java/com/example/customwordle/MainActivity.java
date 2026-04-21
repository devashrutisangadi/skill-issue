package com.example.customwordle;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private Spinner categorySpinner;
    private GridLayout grid;
    private Button startButton;
    private Map<String, List<String>> categories;
    private String targetWord, currentCategory;
    private TextView[][] tiles = new TextView[6][5];
    private int currentRow = 0, currentCol = 0;
    private Map<Character, Integer> keyboardState = new HashMap<>();  // Track gray/yellow/green per letter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        categorySpinner = findViewById(R.id.categorySpinner);
        grid = findViewById(R.id.gridLayout);
        startButton = findViewById(R.id.startButton);

        categories = WordLoader.loadCategories(this);

        if (categories != null) {
            ArrayList<String> catList = new ArrayList<>(categories.keySet());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, catList);
            adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
            categorySpinner.setAdapter(adapter);
        }

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        startButton.setOnClickListener(v -> startGame());

        createTiles();
        setupKeyboard();
    }

    private void startGame() {
        if (currentCategory == null) {
            Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> words = categories.get(currentCategory);
        Random rand = new Random();
        targetWord = words.get(rand.nextInt(words.size())).toUpperCase();
        currentRow = currentCol = 0;
        keyboardState.clear();
        resetTiles();
        resetKeyboard();
        startButton.setEnabled(false);
    }

    private void createTiles() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int horizontalPadding = dpToPx(24);
        int totalTileSpacing = dpToPx(24);
        int tileSize = (screenWidth - horizontalPadding - totalTileSpacing) / 5;
        tileSize = Math.min(tileSize, dpToPx(62));

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                tiles[row][col] = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                params.width = tileSize;
                params.height = tileSize;
                params.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
                tiles[row][col].setLayoutParams(params);
                tiles[row][col].setGravity(Gravity.CENTER);
                tiles[row][col].setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                tiles[row][col].setTypeface(Typeface.DEFAULT_BOLD);
                tiles[row][col].setBackgroundResource(R.drawable.tile_background);
                tiles[row][col].setTextColor(getResources().getColor(R.color.white, null));
                grid.addView(tiles[row][col]);
            }
        }
    }

    private void setupKeyboard() {
        String[] letters = {"Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","Z","X","C","V","B","N","M"};
        int[] keyIds = {R.id.keyQ, R.id.keyW, R.id.keyE, R.id.keyR, R.id.keyT, R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
                R.id.keyA, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG, R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL,
                R.id.keyZ, R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB, R.id.keyN, R.id.keyM};

        for (int i = 0; i < letters.length; i++) {
            int id = keyIds[i];
            TextView btn = findViewById(id);
            char letter = letters[i].charAt(0);
            btn.setText(String.valueOf(letter));
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setGravity(Gravity.CENTER);
            btn.setBackgroundResource(R.drawable.key_normal);
            btn.setBackgroundTintList(null);
            btn.setTextColor(getResources().getColor(R.color.white, null));
            btn.setOnClickListener(v -> handleKey(letter));
            keyboardState.put(letter, 0);
        }

        TextView enterButton = findViewById(R.id.keyENTER);
        enterButton.setText("ENTER");
        enterButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        enterButton.setTypeface(Typeface.DEFAULT_BOLD);
        enterButton.setGravity(Gravity.CENTER);
        enterButton.setBackgroundResource(R.drawable.key_enter);
        enterButton.setBackgroundTintList(null);
        enterButton.setTextColor(getResources().getColor(R.color.white, null));
        enterButton.setOnClickListener(v -> {
            if (currentCol == 5) submitGuess();
        });

        TextView deleteButton = findViewById(R.id.keyDELETE);
        deleteButton.setText("DEL");
        deleteButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        deleteButton.setTypeface(Typeface.DEFAULT_BOLD);
        deleteButton.setGravity(Gravity.CENTER);
        deleteButton.setBackgroundResource(R.drawable.key_enter);
        deleteButton.setBackgroundTintList(null);
        deleteButton.setTextColor(getResources().getColor(R.color.white, null));
        deleteButton.setOnClickListener(v -> {
            if (currentCol > 0) {
                currentCol--;
                tiles[currentRow][currentCol].setText("");
            }
        });
    }

    private void handleKey(char key) {
        if (currentCol >= 5) return;
        tiles[currentRow][currentCol].setText(String.valueOf(key));
        currentCol++;
    }

    private void submitGuess() {
        if (currentCol != 5) return;

        StringBuilder sb = new StringBuilder();
        for (int col = 0; col < 5; col++) {
            sb.append(tiles[currentRow][col].getText());
        }
        String guess = sb.toString();

        if (guess.length() != 5 || !categories.get(currentCategory).contains(guess.toLowerCase())) {
            shakeRow();
            return;
        }

        int[] result = checkGuess(guess, targetWord);
        colorTiles(result);
        updateKeyboard(result, guess);

        currentRow++;
        currentCol = 0;

        if (guess.equals(targetWord)) {
            Toast.makeText(this, "Genius! 🎉", Toast.LENGTH_LONG).show();
            startButton.setEnabled(true);
            return;
        }
        if (currentRow == 6) {
            Toast.makeText(this, "Lost! The word was: " + targetWord, Toast.LENGTH_LONG).show();
            startButton.setEnabled(true);
        }
    }

    private int[] checkGuess(String guess, String target) {
        int[] result = new int[5];
        boolean[] used = new boolean[5];

        for (int i = 0; i < 5; i++) {
            if (guess.charAt(i) == target.charAt(i)) {
                result[i] = 2;
                used[i] = true;
                keyboardState.put(guess.charAt(i), 3);
            }
        }

        for (int i = 0; i < 5; i++) {
            if (result[i] == 2) continue;
            for (int j = 0; j < 5; j++) {
                if (!used[j] && Character.toUpperCase(guess.charAt(i)) == Character.toUpperCase(target.charAt(j))) {
                    result[i] = 1;
                    used[j] = true;
                    int state = Math.max(keyboardState.getOrDefault(Character.toUpperCase(guess.charAt(i)), 0), 2);
                    keyboardState.put(Character.toUpperCase(guess.charAt(i)), state);
                    break;
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            if (result[i] == 0) {
                keyboardState.put(Character.toUpperCase(guess.charAt(i)), 1);
            }
        }
        return result;
    }

    private void colorTiles(int[] result) {
        for (int i = 0; i < 5; i++) {
            TextView tile = tiles[currentRow][i];
            int color;
            switch (result[i]) {
                case 2: color = getResources().getColor(R.color.tile_green, null); break;
                case 1: color = getResources().getColor(R.color.tile_yellow, null); break;
                default: color = getResources().getColor(R.color.tile_gray, null);
            }
            tile.setBackgroundColor(color);
            tile.setTextColor(Color.WHITE);
        }
    }

    private void updateKeyboard(int[] result, String guess) {
        for (int i = 0; i < 5; i++) {
            char letter = Character.toUpperCase(guess.charAt(i));
            TextView btn = findKeyButton(letter);
            if (btn != null) {
                int state = keyboardState.get(letter);
                switch (state) {
                    case 3:
                        btn.setBackgroundTintList(null);
                        btn.setBackgroundColor(getResources().getColor(R.color.tile_green, null));
                        btn.setTextColor(getResources().getColor(R.color.white, null));
                        break;
                    case 2:
                        btn.setBackgroundTintList(null);
                        btn.setBackgroundColor(getResources().getColor(R.color.tile_yellow, null));
                        btn.setTextColor(getResources().getColor(R.color.white, null));
                        break;
                    case 1:
                        btn.setBackgroundTintList(null);
                        btn.setBackgroundColor(getResources().getColor(R.color.tile_gray, null));
                        btn.setTextColor(getResources().getColor(R.color.white, null));
                        break;
                    default:
                        btn.setBackgroundResource(R.drawable.key_normal);
                        btn.setBackgroundTintList(null);
                        btn.setTextColor(getResources().getColor(R.color.white, null));
                        break;
                }
            }
        }
    }

    private TextView findKeyButton(char letter) {
        int id = getResources().getIdentifier("key" + letter, "id", getPackageName());
        return id != 0 ? findViewById(id) : null;
    }

    private void resetTiles() {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                tiles[row][col].setText("");
                tiles[row][col].setBackgroundResource(R.drawable.tile_background);
                tiles[row][col].setTextColor(getResources().getColor(R.color.white, null));
            }
        }
    }

    private void resetKeyboard() {
        String[] letters = {"Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","Z","X","C","V","B","N","M"};
        for (String letter : letters) {
            TextView btn = findKeyButton(letter.charAt(0));
            if (btn != null) {
                btn.setBackgroundResource(R.drawable.key_normal);
                btn.setBackgroundTintList(null);
                btn.setTextColor(getResources().getColor(R.color.white, null));
            }
        }

        TextView enterButton = findViewById(R.id.keyENTER);
        if (enterButton != null) {
            enterButton.setBackgroundResource(R.drawable.key_enter);
            enterButton.setBackgroundTintList(null);
            enterButton.setTextColor(getResources().getColor(R.color.white, null));
        }

        TextView deleteButton = findViewById(R.id.keyDELETE);
        if (deleteButton != null) {
            deleteButton.setBackgroundResource(R.drawable.key_enter);
            deleteButton.setBackgroundTintList(null);
            deleteButton.setTextColor(getResources().getColor(R.color.white, null));
        }
    }

    private void shakeRow() {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        grid.startAnimation(shake);
        Toast.makeText(this, "Not a valid word", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}
