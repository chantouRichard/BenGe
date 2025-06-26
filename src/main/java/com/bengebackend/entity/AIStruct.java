package com.bengebackend.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AIStruct {

    public class ScriptDetail {
        @JsonProperty("作为作家的礼貌性回答")
        private String describe;

        @JsonProperty("标题")
        private String title;

        @JsonProperty("背景")
        private String background;

        @JsonProperty("人物剧本")
        private List<String> chrScript;

        @JsonProperty("线索")
        private List<String> clues;

        @JsonProperty("真相")
        private String truths;

        @JsonProperty("组织者手册")
        private String dmBook;

        // Getter 和 Setter 方法
        public String getDescribe() {
            return describe;
        }

        public void setDescribe(String describe) {
            this.describe = describe;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBackground() {
            return background;
        }

        public void setBackground(String background) {
            this.background = background;
        }

        public List<String> getChrScript() {
            return chrScript;
        }

        public void setChrScript(List<String> chrScript) {
            this.chrScript = chrScript;
        }

        public List<String> getClues() {
            return clues;
        }

        public void setClues(List<String> clues) {
            this.clues = clues;
        }

        public String getTruths() {
            return truths;
        }

        public void setTruths(String truths) {
            this.truths = truths;
        }

        public String getDmBook() {
            return dmBook;
        }

        public void setDmBook(String dmBook) {
            this.dmBook = dmBook;
        }
    }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class ImageMsgs {
        private List<ThingDesc> thingDesc;

        public List<ThingDesc> getThingDesc() {
            return thingDesc;
        }

        public void setThingDesc(List<ThingDesc> thingDesc) {
            this.thingDesc = thingDesc;
        }

        public ImageMsgs(List<ThingDesc> l) {
            thingDesc = l;
        }
    }

    public class ThingDesc {
        @JsonProperty("名称")
        private List<String> name;

        @JsonProperty("描绘")
        private List<String> desc;

        public List<String> getName() {
            return name;
        }

        public void setName(List<String> name) {
            this.name = name;
        }

        public List<String> getDesc() {
            return desc;
        }

        public void setDesc(List<String> desc) {
            this.desc = desc;
        }
    }

    public static class ApiRequest {
        private final String model;
        private final List<Message> messages;
        private final int max_tokens;

        public ApiRequest(String model, List<Message> messages, int max_tokens) {
            this.model = model;
            this.messages = messages;
            this.max_tokens = max_tokens;
        }

        // Getter方法
        public String getModel() {
            return model;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public int getMax_tokens() {
            return max_tokens;
        }
    }

    public static class DeepSeekResponse {
        @JsonProperty("id")
        private String id;

        @JsonProperty("object")
        private String object;

        @JsonProperty("created")
        private long created;

        @JsonProperty("model")
        private String model;

        @JsonProperty("choices")
        private List<ChoiceMessage> choices;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        public long getCreated() {
            return created;
        }

        public void setCreated(long created) {
            this.created = created;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<ChoiceMessage> getChoices() {
            return choices;
        }

        public void setChoices(List<ChoiceMessage> choices) {
            this.choices = choices;
        }
    }

    public static class ChoiceMessage {
        @JsonProperty("index")
        private int index;

        @JsonProperty("message")
        private Message mes;

        // Getters and Setters
        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public Message getMes() {
            return mes;
        }

        public void setMes(Message mes) {
            this.mes = mes;
        }
    }

}
