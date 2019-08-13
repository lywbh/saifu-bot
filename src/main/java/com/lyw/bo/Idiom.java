package com.lyw.bo;

import java.util.HashSet;
import java.util.Set;

public class Idiom {

    private String word;
    private String startPy;
    private String endPy;
    private Set<Idiom> next;

    public Idiom(String word, String startPy, String endPy) {
        this.word = word;
        this.startPy = startPy;
        this.endPy = endPy;
        this.next = new HashSet<>();
    }

    public String getWord() {
        return word;
    }

    public String getStartPy() {
        return startPy;
    }

    public String getEndPy() {
        return endPy;
    }

    public Set<Idiom> getNext() {
        return next;
    }

}
