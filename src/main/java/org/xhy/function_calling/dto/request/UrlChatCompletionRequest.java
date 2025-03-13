package org.xhy.function_calling.dto.request;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class UrlChatCompletionRequest {
    private List<Message> messages;
    private String model;
    private boolean stream;

    // Static factory method
    public static UrlChatCompletionRequest createRequest(List<String> imageUrls, String prompt) {
        UrlChatCompletionRequest request = new UrlChatCompletionRequest();

        // Create message
        Message message = new Message();
        message.setRole("user");

        // Create content list
        List<Content> contentList = new ArrayList<>();

        // Add image contents
        for (String imageUrl : imageUrls) {
            Content imageContent = new Content();
            imageContent.setType("image_url");

            ImageUrl imgUrl = new ImageUrl();
            imgUrl.setUrl(imageUrl);
            imgUrl.setDetail("high");

            imageContent.setImage_url(imgUrl);
            contentList.add(imageContent);
        }

        // Add text content
        if (prompt != null && !prompt.trim().isEmpty()) {
            Content textContent = new Content();
            textContent.setType("text");
            textContent.setText(prompt);
            contentList.add(textContent);
        }

        message.setContent(contentList);

        // Set request properties
        request.setMessages(Arrays.asList(message));
        request.setModel("Qwen/Qwen2-VL-72B-Instruct");
        request.setStream(false);

        return request;
    }

    // Nested Message class
    public static class Message {
        private String role;
        private List<Content> content;

        // Getters and Setters
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<Content> getContent() {
            return content;
        }

        public void setContent(List<Content> content) {
            this.content = content;
        }
    }

    // Content class with type discrimination
    public static class Content {
        private String type;
        private String text;
        private ImageUrl image_url;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public ImageUrl getImage_url() {
            return image_url;
        }

        public void setImage_url(ImageUrl image_url) {
            this.image_url = image_url;
        }
    }

    // ImageUrl class
    public static class ImageUrl {
        private String url;
        private String detail;

        // Getters and Setters
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }

    // Getters and Setters for main class
    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
}
