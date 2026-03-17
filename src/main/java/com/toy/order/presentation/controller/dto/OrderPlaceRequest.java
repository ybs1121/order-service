package com.toy.order.presentation.controller.dto;

public record OrderPlaceRequest(
        String productId,
        int quantity
) {}
