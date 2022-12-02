package com.example.demo;

import java.util.Map;

record KafkaRecord(String message, Map<String, String> headers) {

}
