
package app.crossword.yourealwaysbe.net;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

public class ChatGPTHelp {
    private static final String CHAT_GPT_API_URL
        = "https://api.openai.com/v1/completions";
    private static final String CHAT_GPT_MODEL = "text-davinci-003";
    private static final String CHAT_GPT_RESPONSE_CHOICES = "choices";
    private static final String CHAT_GPT_RESPONSE_TEXT = "text";
    private static final String PREF_CHAT_GPT_API_KEY = "chatGPTAPIKey";
    private static final double CHAT_GPT_TEMPERATURE = 1.0;
    private static final int CHAT_GPT_MAX_TOKENS = 500;
    private static final int HTTP_OK_RESPONSE = 200;
    private static final int QUERY_TIMEOUT = 30000;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public static boolean isEnabled(Context context) {
        String apiKey = getAPIKey(context);
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public void requestHelpForCurrentClue(
        Context context, Playboard board, Consumer<String> callback
    ) {
        String apiKey = getAPIKey(context);
        if (apiKey == null) {
            callback.accept(null);
        } else {
            String query = makeQuery(context, board);
            if (query == null)
                callback.accept(null);
            else
                makeRequest(apiKey, query, callback);
        }
    }

    private static String getAPIKey(Context context) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_CHAT_GPT_API_KEY, null);
    }

    private void makeRequest(
        String apiKey, String query, Consumer<String> callback
    ) {
        executor.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                    new URL(CHAT_GPT_API_URL).openConnection();
                conn.setConnectTimeout(QUERY_TIMEOUT);
                conn.setReadTimeout(QUERY_TIMEOUT);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);

                sendQueryData(conn, query);

                callback.accept(getQueryResponse(conn));
            } catch (IOException | JSONException e) {
                callback.accept(null);
            }
        });
    }

    private void sendQueryData(HttpURLConnection conn, String query)
            throws IOException, JSONException {
        JSONObject data = new JSONObject();
        data.put("model", CHAT_GPT_MODEL);
        data.put("prompt", query);
        data.put("max_tokens", CHAT_GPT_MAX_TOKENS);
        data.put("temperature", CHAT_GPT_TEMPERATURE);

        try (
            BufferedOutputStream os
                = new BufferedOutputStream(conn.getOutputStream())
        ) {
            byte[] bytes
                = data.toString().getBytes("utf-8");
            os.write(bytes, 0, bytes.length);
        }
    }

    private String getQueryResponse(HttpURLConnection conn)
            throws IOException, JSONException {
        if (conn.getResponseCode() != HTTP_OK_RESPONSE)
            return null;

        try (
            BufferedReader is = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            )
        ) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = is.readLine()) != null)
                sb.append(line);

            JSONObject response = new JSONObject(sb.toString());
            JSONArray choices
                = response.getJSONArray(CHAT_GPT_RESPONSE_CHOICES);
            JSONObject first = choices.getJSONObject(0);
            return first.getString(CHAT_GPT_RESPONSE_TEXT).trim();
        }
    }

    private String makeQuery(Context context, Playboard board) {
        String blank = context.getString(R.string.share_clue_blank_box);

        Puzzle puz = board.getPuzzle();
        Clue clue = board == null ? null : board.getClue();
        if (clue == null)
            return null;

        String hint = clue.getHint();
        String response = null;
        String solution = null;

        Zone zone = clue.getZone();
        if (zone != null && puz != null) {
            boolean hasResponse = false;
            StringBuilder responseBuilder = new StringBuilder();
            boolean hasSolution = false;
            StringBuilder solutionBuilder = new StringBuilder();

            for (Position pos : zone) {
                Box box = puz.checkedGetBox(pos);
                if (pos != null) {
                    if (!box.isBlank()) {
                        hasResponse = true;
                        responseBuilder.append(box.getResponse());
                    } else {
                        responseBuilder.append(blank);
                    }

                    if (box.hasSolution()) {
                        hasSolution = true;
                        solutionBuilder.append(box.getSolution());
                    } else {
                        solutionBuilder.append(blank);
                    }
                }
            }

            if (hasSolution)
                solution = solutionBuilder.toString();
            if (hasResponse)
                response = responseBuilder.toString();
        }

        String query;
        if (response != null && solution != null) {
            return context.getString(
                R.string.help_query_solution_and_response,
                hint, response, solution
            );
        } else if (response != null) {
            return context.getString(
                R.string.help_query_just_response,
                hint, response
            );
        } else if (solution != null) {
            return context.getString(
                R.string.help_query_just_solution,
                hint, solution
            );
        } else {
            return context.getString(R.string.help_query_just_clue, clue);
        }
    }
}
