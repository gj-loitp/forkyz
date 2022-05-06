package app.crossword.yourealwaysbe.view.recycler;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by rcooper on 7/8/15.
 */
public class SeparatedRecyclerViewAdapter<
    BodyHolder extends RecyclerView.ViewHolder,
    SectionAdapter extends RemovableRecyclerViewAdapter<BodyHolder>
> extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Dismissable {
    private static int HEADER = Integer.MIN_VALUE;

    private LinkedHashMap<String, SectionAdapter> sections = new LinkedHashMap<>();
    private final int textViewId;
    private Class<BodyHolder> bodyHolderClass;

    /**
     * Needs BodyHolder class for type safety
     */
    public SeparatedRecyclerViewAdapter(
        int textViewId, Class<BodyHolder> bodyHolderClass
    ) {
        this.textViewId = textViewId;
        this.bodyHolderClass = bodyHolderClass;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if(viewType == HEADER){
            TextView view = (TextView) LayoutInflater.from(viewGroup.getContext())
                    .inflate(textViewId, viewGroup, false);
            return new SimpleTextViewHolder(view);
        } else {
            RecyclerView.ViewHolder result = null;
            for(SectionAdapter sectionAdapter : sections.values()){
                try {
                    result = sectionAdapter.onCreateViewHolder(viewGroup, viewType);
                } catch(Exception e){
                    e.printStackTrace();
                }
                if (result != null)
                    return result;
            }
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        int sectionPosition = 0;
        for (Map.Entry<String, SectionAdapter> entry : this.sections.entrySet()) {
            int size = entry.getValue().getItemCount() + 1;
            if(position < sectionPosition + size){
                int index = position - sectionPosition;
                if(index == 0){
                    TextView view = (TextView) ((SimpleTextViewHolder) viewHolder).itemView;
                    view.setText(entry.getKey());
                } else {
                    BodyHolder bodyHolder = bodyHolderClass.cast(viewHolder);
                    entry.getValue().onBindViewHolder(bodyHolder, index - 1);
                }
                break;
            }
            sectionPosition += size;
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        for(SectionAdapter section : sections.values()){
            count++;
            count += section.getItemCount();
        }
        return count;
    }

    public boolean isEmpty() {
        for (SectionAdapter section : sections.values()) {
            if (section.getItemCount() > 0)
                return false;
        }
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        int sectionPosition = 0;
        for (Map.Entry<String, SectionAdapter> entry : this.sections.entrySet()) {
            int size = entry.getValue().getItemCount() + 1;
            if(position < sectionPosition + size){
                int index = position - sectionPosition;
                if(index == 0){
                    return HEADER;
                } else {
                    return entry.getValue().getItemViewType(index -1);
                }
            }
            sectionPosition += size;
        };
        throw new RuntimeException("Unable to find anything for position "+position);
    }

    public void addSection(String header, SectionAdapter adapter) {
        this.sections.put(header, adapter);
        adapter.registerAdapterDataObserver(
            new RecyclerView.AdapterDataObserver() {
                @Override
                @SuppressLint("NotifyDataSetChanged")
                public void onChanged() {
                    notifyDataSetChanged();
                }
                @Override
                public void onItemRangeChanged(
                    int positionStart, int itemCount, Object payload
                ) {
                    int offset = getSectionOffset(header) + 1;
                    notifyItemRangeChanged(
                        offset + positionStart, itemCount, payload
                    );
                }
                @Override
                public void onItemRangeChanged(
                    int positionStart, int itemCount
                ) {
                    int offset = getSectionOffset(header) + 1;
                    notifyItemRangeChanged(
                        offset + positionStart, itemCount
                    );
                }
                @Override
                public void onItemRangeInserted(
                    int positionStart, int itemCount
                ) {
                    int offset = getSectionOffset(header) + 1;
                    notifyItemRangeInserted(
                        offset + positionStart, itemCount
                    );
                }
                @Override
                public void onItemRangeMoved(
                    int fromPosition, int toPosition, int itemCount
                ) {
                    int offset = getSectionOffset(header) + 1;
                    for (int i = 0; i < itemCount; i++) {
                        notifyItemMoved(
                            offset + fromPosition + i,
                            offset + toPosition + i
                        );
                    }
                }
                @Override
                public void onItemRangeRemoved(
                    int positionStart, int itemCount
                ) {
                    int offset = getSectionOffset(header) + 1;
                    notifyItemRangeRemoved(
                        offset + positionStart, itemCount
                    );
                }
                @Override
                public void onStateRestorationPolicyChanged() {
                    // TODO: don't know what to do with this news
                    // It is not currently important for Forkyz
                }
            }
        );
    }

    public Iterable<SectionAdapter> sectionAdapters() {
        return this.sections.values();
    }

    @Override
    public void onItemDismiss(int position) {
       int sectionPosition = 0;
       for (Map.Entry<String, SectionAdapter> entry : new LinkedList<>(this.sections.entrySet())) {
            int size = entry.getValue().getItemCount() + 1;
            if (position < sectionPosition + size) {
                int index = position - sectionPosition;
                if (index == 0) {
                    return;
                } else {
                    entry.getValue().remove(index - 1);
                    notifyItemRemoved(position);
                    if(entry.getValue().getItemCount() == 0){
                        this.sections.remove(entry.getKey());
                        notifyItemRemoved(position - 1);
                    }
                    break;
                }
            }
            sectionPosition += size;
        }

    }

    public static class SimpleTextViewHolder extends RecyclerView.ViewHolder {
        public SimpleTextViewHolder(TextView itemView) {
            super(itemView);
        }
    }

    private int getSectionOffset(String header) {
        int offset = 0;
        for (
            Map.Entry<String, SectionAdapter> entry
                : this.sections.entrySet()
        ) {
            if (entry.getKey().equals(header))
                return offset;
            // +1 for header
            offset += entry.getValue().getItemCount() + 1;
        }
        throw new IllegalArgumentException("Section " + header + " unknown.");
    }
}
