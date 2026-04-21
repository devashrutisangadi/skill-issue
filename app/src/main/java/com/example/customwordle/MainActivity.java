package com.example.customwordle;

import android.graphics.Color;
import android.os.Bundle;
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
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, catList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        // Toast.makeText(this, "Target: " + targetWord, Toast.LENGTH_LONG).show();  // Remove later
    }

    private void createTiles() {
        int tileSize = (int) (Math.min(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels * 0.6) / 5.2);
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                tiles[row][col] = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                params.width = tileSize;
                params.height = tileSize;
                params.setMargins(6, 6, 6, 6);
                tiles[row][col].setLayoutParams(params);
                tiles[row][col].setGravity(Gravity.CENTER);
                tiles[row][col].setTextSize(32);
                tiles[row][col].setBackgroundColor(Color.parseColor("#ffffff"));
                tiles[row][col].setTextColor(Color.parseColor("#000000"));
                grid.addView(tiles[row][col]);
            }
        }
    }

    private void setupKeyboard() {
        // Letter keys
        String[] letters = {"Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","Z","X","C","V","B","N","M"};
        int[] keyIds = {R.id.keyQ, R.id.keyW, R.id.keyE, R.id.keyR, R.id.keyT, R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
                R.id.keyA, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG, R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL,
                R.id.keyZ, R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB, R.id.keyN, R.id.keyM};

        for (int i = 0; i < letters.length; i++) {
            int id = keyIds[i];
            Button btn = findViewById(id);
            char letter = letters[i].charAt(0);
            btn.setOnClickListener(v -> handleKey(letter));
            keyboardState.put(letter, 0);  // 0=normal,1=gray,2=yellow,3=green
        }

        // ENTER
        findViewById(R.id.keyENTER).setOnClickListener(v -> {
            if (currentCol == 5) submitGuess();
        });
        // DELETE
        findViewById(R.id.keyDELETE).setOnClickListener(v -> {
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
        // Green first
        for (int i = 0; i < 5; i++) {
            if (guess.charAt(i) == target.charAt(i)) {
                result[i] = 2;
                used[i] = true;
                keyboardState.put(guess.charAt(i), 3);  // Green
            }
        }
        // Yellow/Gray
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
        // Remaining gray
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
            Button btn = findKeyButton(letter);
            if (btn != null) {
                int state = keyboardState.get(letter);
                switch (state) {
                    case 3: case 2: btn.setBackgroundTintList(getResources().getColorStateList(R.color.tile_green, null)); break;
                    case 1: btn.setBackgroundTintList(getResources().getColorStateList(R.color.tile_yellow, null)); break;
                    case 0: btn.setBackgroundTintList(getResources().getColorStateList(R.color.tile_gray, null)); break;
                }
            }
        }
    }

    private Button findKeyButton(char letter) {
        int id = getResources().getIdentifier("key" + letter, "id", getPackageName());
        return id != 0 ? findViewById(id) : null;
    }

    private void resetTiles() {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                tiles[row][col].setText("");
                tiles[row][col].setBackgroundColor(Color.parseColor("#ffffff"));
                tiles[row][col].setTextColor(Color.BLACK);
            }
        }
    }

    private void resetKeyboard() {
        String[] letters = {"Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","Z","X","C","V","B","N","M"};
        for (String letter : letters) {
            Button btn = findKeyButton(letter.charAt(0));
            if (btn != null) btn.setBackgroundTintList(null);  // Reset to default
        }
    }

    private void shakeRow() {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);  // Create res/anim/shake.xml later
        grid.startAnimation(shake);
        Toast.makeText(this, "Not a valid word", Toast.LENGTH_SHORT).show();
    }
}