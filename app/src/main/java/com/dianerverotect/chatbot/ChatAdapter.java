package com.dianerverotect.chatbot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dianerverotect.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the chat messages in the chatbot
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    
    private final Context context;
    private final List<ChatMessage> chatMessages;
    
    /**
     * Constructor
     * @param context Context
     * @param chatMessages List of chat messages
     */
    public ChatAdapter(Context context, List<ChatMessage> chatMessages) {
        this.context = context;
        this.chatMessages = chatMessages;
    }
    
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        return message.isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_bot, parent, false);
            return new BotMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        
        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            UserMessageViewHolder userHolder = (UserMessageViewHolder) holder;
            userHolder.messageText.setText(message.getMessage());
            
            // Set timestamp if available
            if (userHolder.messageTimestamp != null) {
                userHolder.messageTimestamp.setText(getCurrentTime());
            }
        } else {
            BotMessageViewHolder botHolder = (BotMessageViewHolder) holder;
            
            // Set timestamp if available
            if (botHolder.messageTimestamp != null) {
                botHolder.messageTimestamp.setText(getCurrentTime());
            }
            
            // If the message has an image, show it
            if (message.getImageResourceId() != 0) {
                botHolder.messageImage.setVisibility(View.VISIBLE);
                botHolder.messageImage.setImageResource(message.getImageResourceId());
            } else {
                botHolder.messageImage.setVisibility(View.GONE);
            }
            
            // Handle typing effect for bot messages
            if (message.isTyping() && !message.isTypingComplete()) {
                botHolder.typingIndicator.setVisibility(View.VISIBLE);
                botHolder.messageText.setVisibility(View.GONE);
                
                // Animate the dots
                TextView dot1 = botHolder.typingIndicator.findViewById(R.id.typing_dot1);
                TextView dot2 = botHolder.typingIndicator.findViewById(R.id.typing_dot2);
                TextView dot3 = botHolder.typingIndicator.findViewById(R.id.typing_dot3);
                
                if (dot1 != null && dot2 != null && dot3 != null) {
                    TypingIndicator typingIndicator = new TypingIndicator(dot1, dot2, dot3);
                    typingIndicator.startAnimation();
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        botHolder.typingIndicator.setVisibility(View.GONE);
                        botHolder.messageText.setVisibility(View.VISIBLE);
                        typingIndicator.stopAnimation();
                        
                        TypewriterEffect typewriterEffect = new TypewriterEffect(botHolder.messageText);
                        typewriterEffect.setOnTypingCompleteListener(() -> {
                            message.setTypingComplete(true);
                            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                                notifyItemChanged(holder.getAdapterPosition());
                            }
                        });
                        typewriterEffect.startTyping(message.getMessage());
                    }, 1200); // Simulate some thinking time
                }
            } else {
                botHolder.typingIndicator.setVisibility(View.GONE);
                botHolder.messageText.setVisibility(View.VISIBLE);
                botHolder.messageText.setText(message.getMessage());
            }
        }
    }
    
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
    
    /**
     * Get current time formatted as HH:mm
     * @return Formatted time string
     */
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * ViewHolder for user messages
     */
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView messageTimestamp;
        
        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.user_message_text);
            messageTimestamp = itemView.findViewById(R.id.message_timestamp);
        }
    }
    
    /**
     * ViewHolder for bot messages
     */
    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView messageImage;
        TextView messageTimestamp;
        View typingIndicator;
        
        BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.bot_message_text);
            messageImage = itemView.findViewById(R.id.bot_message_image);
            messageTimestamp = itemView.findViewById(R.id.message_timestamp);
            typingIndicator = itemView.findViewById(R.id.typing_indicator);
        }
    }
}
