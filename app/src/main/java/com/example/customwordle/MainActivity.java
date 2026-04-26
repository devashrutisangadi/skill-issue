package com.example.customwordle;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int MIN_LETTER_COUNT = 3;
    private static final long REVEAL_STAGGER_MS = 220L;
    private static final long REVEAL_HALF_DURATION_MS = 130L;

    private Spinner categorySpinner;
    private GridLayout grid;
    private HorizontalScrollView boardScroll;
    private View keyboardContainer;
    private TextView introText;
    private Button startButton;

    private Map<String, List<String>> categories;
    private String targetWord;
    private String currentCategory;

    private TextView[][] tiles = new TextView[0][0];
    private Set<String> validGuesses = new HashSet<>();
    private Map<Character, Integer> keyboardState = new HashMap<>(); // 0 normal, 1 gray, 2 yellow, 3 green

    private int wordLength = 5;
    private int maxRows = 6;
    private int currentRow = 0;
    private int currentCol = 0;
    private boolean isRowAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        categorySpinner = findViewById(R.id.categorySpinner);
        grid = findViewById(R.id.gridLayout);
        boardScroll = findViewById(R.id.boardScroll);
        keyboardContainer = findViewById(R.id.keyboardContainer);
        introText = findViewById(R.id.introText);
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        startButton.setOnClickListener(v -> startGame());

        setupKeyboard();
        showIntroState();
    }

    private void startGame() {
        if (currentCategory == null) {
            Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> words = categories.get(currentCategory);
        if (words == null || words.isEmpty()) {
            Toast.makeText(this, "No words in this category", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> playable = getPlayableEntries(words);
        if (playable.isEmpty()) {
            Toast.makeText(this, "No playable entries found", Toast.LENGTH_SHORT).show();
            return;
        }

        Random rand = new Random();
        targetWord = playable.get(rand.nextInt(playable.size()));

        wordLength = targetWord.length();
        maxRows = calculateMaxRows(getLetterCount(targetWord));
        validGuesses = buildValidGuesses(playable, targetWord);

        currentRow = 0;
        currentCol = 0;
        isRowAnimating = false;
        keyboardState.clear();

        showGameState();
        createTiles();
        resetKeyboard();
        currentCol = findNextEditableCol(0);
        centerViewportOnCol(currentCol, false);

        startButton.setEnabled(false);
    }

    private void showIntroState() {
        if (introText != null) {
            introText.setVisibility(View.VISIBLE);
        }
        if (boardScroll != null) {
            boardScroll.setVisibility(View.GONE);
        }
        if (keyboardContainer != null) {
            keyboardContainer.setVisibility(View.GONE);
        }
    }

    private void showGameState() {
        if (introText != null) {
            introText.setVisibility(View.GONE);
        }
        if (boardScroll != null) {
            boardScroll.setVisibility(View.VISIBLE);
        }
        if (keyboardContainer != null) {
            keyboardContainer.setVisibility(View.VISIBLE);
        }
    }

    private void createTiles() {
        grid.removeAllViews();
        grid.setColumnCount(wordLength);
        grid.setRowCount(maxRows);
        tiles = new TextView[maxRows][wordLength];

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int gridHeightBudget = (int) (screenHeight * 0.40f);
        int verticalGaps = maxRows * dpToPx(6);
        int tileSize = (gridHeightBudget - verticalGaps) / Math.max(maxRows, 1);
        tileSize = Math.max(dpToPx(28), Math.min(tileSize, dpToPx(56)));
        int spaceWidth = Math.max(dpToPx(8), tileSize / 3);

        for (int row = 0; row < maxRows; row++) {
            for (int col = 0; col < wordLength; col++) {
                TextView tile = new TextView(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                params.width = isFixedSpace(col) ? spaceWidth : tileSize;
                params.height = tileSize;
                params.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));

                tile.setLayoutParams(params);
                tile.setGravity(Gravity.CENTER);
                tile.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                tile.setIncludeFontPadding(false);
                tile.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                tile.setTypeface(Typeface.DEFAULT_BOLD);

                if (isFixedSpace(col)) {
                    tile.setText(" ");
                    tile.setBackgroundColor(getResources().getColor(R.color.dark_bg, null));
                    tile.setTextColor(getResources().getColor(R.color.dark_bg, null));
                } else {
                    tile.setText("");
                    tile.setBackgroundResource(R.drawable.tile_background);
                    tile.setTextColor(getResources().getColor(R.color.white, null));
                }

                tiles[row][col] = tile;
                grid.addView(tile);
            }
        }
    }

    private void setupKeyboard() {
        String[] letters = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "A", "S", "D", "F", "G", "H", "J", "K", "L", "Z", "X", "C", "V", "B", "N", "M"};
        int[] keyIds = {R.id.keyQ, R.id.keyW, R.id.keyE, R.id.keyR, R.id.keyT, R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
                R.id.keyA, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG, R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL,
                R.id.keyZ, R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB, R.id.keyN, R.id.keyM};

        for (int i = 0; i < letters.length; i++) {
            TextView btn = findViewById(keyIds[i]);
            char letter = letters[i].charAt(0);

            btn.setText(String.valueOf(letter));
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setGravity(Gravity.CENTER);
            btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            btn.setIncludeFontPadding(false);
            btn.setBackgroundResource(R.drawable.key_normal);
            btn.setBackgroundTintList(null);
            btn.setTextColor(getResources().getColor(R.color.white, null));

            btn.setOnClickListener(v -> handleKey(letter));
            keyboardState.put(letter, 0);
        }

        TextView enterButton = findViewById(R.id.keyENTER);
        enterButton.setText("ENTER");
        enterButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        enterButton.setTypeface(Typeface.DEFAULT_BOLD);
        enterButton.setGravity(Gravity.CENTER);
        enterButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        enterButton.setIncludeFontPadding(false);
        enterButton.setBackgroundResource(R.drawable.key_enter);
        enterButton.setBackgroundTintList(null);
        enterButton.setTextColor(getResources().getColor(R.color.white, null));
        enterButton.setOnClickListener(v -> submitGuess());

        TextView deleteButton = findViewById(R.id.keyDELETE);
        deleteButton.setText("DEL");
        deleteButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        deleteButton.setTypeface(Typeface.DEFAULT_BOLD);
        deleteButton.setGravity(Gravity.CENTER);
        deleteButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        deleteButton.setIncludeFontPadding(false);
        deleteButton.setBackgroundResource(R.drawable.key_enter);
        deleteButton.setBackgroundTintList(null);
        deleteButton.setTextColor(getResources().getColor(R.color.white, null));
        deleteButton.setOnClickListener(v -> handleDelete());
    }

    private void handleKey(char key) {
        if (targetWord == null || currentRow >= maxRows || isRowAnimating) {
            return;
        }

        currentCol = findNextEditableCol(currentCol);
        if (currentCol >= wordLength) {
            return;
        }

        tiles[currentRow][currentCol].setText(String.valueOf(key));
        currentCol = findNextEditableCol(currentCol + 1);
        int focusCol = Math.min(currentCol, wordLength - 1);
        centerViewportOnCol(focusCol, true);
    }

    private void handleDelete() {
        if (targetWord == null || currentRow >= maxRows || isRowAnimating) {
            return;
        }

        int prev = findPreviousEditableCol(currentCol - 1);
        if (prev >= 0) {
            tiles[currentRow][prev].setText("");
            currentCol = prev;
            centerViewportOnCol(currentCol, true);
        }
    }

    private void submitGuess() {
        if (isRowAnimating) {
            return;
        }

        if (targetWord == null) {
            Toast.makeText(this, "Start a game first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentRow >= maxRows) {
            return;
        }

        if (!isRowComplete(currentRow)) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show();
            shakeRow();
            return;
        }

        String guess = buildGuessFromRow(currentRow);
        if (!validGuesses.contains(guess)) {
            Toast.makeText(this, "Not in word list", Toast.LENGTH_SHORT).show();
            shakeRow();
            return;
        }

        int[] result = checkGuess(guess, targetWord);
        isRowAnimating = true;
        animateRowReveal(currentRow, result, () -> {
            updateKeyboard(guess, result);
            isRowAnimating = false;

            if (guess.equals(targetWord)) {
                Toast.makeText(this, "Genius!", Toast.LENGTH_LONG).show();
                startButton.setEnabled(true);
                return;
            }

            currentRow++;
            currentCol = findNextEditableCol(0);
            centerViewportOnCol(currentCol, true);

            if (currentRow == maxRows) {
                Toast.makeText(this, "Lost! The word was: " + targetWord, Toast.LENGTH_LONG).show();
                startButton.setEnabled(true);
            }
        });
    }

    private boolean isRowComplete(int row) {
        for (int col = 0; col < wordLength; col++) {
            if (isFixedSpace(col)) {
                continue;
            }
            if (tiles[row][col].getText().toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String buildGuessFromRow(int row) {
        StringBuilder guess = new StringBuilder();
        for (int col = 0; col < wordLength; col++) {
            if (isFixedSpace(col)) {
                guess.append(' ');
            } else {
                String letter = tiles[row][col].getText().toString();
                guess.append(letter.isEmpty() ? ' ' : Character.toUpperCase(letter.charAt(0)));
            }
        }
        return guess.toString();
    }

    private int[] checkGuess(String guess, String target) {
        int[] result = new int[wordLength];
        Map<Character, Integer> remaining = new HashMap<>();

        for (int i = 0; i < wordLength; i++) {
            if (target.charAt(i) == ' ') {
                result[i] = -1;
            } else {
                result[i] = 0;
            }
        }

        for (int i = 0; i < wordLength; i++) {
            char t = target.charAt(i);
            if (t == ' ') {
                continue;
            }

            char g = guess.charAt(i);
            if (g == t) {
                result[i] = 2;
            } else {
                remaining.put(t, remaining.getOrDefault(t, 0) + 1);
            }
        }

        for (int i = 0; i < wordLength; i++) {
            if (result[i] != 0) {
                continue;
            }

            char g = guess.charAt(i);
            int count = remaining.getOrDefault(g, 0);
            if (count > 0) {
                result[i] = 1;
                remaining.put(g, count - 1);
            }
        }

        for (int i = 0; i < wordLength; i++) {
            if (result[i] == -1) {
                continue;
            }

            char g = guess.charAt(i);
            if (result[i] == 2) {
                updateKeyboardState(g, 3);
            } else if (result[i] == 1) {
                updateKeyboardState(g, 2);
            } else {
                updateKeyboardState(g, 1);
            }
        }

        return result;
    }

    private void colorTiles(int[] result) {
        for (int i = 0; i < wordLength; i++) {
            TextView tile = tiles[currentRow][i];
            applyTileState(tile, result[i]);
        }
    }

    private void updateKeyboard(String guess, int[] result) {
        for (int i = 0; i < wordLength; i++) {
            if (result[i] == -1) {
                continue;
            }

            char letter = guess.charAt(i);
            TextView btn = findKeyView(letter);
            if (btn == null) {
                continue;
            }

            int state = keyboardState.getOrDefault(letter, 0);
            switch (state) {
                case 3:
                    btn.setBackgroundColor(getResources().getColor(R.color.tile_green, null));
                    btn.setTextColor(getResources().getColor(R.color.white, null));
                    break;
                case 2:
                    btn.setBackgroundColor(getResources().getColor(R.color.tile_yellow, null));
                    btn.setTextColor(getResources().getColor(R.color.white, null));
                    break;
                case 1:
                    btn.setBackgroundColor(getResources().getColor(R.color.tile_gray, null));
                    btn.setTextColor(getResources().getColor(R.color.white, null));
                    break;
                default:
                    btn.setBackgroundResource(R.drawable.key_normal);
                    btn.setTextColor(getResources().getColor(R.color.white, null));
                    break;
            }
        }
    }

    private void updateKeyboardState(char letter, int newState) {
        int currentState = keyboardState.getOrDefault(letter, 0);
        if (newState > currentState) {
            keyboardState.put(letter, newState);
        }
    }

    private TextView findKeyView(char letter) {
        int id = getResources().getIdentifier("key" + Character.toUpperCase(letter), "id", getPackageName());
        return id != 0 ? findViewById(id) : null;
    }

    private void resetKeyboard() {
        String[] letters = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "A", "S", "D", "F", "G", "H", "J", "K", "L", "Z", "X", "C", "V", "B", "N", "M"};
        for (String letter : letters) {
            TextView btn = findKeyView(letter.charAt(0));
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

    private boolean isFixedSpace(int col) {
        return targetWord != null && col >= 0 && col < targetWord.length() && targetWord.charAt(col) == ' ';
    }

    private int findNextEditableCol(int fromCol) {
        int col = Math.max(fromCol, 0);
        while (col < wordLength && isFixedSpace(col)) {
            col++;
        }
        return col;
    }

    private int findPreviousEditableCol(int fromCol) {
        int col = Math.min(fromCol, wordLength - 1);
        while (col >= 0 && isFixedSpace(col)) {
            col--;
        }
        return col;
    }

    private void centerViewportOnCol(int col, boolean smooth) {
        if (boardScroll == null || tiles.length == 0 || wordLength == 0) {
            return;
        }

        int row = Math.max(0, Math.min(currentRow, maxRows - 1));
        int safeCol = Math.max(0, Math.min(col, wordLength - 1));
        TextView anchor = tiles[row][safeCol];
        if (anchor == null) {
            return;
        }

        boardScroll.post(() -> {
            View content = boardScroll.getChildAt(0);
            if (content == null) {
                return;
            }

            int anchorXInScroll = anchor.getLeft() + grid.getLeft();
            int anchorCenter = anchorXInScroll + (anchor.getWidth() / 2);
            int targetScrollX = anchorCenter - (boardScroll.getWidth() / 2);
            int maxScroll = Math.max(0, content.getWidth() - boardScroll.getWidth());
            targetScrollX = Math.max(0, Math.min(targetScrollX, maxScroll));

            if (smooth) {
                boardScroll.smoothScrollTo(targetScrollX, 0);
            } else {
                boardScroll.scrollTo(targetScrollX, 0);
            }
        });
    }

    private void shakeRow() {
        for (int col = 0; col < wordLength; col++) {
            if (isFixedSpace(col)) {
                continue;
            }
            TextView tile = tiles[currentRow][col];
            if (tile != null) {
                Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
                tile.startAnimation(shake);
            }
        }
    }

    private void animateRowReveal(int row, int[] result, Runnable onComplete) {
        int revealableTiles = 0;
        for (int col = 0; col < wordLength; col++) {
            if (!isFixedSpace(col) && result[col] != -1) {
                revealableTiles++;
            }
        }
        final int totalRevealableTiles = revealableTiles;

        if (totalRevealableTiles == 0) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        final int[] completed = {0};
        int sequence = 0;
        for (int col = 0; col < wordLength; col++) {
            if (isFixedSpace(col) || result[col] == -1) {
                continue;
            }

            TextView tile = tiles[row][col];
            int tileResult = result[col];
            long delay = sequence * REVEAL_STAGGER_MS;
            sequence++;
            animateSingleTileReveal(tile, tileResult, delay, () -> {
                completed[0]++;
                if (completed[0] == totalRevealableTiles && onComplete != null) {
                    onComplete.run();
                }
            });
        }
    }

    private void animateSingleTileReveal(TextView tile, int tileResult, long delayMs, Runnable onEnd) {
        tile.animate().cancel();
        tile.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        tile.setScaleY(1f);
        tile.setPivotY(tile.getHeight() / 2f);

        tile.animate()
                .setStartDelay(delayMs)
                .setDuration(REVEAL_HALF_DURATION_MS)
                .scaleY(0.05f)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        applyTileState(tile, tileResult);
                        tile.animate()
                                .setStartDelay(0)
                                .setDuration(REVEAL_HALF_DURATION_MS)
                                .scaleY(1f)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        tile.setLayerType(View.LAYER_TYPE_NONE, null);
                                        if (onEnd != null) {
                                            onEnd.run();
                                        }
                                    }
                                })
                                .start();
                    }
                })
                .start();
    }

    private void applyTileState(TextView tile, int tileResult) {
        if (tileResult == -1) {
            tile.setText(" ");
            tile.setBackgroundColor(getResources().getColor(R.color.dark_bg, null));
            tile.setTextColor(getResources().getColor(R.color.dark_bg, null));
            return;
        }

        int color;
        switch (tileResult) {
            case 2:
                color = getResources().getColor(R.color.tile_green, null);
                break;
            case 1:
                color = getResources().getColor(R.color.tile_yellow, null);
                break;
            default:
                color = getResources().getColor(R.color.tile_gray, null);
                break;
        }

        tile.setBackgroundColor(color);
        tile.setTextColor(Color.WHITE);
    }

    private List<String> getPlayableEntries(List<String> words) {
        List<String> playable = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String raw : words) {
            String cleaned = sanitizeEntry(raw);
            if (cleaned.isEmpty()) {
                continue;
            }

            int letters = getLetterCount(cleaned);
            if (letters < MIN_LETTER_COUNT) {
                continue;
            }

            if (seen.add(cleaned)) {
                playable.add(cleaned);
            }
        }

        return playable;
    }

    private Set<String> buildValidGuesses(List<String> playable, String patternWord) {
        Set<String> valid = new HashSet<>();
        for (String candidate : playable) {
            if (candidate.length() == patternWord.length() && hasSameSpacePattern(candidate, patternWord)) {
                valid.add(candidate);
            }
        }
        return valid;
    }

    private boolean hasSameSpacePattern(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        for (int i = 0; i < a.length(); i++) {
            if ((a.charAt(i) == ' ') != (b.charAt(i) == ' ')) {
                return false;
            }
        }
        return true;
    }

    private String sanitizeEntry(String rawWord) {
        if (rawWord == null) {
            return "";
        }

        String cleaned = rawWord
                .toUpperCase()
                .replaceAll("[^A-Z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return getLetterCount(cleaned) == 0 ? "" : cleaned;
    }

    private int getLetterCount(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) >= 'A' && value.charAt(i) <= 'Z') {
                count++;
            }
        }
        return count;
    }

    private int calculateMaxRows(int letterCount) {
        int base = letterCount + 1;
        return Math.max(4, Math.min(base, 9));
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}
