package com.hometusk.shopping.domain;

public record ItemCounts(int total, int purchased, int remaining) {
    public static ItemCounts of(int total, int purchased) {
        return new ItemCounts(total, purchased, total - purchased);
    }
}
