package com.dianerverotect.chatbot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.dianerverotect.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the ChatAdapter class
 */
@RunWith(MockitoJUnitRunner.class)
public class ChatAdapterTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private LayoutInflater mockInflater;
    
    @Mock
    private View mockView;
    
    @Mock
    private TextView mockTextView;
    
    @Mock
    private ImageView mockImageView;
    
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    
    @Before
    public void setUp() {
        // Set up mock context and inflater
        when(mockContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).thenReturn(mockInflater);
        when(mockInflater.inflate(anyInt(), any(ViewGroup.class), any(Boolean.class))).thenReturn(mockView);
        when(mockView.findViewById(anyInt())).thenReturn(mockTextView);
        
        // Create test data
        chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("Hello", true));
        chatMessages.add(new ChatMessage("Hi there! How can I help you?", false));
        
        // Create a new instance of ChatAdapter with the mock context and test data
        chatAdapter = new ChatAdapter(mockContext, chatMessages);
    }
    
    @Test
    public void testGetItemCount() {
        // Test that getItemCount returns the correct number of items
        assertEquals("Item count should match the number of chat messages", 
                chatMessages.size(), chatAdapter.getItemCount());
    }
    
    @Test
    public void testGetItemViewType() {
        // Test that getItemViewType returns the correct view type for user messages
        assertEquals("View type for user message should be 1", 
                1, chatAdapter.getItemViewType(0));
        
        // Test that getItemViewType returns the correct view type for bot messages
        assertEquals("View type for bot message should be 2", 
                2, chatAdapter.getItemViewType(1));
    }
    
    @Test
    public void testAddMessage() {
        // Get the initial count
        int initialCount = chatAdapter.getItemCount();
        
        // Add a new message
        chatMessages.add(new ChatMessage("New message", true));
        
        // Test that the count increased by 1
        assertEquals("Item count should increase by 1 after adding a message", 
                initialCount + 1, chatAdapter.getItemCount());
    }
    
    @Test
    public void testCreateViewHolder() {
        // Mock the parent ViewGroup
        ViewGroup mockParent = mock(ViewGroup.class);
        
        // Test creating a view holder for a user message
        RecyclerView.ViewHolder userHolder = chatAdapter.onCreateViewHolder(mockParent, 1);
        assertNotNull("User message ViewHolder should not be null", userHolder);
        
        // Test creating a view holder for a bot message
        RecyclerView.ViewHolder botHolder = chatAdapter.onCreateViewHolder(mockParent, 2);
        assertNotNull("Bot message ViewHolder should not be null", botHolder);
    }
}
