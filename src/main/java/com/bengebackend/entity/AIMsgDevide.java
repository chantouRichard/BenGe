package com.bengebackend.entity;

import java.util.List;

public class AIMsgDevide {
    private String MsgForUser;
    private String StrScript;
    private String Title;
    private String Background;
    private List<String> ChrScript;
    private List<String> Clues;
    private String Trues;
    private String GoBook;

    public AIMsgDevide() {
    }

    public AIMsgDevide(String Title, String StrScript) {
        this.Title = Title;
        this.StrScript = StrScript;
    }

    public AIMsgDevide(String MsgForUser, String StrScript, String Title, String Background,
            List<String> ChrScript, List<String> Clues, String Trues, String GoBook) {
        this.MsgForUser = MsgForUser;
        this.StrScript = StrScript;
        this.Title = Title;
        this.Background = Background;
        this.ChrScript = ChrScript;
        this.Clues = Clues;
        this.Trues = Trues;
        this.GoBook = GoBook;
    }

    public String getMsgForUser() {
        return MsgForUser;
    }

    public void setMsgForUser(String MsgForUser) {
        this.MsgForUser = MsgForUser;
    }

    public String getStrScript() {
        return StrScript;
    }

    public void setStrScript(String StrScript) {
        this.StrScript = StrScript;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String Title) {
        this.Title = Title;
    }

    public String getBackground() {
        return Background;
    }

    public void setBackground(String Background) {
        this.Background = Background;
    }

    public List<String> getChrScript() {
        return ChrScript;
    }

    public void setChrScript(List<String> ChrScript) {
        this.ChrScript = ChrScript;
    }

    public List<String> getClues() {
        return Clues;
    }

    public void setClues(List<String> Clues) {
        this.Clues = Clues;
    }

    public String getTrues() {
        return Trues;
    }

    public void setTrues(String Trues) {
        this.Trues = Trues;
    }

    public String getGoBook() {
        return GoBook;
    }

    public void setGoBook(String GoBook) {
        this.GoBook = GoBook;
    }

    @Override
    public String toString() {
        return "Stage2AIMsgDevide{" +
                "MsgForUser='" + MsgForUser + "\n\n" +
                ", StrScript='" + StrScript + "\n\n" +
                ", Title='" + Title + "\n\n" +
                ", Background='" + Background + "\n\n" +
                ", ChrScript=" + ChrScript +
                ", Clues=" + Clues +
                ", Trues='" + Trues + "\n\n" +
                ", GoBook='" + GoBook + "\n\n" +
                '}';
    }
}
