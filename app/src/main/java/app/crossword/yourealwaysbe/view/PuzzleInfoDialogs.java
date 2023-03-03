package app.crossword.yourealwaysbe.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import app.crossword.yourealwaysbe.ImaginaryTimer;
import app.crossword.yourealwaysbe.PuzzleActivity;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.util.files.PuzHandle;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class PuzzleInfoDialogs {
    public static class Finished extends DialogFragment {
        private static final long SECONDS = 1000;
        private static final long MINUTES = SECONDS * 60;
        private static final long HOURS = MINUTES * 60;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(activity);

            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
            );
            View layout = inflater.inflate(
                R.layout.completed,
                (ViewGroup) activity.findViewById(R.id.finished)
            );

            builder.setTitle(activity.getString(R.string.puzzle_finished_title));
            builder.setView(layout);

            Puzzle puz = getPuzzle();
            if (puz == null)
                return builder.create();

            populateFinishedInfo(puz, layout);

            String shareMessage = getShareMessage(puz);

            // with apologies to the Material guidelines..
            builder.setNegativeButton(
                R.string.share,
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                        sendIntent.setType("text/plain");
                        activity.startActivity(Intent.createChooser(
                            sendIntent, activity.getString(R.string.share_your_time)
                        ));
                    }
                }
            );

            builder.setPositiveButton(R.string.done, null);

            return builder.create();

        }

        protected void populateFinishedInfo(Puzzle puz, View layout) {
            Activity activity = getActivity();

            addCompletedMsg(puz, layout.findViewById(R.id.puzzle_completed_msg));

            long elapsed = puz.getTime();
            long finishedTime = elapsed;

            long hours = elapsed / HOURS;
            elapsed = elapsed % HOURS;

            long minutes = elapsed / MINUTES;
            elapsed = elapsed % MINUTES;

            long seconds = elapsed / SECONDS;

            String elapsedString;
            if (hours > 0) {
                elapsedString = activity.getString(
                    R.string.completed_time_format_with_hours,
                    hours, minutes, seconds
                );
            } else {
                elapsedString = activity.getString(
                    R.string.completed_time_format_no_hours,
                    minutes, seconds
                );
            }

            int totalClues = puz.getNumberOfClues();
            int totalBoxes = getNumBoxes(puz);;
            int cheatedBoxes = getNumCheatedBoxes(puz);;

            int cheatLevel = cheatedBoxes * 100 / totalBoxes;
            if(cheatLevel == 0 && cheatedBoxes > 0){
                cheatLevel = 1;
            }
            String cheatedString = activity.getString(
                R.string.num_hinted_boxes, cheatedBoxes, cheatLevel
            );

            TextView elapsedTime = layout.findViewById(R.id.elapsed);
            elapsedTime.setText(elapsedString);

            TextView totalCluesView = layout.findViewById(R.id.totalClues);
            totalCluesView.setText(String.format(
                Locale.getDefault(), "%d", totalClues)
            );

            TextView totalBoxesView = layout.findViewById(R.id.totalBoxes);
            totalBoxesView.setText(String.format(
                Locale.getDefault(), "%d", totalBoxes
            ));

            TextView cheatedBoxesView = layout.findViewById(R.id.cheatedBoxes);
            cheatedBoxesView.setText(cheatedString);
        }

        private String getShareMessage(Puzzle puz) {
            Activity activity = getActivity();

            String source = puz.getSource();
            if (source == null)
                source = puz.getTitle();
            if (source == null)
                source = "";

            int cheatedBoxes = getNumCheatedBoxes(puz);

            if (puz.getDate() != null) {
                DateTimeFormatter dateFormat
                    = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

                return activity.getResources().getQuantityString(
                    R.plurals.share_message_with_date,
                    cheatedBoxes,
                    source, dateFormat.format(puz.getDate()), cheatedBoxes
                );
            } else {
                return activity.getResources().getQuantityString(
                    R.plurals.share_message_no_date,
                    cheatedBoxes,
                    source, cheatedBoxes
                );
            }
        }

        private int getNumBoxes(Puzzle puz) {
            int totalBoxes = 0;
            for(Box b : puz.getBoxesList()){
                if(!Box.isBlock(b))
                    totalBoxes++;
            }
            return totalBoxes;
        }

        private int getNumCheatedBoxes(Puzzle puz) {
            int cheatedBoxes = 0;
            for(Box b : puz.getBoxesList()){
                if(!Box.isBlock(b) && b.isCheated())
                    cheatedBoxes++;
            }
            return cheatedBoxes;
        }

        private void addCompletedMsg(Puzzle puz, TextView view) {
            String msg = puz.getCompletionMessage();
            if (msg == null || msg.isEmpty()) {
                view.setVisibility(View.GONE);
            } else {
                view.setText(HtmlCompat.fromHtml(msg, 0));
                view.setVisibility(View.VISIBLE);
            }
        }
    }

    // kind of a weird extends, but they share finished info
    public static class Info extends Finished {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.puzzle_info_dialog, null);

            Activity activity = getActivity();

            Puzzle puz = getPuzzle();
            if (puz != null) {
                TextView title = view.findViewById(R.id.puzzle_info_title);
                title.setText(smartHtml(puz.getTitle()));

                TextView author = view.findViewById(R.id.puzzle_info_author);
                author.setText(puz.getAuthor());

                TextView copyright
                    = view.findViewById(R.id.puzzle_info_copyright);
                copyright.setText(smartHtml(puz.getCopyright()));

                TextView time = view.findViewById(R.id.puzzle_info_time);

                ImaginaryTimer timer = getTimer();
                if (timer != null) {
                    time.setText(getString(
                        R.string.elapsed_time, timer.time()
                    ));
                } else {
                    time.setText(getString(
                        R.string.elapsed_time,
                        new ImaginaryTimer(puz.getTime()).time()
                    ));
                }

                LinearProgressIndicator progress
                    = view.findViewById(R.id.puzzle_info_progress);
                progress.setProgress(puz.getPercentComplete());

                TextView filename
                    = view.findViewById(R.id.puzzle_info_filename);
                FileHandler fileHandler
                    = ForkyzApplication.getInstance().getFileHandler();
                PuzHandle handle = getPuzHandle();
                if (handle != null) {
                    filename.setText(
                        fileHandler.getUri(handle).toString()
                    );
                }

                addIntro(view);
                addNotes(view);
                addCompletedInfo(view);
            }

            builder.setView(view);

            return builder.create();
        }

        private ImaginaryTimer getTimer() {
            Activity activity = getActivity();
            if (activity instanceof PuzzleActivity)
                return ((PuzzleActivity) activity).getTimer();
            else
                return null;
        }

        private PuzHandle getPuzHandle() {
            return ForkyzApplication.getInstance().getPuzHandle();
        }

        private void addIntro(View dialogView) {
            TextView titleView
                = dialogView.findViewById(R.id.puzzle_info_intro_title);
            TextView view = dialogView.findViewById(R.id.puzzle_info_intro);

            Puzzle puz = getPuzzle();
            if (puz == null)
                return;

            String intro = puz.getIntroMessage();
            if (intro == null || intro.isEmpty()) {
                titleView.setVisibility(View.GONE);
                view.setVisibility(View.GONE);
            } else {
                view.setText(smartHtml(intro));
                titleView.setVisibility(View.VISIBLE);
                view.setVisibility(View.VISIBLE);
            }
        }

        private void addNotes(View dialogView) {
            TextView titleView
                = dialogView.findViewById(R.id.puzzle_info_notes_title);
            TextView view = dialogView.findViewById(R.id.puzzle_info_notes);

            Puzzle puz = getPuzzle();
            if (puz == null || !puz.hasNotes()) {
                titleView.setVisibility(View.GONE);
                view.setVisibility(View.GONE);
                return;
            }

            titleView.setVisibility(View.VISIBLE);
            view.setVisibility(View.VISIBLE);

            String puzNotes = puz.getNotes();

            final String notes = puzNotes;

            String[] split = notes.split(
                "(?i:(?m:"
                    + "^\\s*Across:?\\s*$|^.*>Across<.*|"
                    + "^\\s*Down:?\\s*$|^.*>Down<.*|"
                    + "^\\s*\\d))", 2
            );

            final String text = split[0].trim();
            final boolean hasMore = split.length > 1;

            if (!hasMore) {
                view.setText(smartHtml(text));
            } else {
                if (text.length() > 0) {
                    view.setText(smartHtml(
                        getString(R.string.tap_to_show_full_notes_with_text, text)
                    ));
                } else {
                    view.setText(getString(
                        R.string.tap_to_show_full_notes_no_text
                    ));
                }

                view.setOnClickListener(new View.OnClickListener() {
                    private boolean showAll = true;

                    public void onClick(View view) {
                        TextView tv = (TextView) view;

                        if (showAll) {
                            if (notes == null || notes.length() == 0) {
                                tv.setText(getString(
                                    R.string.tap_to_hide_full_notes_no_text
                                ));
                            } else {
                                tv.setText(smartHtml(
                                    getString(
                                        R.string.tap_to_hide_full_notes_with_text,
                                        notes
                                    )
                                ));
                            }
                        } else {
                            if (text == null || text.length() == 0) {
                                tv.setText(getString(
                                    R.string.tap_to_show_full_notes_no_text
                                ));
                            } else {
                                tv.setText(smartHtml(
                                    getString(
                                        R.string.tap_to_show_full_notes_with_text,
                                        text
                                    )
                                ));
                            }
                        }

                        showAll = !showAll;
                    }
                });
            }
        }

        private void addCompletedInfo(View dialogView) {
            View completedTitle
                = dialogView.findViewById(R.id.puzzle_info_completed_title);
            View completedPadding
                = dialogView.findViewById(R.id.puzzle_info_completed_padding);
            View completedMsg
                = dialogView.findViewById(R.id.puzzle_completed_msg);
            View statsTable
                = dialogView.findViewById(R.id.stats_table);

            Puzzle puz = getPuzzle();
            if (puz == null || puz.getPercentComplete() < 100) {
                completedTitle.setVisibility(View.GONE);
                completedPadding.setVisibility(View.GONE);
                completedMsg.setVisibility(View.GONE);
                statsTable.setVisibility(View.GONE);
            } else {
                String msg = puz.getCompletionMessage();
                if (msg == null || msg.isEmpty()) {
                    completedTitle.setVisibility(View.GONE);
                    completedPadding.setVisibility(View.VISIBLE);
                    completedMsg.setVisibility(View.GONE);
                } else {
                    completedTitle.setVisibility(View.VISIBLE);
                    completedPadding.setVisibility(View.GONE);
                    completedMsg.setVisibility(View.VISIBLE);
                }
                statsTable.setVisibility(View.VISIBLE);
                populateFinishedInfo(puz, dialogView);
            }
        }
    }

    public static class Intro extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            Puzzle puz = getPuzzle();
            if (puz != null && puz.hasIntroMessage()) {
                builder.setTitle(getString(R.string.introduction))
                    .setMessage(smartHtml(puz.getIntroMessage()))
                    .setPositiveButton(R.string.ok, null);
            }

            return builder.create();
        }
    }

    private static Spanned smartHtml(String text) {
        return text == null ? null : HtmlCompat.fromHtml(text, 0);
    }

    private static Puzzle getPuzzle() {
        Playboard board = ForkyzApplication.getInstance().getBoard();
        return board == null ? null : board.getPuzzle();
    }
}
