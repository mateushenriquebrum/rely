package com.split.me.rely;

public sealed interface Status permits
        Status.Pending,
        Status.Retry,
        Status.Unreachable,
        Status.Success,
        Status.Failure {
    record Pending(String topic, String payload, int id) implements Status {}
    record Retry(String topic, String payload, int id, int invoked) implements Status {}
    record Unreachable(String topic, String payload, int id, int invoked) implements Status {}
    record Success(String topic, String payload, int id) implements Status {}
    record Failure(String topic, String payload, int id) implements Status {}
}

