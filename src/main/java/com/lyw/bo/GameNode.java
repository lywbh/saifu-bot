package com.lyw.bo;

public class GameNode {

    private Long qq;

    private Idiom idiom;

    public GameNode(Long qq, Idiom idiom) {
        this.qq = qq;
        this.idiom = idiom;
    }

    public Long getQq() {
        return qq;
    }

    public Idiom getIdiom() {
        return idiom;
    }

}
