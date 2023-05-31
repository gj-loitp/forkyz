package app.crossword.yourealwaysbe;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import androidx.core.text.HtmlCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import app.crossword.yourealwaysbe.forkyz.databinding.HtmlViewBinding;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.view.recycler.ShowHideOnScroll;

public class HTMLActivity extends ForkyzActivity {
    private AndroidVersionUtils utils
        = AndroidVersionUtils.Factory.getInstance();
    private static final Charset CHARSET = Charset.forName("UTF-8");

    private ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    private HtmlViewBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = HtmlViewBinding.inflate(getLayoutInflater());

        utils.holographic(this);
        utils.finishOnHomeButton(this);
        this.setContentView(binding.getRoot());

        String assetName = this.getIntent().getData().toString();
        startLoadAssetName(assetName);
        binding.backButton.setOnClickListener((v) -> { finish(); });

        setScrollListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getTitle() == null){
            finish();
            return true;
        }
        return false;
    }

    // suppress because ShowHideOnScroll does not consume/handle clicks
    @SuppressWarnings("ClickableViewAccessibility")
    private void setScrollListener() {
        binding.scrollView.setOnTouchListener(
            new ShowHideOnScroll(binding.backButton)
        );
    }

    private void startLoadAssetName(String assetName) {
        executorService.execute(() -> {
            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        getAssets().open(assetName)
                    )
                )
            ) {
                String htmlData
                    = reader.lines().collect(Collectors.joining("\n"));
                handler.post(() -> {
                    binding.content.setText(HtmlCompat.fromHtml(htmlData, 0));
                });
            } catch (IOException e) {
                handler.post(() -> {
                    finish();
                });
            }
        });
    }
}
