package com.meteor.support.client.example;

import com.meteor.support.client.example.model.ExampleClientResult;

record ExampleResponseDto(String id, String message) {

    ExampleClientResult toResult() {
        return new ExampleClientResult(id, message);
    }

}
