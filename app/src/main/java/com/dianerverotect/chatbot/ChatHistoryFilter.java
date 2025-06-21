package com.dianerverotect.chatbot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ChatHistoryFilter {
    public enum SortOrder {
        NEWEST_FIRST,
        OLDEST_FIRST
    }

    public enum FilterType {
        ALL,
        MESSAGES_ONLY,
        QUESTIONNAIRES_ONLY
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);

    public static void applyFilter(List<ChatHistoryItem> list, FilterType filterType, SortOrder sortOrder) {
        // Appliquer le filtre par type
        if (filterType != FilterType.ALL) {
            list.removeIf(item -> {
                boolean isQuestionnaire = item.getTypeEchange().toLowerCase().contains("questionnaire");
                return (filterType == FilterType.MESSAGES_ONLY && isQuestionnaire) ||
                       (filterType == FilterType.QUESTIONNAIRES_ONLY && !isQuestionnaire);
            });
        }

        // Appliquer le tri par date
        Collections.sort(list, (item1, item2) -> {
            try {
                long date1 = dateFormat.parse(item1.getDateHeure()).getTime();
                long date2 = dateFormat.parse(item2.getDateHeure()).getTime();
                return sortOrder == SortOrder.NEWEST_FIRST ? 
                       Long.compare(date2, date1) : Long.compare(date1, date2);
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    public static void applySearch(List<ChatHistoryItem> sourceList, List<ChatHistoryItem> targetList, String query) {
        targetList.clear();
        String lowercaseQuery = query.toLowerCase(Locale.getDefault());
        
        for (ChatHistoryItem item : sourceList) {
            if (matchesSearch(item, lowercaseQuery)) {
                targetList.add(item);
            }
        }
    }

    private static boolean matchesSearch(ChatHistoryItem item, String query) {
        return item.getResume().toLowerCase(Locale.getDefault()).contains(query) ||
               item.getDateHeure().toLowerCase(Locale.getDefault()).contains(query) ||
               item.getTypeEchange().toLowerCase(Locale.getDefault()).contains(query);
    }
} 